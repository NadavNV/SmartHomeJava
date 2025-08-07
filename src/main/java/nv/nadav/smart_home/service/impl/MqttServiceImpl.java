package nv.nadav.smart_home.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolation;
import nv.nadav.smart_home.dto.DeviceDto;
import nv.nadav.smart_home.dto.DeviceUpdateDto;
import nv.nadav.smart_home.exception.DeviceNotFoundException;
import nv.nadav.smart_home.exception.DeviceValidationException;
import nv.nadav.smart_home.serialization.DelegatingParametersDeserializer;
import nv.nadav.smart_home.serialization.DeviceParametersDeserializer;
import nv.nadav.smart_home.service.DeviceService;
import nv.nadav.smart_home.service.MqttService;
import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.UserProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class MqttServiceImpl implements MqttService {
    private static final Logger logger = LoggerFactory.getLogger("smart_home.mqtt");
    private final Validator validator;
    private final MqttClient mqttClient;
    private final DeviceService deviceService;
    private Queue<MessageRecord> messageQueue;

    @Autowired
    public MqttServiceImpl(MqttClient client, DeviceService deviceService, Validator validator) {
        mqttClient = client;
        this.deviceService = deviceService;
        messageQueue = new LinkedList<>();
        this.validator = validator;
    }

    private record MessageRecord(String topic, MqttMessage message) {
    }

    @PostConstruct
    public void mqttInit() {
        try {
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void disconnected(MqttDisconnectResponse response) {
                    int reasonCode = response.getReturnCode();
                    String reasonString = response.getReasonString();
                    System.out.printf("Disconnected: [%d] %s%n", reasonCode, reasonString);
                }

                @Override
                public void mqttErrorOccurred(MqttException e) {
                    logger.error("Unknown MQTT error occurred", e);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    MqttProperties props = message.getProperties();
                    List<UserProperty> userProps = props.getUserProperties();
                    String senderId = null;
                    String senderGroup = null;
                    for (UserProperty prop : userProps) {
                        if ("sender_id".equals(prop.getKey())) {
                            senderId = prop.getValue();
                        } else if ("sender_group".equals(prop.getKey())) {
                            senderGroup = prop.getValue();
                        }
                    }
                    if (senderId == null) {
                        logger.error("Message missing sender");
                        return;
                    }
                    if (senderGroup == null) {
                        logger.error("Message missing sender group");
                        return;
                    }
                    if (mqttClient.getClientId().equals(senderId) || "backend".equals(senderGroup)) {
                        return;
                    }
                    logger.info("Message received on topic {}", topic);

                    String json = new String(message.getPayload(), StandardCharsets.UTF_8);

                    // Extract device_id from topic:
                    // expected format nadavnv-smart-home/devices/<device_id>/<method> or similar
                    String[] topicSegments = topic.split("/");
                    if (topicSegments.length == 4) {
                        String deviceId = topicSegments[2];
                        Method method;
                        try {
                            method = Method.fromValue(topicSegments[3]);
                        } catch (IllegalArgumentException e) {
                            logger.error("Unknown method {}", topicSegments[3], e);
                            return;
                        }
                        ObjectMapper mapper = new ObjectMapper();
                        switch (method) {
                            case POST -> {
                                try {
                                    DeviceDto deviceDto = mapper.readValue(json, DeviceDto.class);
                                    Set<ConstraintViolation<DeviceDto>> violations = validator.validate(deviceDto);
                                    if (!violations.isEmpty()) {
                                        throw new DeviceValidationException(violations.stream()
                                                .map(ConstraintViolation::getMessage)
                                                .toList());
                                    }
                                    deviceService.addDevice(deviceDto);
                                } catch (JsonProcessingException e) {
                                    logger.error("Error parsing json", e);
                                } catch (DeviceValidationException e) {
                                    logger.error("Error validating {}", deviceId, e);
                                }
                            }
                            case UPDATE -> {
                                try {
                                    DeviceDto device = deviceService.getDeviceById(deviceId);
                                    DelegatingParametersDeserializer.delegate.set(
                                            new DeviceParametersDeserializer(device.getType()));
                                    DeviceUpdateDto update = mapper.readValue(json, DeviceUpdateDto.class);
                                    deviceService.updateDevice(deviceId, update);
                                } catch (DeviceNotFoundException e) {
                                    logger.error("Device {} not found", deviceId, e);
                                } catch (DeviceValidationException e) {
                                    logger.error("Error validating {}", deviceId, e);
                                } finally {
                                    DelegatingParametersDeserializer.delegate.remove();
                                }
                            }
                            case DELETE -> {
                                try {
                                    deviceService.deleteDeviceById(deviceId);
                                } catch (DeviceNotFoundException e) {
                                    logger.error("Device {} not found", deviceId, e);
                                }
                            }
                        }
                    } else {
                        logger.error("Incorrect topic {}", topic);
                    }

                }

                @Override
                public void deliveryComplete(IMqttToken iMqttToken) {
                    logger.info("Delivery complete");
                }

                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    logger.info("Connected to broker at {}{}", serverURI, reconnect ? " (reconnected)" : "");
                    Queue<MessageRecord> unsentMessages = new LinkedList<>();
                    while (!messageQueue.isEmpty()) {
                        MessageRecord message = messageQueue.poll();
                        try {
                            mqttClient.publish(message.topic, message.message);
                        } catch (MqttException e) {
                            unsentMessages.add(message);
                        }
                    }
                    messageQueue = unsentMessages;
                }

                @Override
                public void authPacketArrived(int reasonCode, MqttProperties mqttProperties) {
                    logger.info("Auth packet arrived");
                }
            });
            MqttConnectionOptions options = new MqttConnectionOptions();
            options.setAutomaticReconnect(true);
            options.setCleanStart(false);

            mqttClient.connect(options);

            mqttClient.subscribe("$share/backend/" + TOPIC + "/#", 2);

            System.out.println("MQTT connected and subscribed");
        } catch (MqttException e) {
            logger.error("Error while establishing MQTT client", e);
        }
    }

    @Override
    public void publishMqtt(Map<String, Object> payload, String topicPrefix, String deviceId, Method method) {
        String topic = String.format("%s/%s/%s", topicPrefix, deviceId, method.getValue());
        payload.remove("_id");  // Discard mongoDB _id field if present
        String jsonPayload;
        try {
            jsonPayload = new ObjectMapper().writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            logger.error("Error processing payload", e);
            return;
        }
        MqttProperties props = new MqttProperties();
        props.setUserProperties(List.of(
                new UserProperty("sender_id", mqttClient.getClientId()),
                new UserProperty("sender_group", "backend")
        ));

        MqttMessage message = new MqttMessage(jsonPayload.getBytes(StandardCharsets.UTF_8));
        message.setQos(2);
        message.setProperties(props);
        try {
            mqttClient.publish(topic, message);
        } catch (MqttException e) {
            logger.error("Error trying to publish", e);
            messageQueue.add(new MessageRecord(topic, message));
        }
    }

    @Override
    public boolean isConnected() {
        return mqttClient.isConnected();
    }
}
