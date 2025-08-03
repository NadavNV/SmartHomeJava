package nv.nadav.smart_home.constants;

import java.util.Set;

public class Constants {
    // Scalar numeric limits
    public static final int MIN_WATER_TEMP = 49;
    public static final int MAX_WATER_TEMP = 60;
    public static final int MIN_AC_TEMP = 16;
    public static final int MAX_AC_TEMP = 30;
    public static final int MIN_BRIGHTNESS = 0;
    public static final int MAX_BRIGHTNESS = 100;
    public static final int MIN_BATTERY = 0;
    public static final int MAX_BATTERY = 100;
    public static final int MIN_POSITION = 0;
    public static final int MAX_POSITION = 100;

    // Valid statuses
    public static final Set<String> AC_STATUSES = Set.of("on", "off");
    public static final Set<String> CURTAIN_STATUSES = Set.of("open", "closed");
    public static final Set<String> DOOR_LOCK_STATUSES = Set.of("locked", "unlocked");
    public static final Set<String> LIGHT_STATUSES = Set.of("on", "off");
    public static final Set<String> WATER_HEATER_STATUSES = Set.of("on", "off");

    // Water heater defaults
    public static final String DEFAULT_WATER_HEATER_STATUS = "off";
    public static final int DEFAULT_WATER_TEMP = 60;
    public static final boolean DEFAULT_IS_HEATING = false;
    public static final boolean DEFAULT_TIMER_ENABLED = false;
    public static final String DEFAULT_START_TIME = "06:30";
    public static final String DEFAULT_STOP_TIME = "08:00";

    // Air conditioner defaults
    public static final String DEFAULT_AC_STATUS = "off";
    public static final int DEFAULT_AC_TEMP = 24;
    public static final String DEFAULT_AC_MODE = "cool";
    public static final String DEFAULT_AC_FAN = "low";
    public static final String DEFAULT_AC_SWING = "off";

    // Light defaults
    public static final String DEFAULT_LIGHT_STATUS = "off";
    public static final boolean DEFAULT_DIMMABLE = false;
    public static final int DEFAULT_BRIGHTNESS = 80;
    public static final boolean DEFAULT_DYNAMIC_COLOR = false;
    public static final String DEFAULT_LIGHT_COLOR = "#FFFFFF";

    // Door lock defaults
    public static final String DEFAULT_LOCK_STATUS = "unlocked";
    public static final boolean DEFAULT_AUTO_LOCK_ENABLED = false;
    public static final int DEFAULT_BATTERY = 100;

    // Curtain default
    public static final String DEFAULT_CURTAIN_STATUS = "open";
    public static final int DEFAULT_POSITION = 100;

    // Regex patterns
    public static final String TIME_REGEX = "^([01][0-9]|2[0-3]):([0-5][0-9])(:[0-5][0-9])?$";
    public static final String COLOR_REGEX = "^#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6})$";
}
