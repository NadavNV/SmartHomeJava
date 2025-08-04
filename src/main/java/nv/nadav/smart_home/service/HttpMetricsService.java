package nv.nadav.smart_home.service;

public interface HttpMetricsService {
    void recordRequest(String method, String endpoint, int statusCode, long durationSeconds);
}
