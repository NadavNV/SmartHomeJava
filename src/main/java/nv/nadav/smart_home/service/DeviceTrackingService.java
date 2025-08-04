package nv.nadav.smart_home.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public interface DeviceTrackingService {
    boolean isDeviceNew(String deviceId);

    void markDeviceSeen(String deviceId);

    void removeDeviceSeen(String deviceId);

    List<Interval> getDeviceIntervals(String deviceId);

    void saveDeviceIntervals(String deviceId, List<Interval> intervals);

    void startNewInterval(String deviceId, Instant startTime);

    double closeLastInterval(String deviceId, Instant endTime);

    record Interval(Instant start, Instant end) {
        public double getDuration() {
            Instant effectiveEnd = (end != null) ? end : Instant.now();
            return Duration.between(start, effectiveEnd).toMillis() / 1000.0;
        }
    }
}
