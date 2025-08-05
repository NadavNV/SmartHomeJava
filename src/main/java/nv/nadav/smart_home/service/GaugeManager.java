package nv.nadav.smart_home.service;

import java.util.Map;

public interface GaugeManager {
    void setNumericGauge(String name, String description, double value, Map<String, String> tags);

    void setBooleanGauge(String name, String description, boolean value, Map<String, String> tags);

    void setEnumGauge(String name, String description, Enum<?> selected, Map<String, String> tags);

    void setScheduleGauge(String name, String description, String deviceId, String scheduledOn, String scheduledOff);
}
