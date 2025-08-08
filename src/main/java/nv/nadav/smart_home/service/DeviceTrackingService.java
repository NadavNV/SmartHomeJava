package nv.nadav.smart_home.service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
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
        private static final DateTimeFormatter PYTHON_FORMATTER =
                new DateTimeFormatterBuilder()
                        .appendPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
                        // Always print offset as +HH:MM, never 'Z'
                        .appendOffset("+HH:MM", "+00:00")
                        .toFormatter();

        public double getDuration() {
            Instant effectiveEnd = (end != null) ? end : Instant.now();
            return Duration.between(start, effectiveEnd).toMillis() / 1000.0;
        }

        public String toJson() {
            return String.format("[\"%s\", %s]", formatInstant(start), end != null ? "\"" + formatInstant(end) + "\"" : "null");
        }

        private static String formatInstant(Instant instant) {
            // Truncate to microseconds to match Python's ISO format()
            return PYTHON_FORMATTER.format(instant.truncatedTo(ChronoUnit.MICROS).atOffset(ZoneOffset.UTC));
        }
    }
}
