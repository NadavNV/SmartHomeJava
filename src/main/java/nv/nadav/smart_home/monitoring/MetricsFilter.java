package nv.nadav.smart_home.monitoring;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nv.nadav.smart_home.service.HttpMetricsService;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class MetricsFilter extends OncePerRequestFilter {
    private final HttpMetricsService metricsService;

    public MetricsFilter(HttpMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.nanoTime();
        filterChain.doFilter(request, response);
        long duration = System.nanoTime() - start;

        metricsService.recordRequest(
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                TimeUnit.NANOSECONDS.toMillis(duration)
        );
    }
}
