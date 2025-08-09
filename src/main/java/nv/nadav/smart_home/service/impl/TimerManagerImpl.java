package nv.nadav.smart_home.service.impl;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import nv.nadav.smart_home.service.TimerManager;
import org.springframework.stereotype.Service;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class TimerManagerImpl implements TimerManager {
    private final MeterRegistry registry;
    private final Cache<@NotNull String, Timer> cache = Caffeine.newBuilder().maximumSize(500).build();

    public TimerManagerImpl(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void record(String name, String description, Map<String, String> tags, long duration, TimeUnit timeUnits) {
        String key = buildKey(name, tags);
        Timer timer = cache.asMap().computeIfAbsent(key, _ -> {
            Timer.Builder builder = Timer.builder(name).description(description);
            tags.forEach(builder::tag);
            return builder.register(registry);
        });

        timer.record(duration, timeUnits);
    }

    private @NotNull String buildKey(@NotNull String name, Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) {
            return name;
        }
        return name + "|" + tags.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(","));
    }
}
