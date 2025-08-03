package nv.nadav.smart_home.service;

import nv.nadav.smart_home.config.MqttProperties;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
public class MqttConfig {

    @Bean
    public MqttClient mqttClient(MqttProperties props) throws MqttException {
        String brokerUri = "tcp://" + props.getHost() + ":" + props.getPort();
        String hostname = Optional.ofNullable(System.getenv("HOSTNAME")).orElse("unknown-host");
        String clientId = "spring-backend-" + hostname;
        return new MqttClient(brokerUri, clientId, new MemoryPersistence());
    }
}
