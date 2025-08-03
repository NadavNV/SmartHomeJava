package nv.nadav.smart_home.service;

import jakarta.annotation.PostConstruct;
import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;

@Service
public class MqttService {
    private static final Logger logger = LoggerFactory.getLogger("smart_home.mqtt");
    private final MqttClient mqttClient;
    private Queue<MessageRecord> messageQueue;

    public MqttService(MqttClient client) {
        mqttClient = client;
        messageQueue = new LinkedList<>();
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
                    logger.info("Message received [{}]: {}\n", topic, new String(message.getPayload()));
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

                }
            });
            MqttConnectionOptions options = new MqttConnectionOptions();
            options.setAutomaticReconnect(true);
            options.setCleanStart(false);

            mqttClient.connect(options);

            mqttClient.subscribe(Optional.ofNullable(System.getenv("MQTT_TOPIC")).orElse("nadavnv-smart-home/devices") + "/#", 2);

            System.out.println("MQTT connected and subscribed");
        } catch (MqttException e) {
            logger.error("Error while establishing MQTT client", e);
        }


    }
}
