package nv.nadav.smart_home.service.impl;

import nv.nadav.smart_home.service.CounterManager;
import nv.nadav.smart_home.service.HttpMetricsService;

import nv.nadav.smart_home.service.TimerManager;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class HttpMetricsServiceImpl implements HttpMetricsService {
    private final CounterManager counterManager;
    private final TimerManager timerManager;

    public HttpMetricsServiceImpl(CounterManager counterManager, TimerManager timerManager) {
        this.counterManager = counterManager;
        this.timerManager = timerManager;
    }

    @Override
    public void recordRequest(String method, String endpoint, int statusCode, long durationNanoseconds) {
        counterManager.increment(
                "request_count_total",
                "Total HTTP Request Count",
                Map.of("method", method, "endpoint", endpoint, "status_code", String.valueOf(statusCode))
        );
        timerManager.record(
                "request_latency_seconds",
                "HTTP Request latency",
                Map.of("endpoint", endpoint),
                durationNanoseconds,
                TimeUnit.NANOSECONDS
        );
    }
}
