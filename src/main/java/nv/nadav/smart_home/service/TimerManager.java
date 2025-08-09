package nv.nadav.smart_home.service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface TimerManager {
    void record(String name, String description, Map<String, String> tags, long duration, TimeUnit timeUnits);
}
