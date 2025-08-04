package nv.nadav.smart_home.service.impl;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import nv.nadav.smart_home.service.HttpMetricsService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class HttpMetricsServiceImpl implements HttpMetricsService {
    private final MeterRegistry registry;

    public HttpMetricsServiceImpl(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void recordRequest(String method, String endpoint, int statusCode, long durationSeconds) {
        Counter.builder("request_count_total")
                .description("Total HTTP Request Count")
                .tags("method", method, "endpoint", endpoint, "status_code", String.valueOf(statusCode))
                .register(registry)
                .increment();

        Timer.builder("request_latency_seconds")
                .description("HTTP Request latency")
                .tags("endpoint", endpoint)
                .register(registry)
                .record(durationSeconds, TimeUnit.MILLISECONDS);
    }
}
