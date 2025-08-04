package nv.nadav.smart_home.controller;

import com.mongodb.client.MongoClient;
import nv.nadav.smart_home.service.MqttService;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @Autowired
    private MongoClient mongoClient;
    @Autowired
    private RedisConnectionFactory redisFactory;
    @Autowired
    private MqttService mqttService;

    @GetMapping("/livez")
    public ResponseEntity<Map<String, String>> healthy() {
        return ResponseEntity.ok(Map.of("Status", "Healthy"));
    }

    @GetMapping("/readyz")
    public ResponseEntity<Map<String, String>> ready() {
        try {
            mongoClient.getDatabase("admin").runCommand(new Document("ping", 1));
            if (!mqttService.isConnected()) {
                return ResponseEntity.status(500).body(Map.of("Status", "Not Ready"));
            }
            redisFactory.getConnection().ping();
            return ResponseEntity.ok(Map.of("Status", "Ready"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("Status", "Not Ready"));
        }
    }
}
