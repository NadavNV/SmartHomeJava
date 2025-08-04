package nv.nadav.smart_home.service;

import java.time.Instant;
import java.util.List;

public interface DeviceTrackingService {
    boolean isDeviceSeen(String deviceId);

    void markDeviceSeen(String deviceId);

    List<Interval> getDeviceIntervals(String deviceId);

    void saveDeviceIntervals(String deviceId, List<Interval> intervals);

    void startNewInterval(String deviceId, Instant startTime);

    void closeLastInterval(String deviceId, Instant endTime);

    record Interval(Instant start, Instant end) {
    }
}
