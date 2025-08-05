package nv.nadav.smart_home.service;

import java.util.Map;

public interface CounterManager {
    void increment(String name, String description, Map<String, String> tags);

    void incrementBy(String name, String description, Map<String, String> tags, double amount);
}
