package nv.nadav.smart_home.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import nv.nadav.smart_home.service.CounterManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CounterManagerImpl implements CounterManager {
    private final MeterRegistry registry;
    private final Cache<@NotNull String, Counter> counters = Caffeine.newBuilder().maximumSize(1_000).build();

    public CounterManagerImpl(MeterRegistry registry) {
        this.registry = registry;
    }

    public void increment(String name, String description, Map<String, String> tags) {
        String key = buildKey(name, tags);
        Counter counter = counters.asMap().computeIfAbsent(key, _ -> {
            Counter.Builder builder = Counter.builder(name).description(description);
            tags.forEach(builder::tag);
            return builder.register(registry);
        });
        counter.increment();
    }

    public void incrementBy(String name, String description, Map<String, String> tags, double amount) {
        String key = buildKey(name, tags);
        Counter counter = counters.asMap().computeIfAbsent(key, _ -> {
            Counter.Builder builder = Counter.builder(name).description(description);
            tags.forEach(builder::tag);
            return builder.register(registry);
        });
        counter.increment(amount);
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
