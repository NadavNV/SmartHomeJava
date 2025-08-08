package nv.nadav.smart_home.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import nv.nadav.smart_home.service.DeviceTrackingService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class DeviceTrackingServiceImpl implements DeviceTrackingService {
    private final RedisCommands<String, String> redis;

    private final ObjectMapper objectMapper;

    public DeviceTrackingServiceImpl(StatefulRedisConnection<String, String> redisConnection) {
        this.redis = redisConnection.sync();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public boolean isDeviceNew(String deviceId) {
        return redis.sismember("seen_devices", deviceId);
    }

    @Override
    public void markDeviceSeen(String deviceId) {
        redis.sadd("seen_devices", deviceId);
    }

    @Override
    public void removeDeviceSeen(String deviceId) {
        redis.srem("seen_devices", deviceId);
    }

    @Override
    public List<Interval> getDeviceIntervals(String deviceId) {
        String json = redis.hget("device_on_intervals", deviceId);
        if (json == null) return new ArrayList<>();
        try {
            // Parse as List of [start, end] tuples
            List<List<String>> rawList = objectMapper.readValue(json, new TypeReference<>() {});
            List<Interval> intervals = new ArrayList<>(rawList.size());
            for (List<String> pair : rawList) {
                Instant start = Instant.parse(pair.get(0));
                Instant end = pair.get(1) != null ? Instant.parse(pair.get(1)) : null;
                intervals.add(new Interval(start, end));
            }
            return intervals;
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse intervals", e);
        }
    }

    @Override
    public void saveDeviceIntervals(String deviceId, List<Interval> intervals) {
        List<List<String>> rawList = intervals.stream()
                .map(i -> {
                    List<String> interval = new ArrayList<>(2);
                    interval.add(i.start().toString());
                    interval.add(i.end() != null ? i.end().toString() : null);
                    return interval;
                }).toList();
        try {
            String json = objectMapper.writeValueAsString(rawList);
            redis.hset("device_on_intervals", deviceId, json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize intervals", e);
        }
    }

    @Override
    public void startNewInterval(String deviceId, Instant startTime) {
        redis.rpush("device_on_intervals:" + deviceId, new Interval(startTime, null).toJson());
    }

    @Override
    public double closeLastInterval(String deviceId, Instant endTime) {
        List<Interval> intervals = getDeviceIntervals(deviceId);
        if (intervals.isEmpty()) {
            throw new IllegalStateException("No intervals found for device " + deviceId);
        }

        Interval last = intervals.getLast();
        if (last.end() != null) {
            throw new IllegalStateException("Last interval already closed for device " + deviceId);
        }

        Interval newInterval = new Interval(last.start(), endTime);
        intervals.set(intervals.size() - 1, newInterval);
        saveDeviceIntervals(deviceId, intervals);
        return newInterval.getDuration();
    }
}
