package nv.nadav.smart_home.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import nv.nadav.smart_home.service.GaugeManager;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import com.google.common.util.concurrent.AtomicDouble;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class GaugeManagerImpl implements GaugeManager {

    private final MeterRegistry registry;
    private final Cache<@NotNull String, AtomicDouble> gauges = Caffeine.newBuilder()
            .maximumSize(1_000)
            .expireAfterAccess(168, TimeUnit.HOURS)  // One week
            .build();

    public GaugeManagerImpl(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void setBooleanGauge(String name, String description, boolean value, Map<String, String> tags) {
        setNumericGauge(name, description, value ? 1.0 : 0.0, tags);
    }

    @Override
    public void setEnumGauge(String name, String description, Enum<?> selected, Map<String, String> baseTags) {
        Class<? extends Enum<?>> enumClass = selected.getDeclaringClass();
        for (Enum<?> value : enumClass.getEnumConstants()) {
            Map<String, String> tags = new java.util.HashMap<>(baseTags);
            tags.put("mode", value.name().toLowerCase());
            setBooleanGauge(name, description, value == selected, tags);
        }
    }

    @Override
    public void setScheduleGauge(String name, String description, String deviceId, String scheduledOn, String scheduledOff) {
        Map<String, String> newTags = Map.of(
                "device_id", deviceId,
                "scheduled_on", scheduledOn,
                "scheduled_off", scheduledOff
        );

        String prefix = name + "|device_id=" + deviceId;

        // Reset all existing gauges with same device_id
        gauges.asMap().entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix))
                .forEach(e -> e.getValue().set(0.0));

        setNumericGauge(name, description, 1.0, newTags);
    }

    @Override
    public void setNumericGauge(String name, String description, double value, Map<String, String> tags) {
        String key = buildKey(name, tags);
        AtomicDouble ref = gauges.asMap().computeIfAbsent(key, k -> {
            AtomicDouble newRef = new AtomicDouble(value);
            Gauge.Builder<AtomicDouble> builder = Gauge.builder(name, newRef, AtomicDouble::get).description(description);
            tags.forEach(builder::tag);
            builder.strongReference(true).register(registry);
            return newRef;
        });
        ref.set(value);
    }

    private String buildKey(String name, Map<String, String> tags) {
        return name + "|" + tags.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(","));
    }
}
