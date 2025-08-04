package nv.nadav.smart_home.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import nv.nadav.smart_home.service.impl.MqttServiceImpl;

import java.util.Map;
import java.util.Optional;


public interface MqttService {
    String TOPIC = Optional.ofNullable(System.getenv("MQTT_TOPIC")).orElse("nadavnv-smart-home/devices");

    void publishMqtt(Map<String, Object> payload, String topicPrefix, String deviceId, MqttServiceImpl.Method method);

    boolean isConnected();

    enum Method {
        POST("post"),
        UPDATE("update"),
        DELETE("delete");

        private final String value;

        Method(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @JsonCreator
        public static Method fromValue(String value) {
            for (Method method : Method.values()) {
                if (method.value.equalsIgnoreCase(value)) {
                    return method;
                }
            }
            throw new IllegalArgumentException("Unknown method: " + value);
        }
    }
}
