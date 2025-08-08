package nv.nadav.smart_home.service.impl;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import nv.nadav.smart_home.service.DeviceTrackingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DeviceTrackingServiceImplTest {

    @Mock
    private RedisCommands<String, String> mockRedis;

    @Mock
    private StatefulRedisConnection<String, String> mockConnection;

    private DeviceTrackingServiceImpl service;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        when(mockConnection.sync()).thenReturn(mockRedis);
        service = spy(new DeviceTrackingServiceImpl(mockConnection));
    }

    @AfterEach
    void tearDown() throws Exception{
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    void testGetDeviceIntervals_valid() {
        Instant now = Instant.now();
        Instant fiveHoursAgo = now.minus(5, ChronoUnit.HOURS);
        Instant threeHoursAgo = now.minus(3, ChronoUnit.HOURS);
        Instant oneHourAgo = now.minus(1, ChronoUnit.HOURS);
        String rawJson = String.format("[[\"%s\", \"%s\"], [\"%s\", null]]", fiveHoursAgo, threeHoursAgo, oneHourAgo);
        when(mockRedis.hget("device_on_intervals", "test")).thenReturn(rawJson);
        List<DeviceTrackingService.Interval> returnedIntervals = service.getDeviceIntervals("test");
        List<DeviceTrackingService.Interval> expected = List.of(
                new DeviceTrackingService.Interval(fiveHoursAgo, threeHoursAgo),
                new DeviceTrackingService.Interval(oneHourAgo, null)
        );
        assertIterableEquals(expected, returnedIntervals);
    }

    @Test
    void testGetDeviceIntervals_empty() {
        when(mockRedis.hget("device_on_intervals", "test")).thenReturn(null);
        List<DeviceTrackingService.Interval> returnedIntervals = service.getDeviceIntervals("test");
        List<DeviceTrackingService.Interval> expected = List.of();
        assertIterableEquals(expected, returnedIntervals);
    }

    @Test
    void testSaveDeviceIntervals_valid() {
        Instant now = Instant.now();
        Instant fiveHoursAgo = now.minus(5, ChronoUnit.HOURS);
        Instant threeHoursAgo = now.minus(3, ChronoUnit.HOURS);
        Instant oneHourAgo = now.minus(1, ChronoUnit.HOURS);
        String rawJson = String.format("[[\"%s\",\"%s\"],[\"%s\",null]]", fiveHoursAgo, threeHoursAgo, oneHourAgo);
        List<DeviceTrackingService.Interval> intervals = List.of(
                new DeviceTrackingService.Interval(fiveHoursAgo, threeHoursAgo),
                new DeviceTrackingService.Interval(oneHourAgo, null)
        );
        service.saveDeviceIntervals("test", intervals);
        verify(mockRedis).hset("device_on_intervals", "test", rawJson);
    }

    @Test
    void testStartNewInterval() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Instant now = Instant.now();
        String expected = new DeviceTrackingService.Interval(now, null).toJson();
        service.startNewInterval("test", now);
        verify(mockRedis).rpush(eq("device_on_intervals:test"), captor.capture());
        assertEquals(expected, captor.getValue());
    }

    @Test
    void testCloseLastInterval_valid() {
        Instant now = Instant.now();
        Instant fiveHoursAgo = now.minus(5, ChronoUnit.HOURS);
        Instant threeHoursAgo = now.minus(3, ChronoUnit.HOURS);
        Instant oneHourAgo = now.minus(1, ChronoUnit.HOURS);
        List<DeviceTrackingService.Interval> intervals = new ArrayList<>(List.of(
                new DeviceTrackingService.Interval(fiveHoursAgo, threeHoursAgo),
                new DeviceTrackingService.Interval(oneHourAgo, null)
        ));
        String deviceId = "test";
        when(service.getDeviceIntervals(deviceId)).thenReturn(intervals);
        assertEquals(3600.0, service.closeLastInterval(deviceId, now));
    }

    @Test
    void testCloseLastInterval_empty() {
        List<DeviceTrackingService.Interval> intervals = new ArrayList<>();
        String deviceId = "test";
        when(service.getDeviceIntervals(deviceId)).thenReturn(intervals);
        Exception exception = assertThrows(IllegalStateException.class, () -> service.closeLastInterval(deviceId, Instant.now()));
        assertEquals("No intervals found for device test", exception.getMessage());
    }

    @Test
    void testCloseLastInterval_alreadyClosed() {
        Instant now = Instant.now();
        Instant fiveHoursAgo = now.minus(5, ChronoUnit.HOURS);
        Instant threeHoursAgo = now.minus(3, ChronoUnit.HOURS);
        List<DeviceTrackingService.Interval> intervals = new ArrayList<>(List.of(
                new DeviceTrackingService.Interval(fiveHoursAgo, threeHoursAgo)
        ));
        String deviceId = "test";
        when(service.getDeviceIntervals(deviceId)).thenReturn(intervals);
        Exception exception = assertThrows(IllegalStateException.class, () -> service.closeLastInterval(deviceId, Instant.now()));
        assertEquals("Last interval already closed for device " + deviceId, exception.getMessage());
    }
}
