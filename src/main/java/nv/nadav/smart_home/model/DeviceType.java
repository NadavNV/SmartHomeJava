package nv.nadav.smart_home.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DeviceType {
    LIGHT("light"),
    WATER_HEATER("water_heater"),
    AIR_CONDITIONER("air_conditioner"),
    DOOR_LOCK("door_lock"),
    CURTAIN("curtain");

    private final String value;

    DeviceType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }


    @JsonCreator
    public static DeviceType fromString(String input) {
        for (DeviceType type : DeviceType.values()) {
            if (type.value.equalsIgnoreCase(input)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown device type: " + input);
    }

    @Override
    public String toString() {
        return value;
    }
}
