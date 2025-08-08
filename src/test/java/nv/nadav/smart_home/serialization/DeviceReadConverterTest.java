package nv.nadav.smart_home.serialization;

import nv.nadav.smart_home.model.Device;
import nv.nadav.smart_home.model.DeviceType;
import nv.nadav.smart_home.model.parameters.*;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceReadConverterTest {

    private DeviceReadConverter converter;

    @BeforeEach
    void setUp() {
        converter = new DeviceReadConverter();
    }

    @Test
    void convert_shouldReadWaterHeaterParameters() {
        ObjectId id = new ObjectId();

        Document paramsDoc = new Document()
                .append("temperature", 45)
                .append("target_temperature", 50)
                .append("is_heating", true)
                .append("timer_enabled", false)
                .append("scheduled_on", "06:00")
                .append("scheduled_off", "22:00");

        Document doc = new Document()
                .append("_id", id)
                .append("id", "waterHeater1")
                .append("type", "water_heater")
                .append("name", "Water Heater")
                .append("room", "Bathroom")
                .append("status", "on")
                .append("parameters", paramsDoc);

        Device device = converter.convert(doc);

        assertThat(device.get_id()).isEqualTo(id);
        assertThat(device.getDeviceId()).isEqualTo("waterHeater1");
        assertThat(device.getType()).isEqualTo(DeviceType.WATER_HEATER);
        assertThat(device.getName()).isEqualTo("Water Heater");
        assertThat(device.getRoom()).isEqualTo("Bathroom");
        assertThat(device.getStatus()).isEqualTo("on");

        assertThat(device.getParameters()).isInstanceOf(WaterHeaterParameters.class);
        WaterHeaterParameters params = (WaterHeaterParameters) device.getParameters();
        assertThat(params.getTemperature()).isEqualTo(45);
        assertThat(params.getTargetTemperature()).isEqualTo(50);
        assertThat(params.isHeating()).isTrue();
        assertThat(params.isTimerEnabled()).isFalse();
        assertThat(params.getScheduledOn()).isEqualTo("06:00");
        assertThat(params.getScheduledOff()).isEqualTo("22:00");
    }

    @Test
    void convert_shouldReadAirConditionerParameters() {
        Document paramsDoc = new Document()
                .append("temperature", 24)
                .append("mode", "cool")
                .append("swing", "on")
                .append("fan_speed", "high");

        Document doc = new Document()
                .append("id", "ac1")
                .append("type", "air_conditioner")
                .append("parameters", paramsDoc);

        Device device = converter.convert(doc);

        assertThat(device.getType()).isEqualTo(DeviceType.AIR_CONDITIONER);
        assertThat(device.getParameters()).isInstanceOf(AirConditionerParameters.class);
        AirConditionerParameters params = (AirConditionerParameters) device.getParameters();
        assertThat(params.getTemperature()).isEqualTo(24);
        assertThat(params.getMode()).isEqualTo(AirConditionerParameters.Mode.COOL);
        assertThat(params.getSwing()).isEqualTo(AirConditionerParameters.Swing.ON);
        assertThat(params.getFanSpeed()).isEqualTo(AirConditionerParameters.FanSpeed.HIGH);
    }

    @Test
    void convert_shouldReadCurtainParameters() {
        Document paramsDoc = new Document("position", 80);

        Document doc = new Document()
                .append("id", "curtain1")
                .append("type", "curtain")
                .append("parameters", paramsDoc);

        Device device = converter.convert(doc);

        assertThat(device.getType()).isEqualTo(DeviceType.CURTAIN);
        assertThat(device.getParameters()).isInstanceOf(CurtainParameters.class);
        CurtainParameters params = (CurtainParameters) device.getParameters();
        assertThat(params.getPosition()).isEqualTo(80);
    }

    @Test
    void convert_shouldReadDoorLockParameters() {
        Document paramsDoc = new Document()
                .append("battery_level", 75)
                .append("auto_lock_enabled", true);

        Document doc = new Document()
                .append("id", "doorlock1")
                .append("type", "door_lock")
                .append("parameters", paramsDoc);

        Device device = converter.convert(doc);

        assertThat(device.getType()).isEqualTo(DeviceType.DOOR_LOCK);
        assertThat(device.getParameters()).isInstanceOf(DoorLockParameters.class);
        DoorLockParameters params = (DoorLockParameters) device.getParameters();
        assertThat(params.getBatteryLevel()).isEqualTo(75);
        assertThat(params.isAutoLockEnabled()).isTrue();
    }

    @Test
    void convert_shouldReadLightParameters() {
        Document paramsDoc = new Document()
                .append("brightness", 90)
                .append("color", "#FFFFFF")
                .append("is_dimmable", true)
                .append("dynamic_color", false);

        Document doc = new Document()
                .append("id", "light1")
                .append("type", "light")
                .append("parameters", paramsDoc);

        Device device = converter.convert(doc);

        assertThat(device.getType()).isEqualTo(DeviceType.LIGHT);
        assertThat(device.getParameters()).isInstanceOf(LightParameters.class);
        LightParameters params = (LightParameters) device.getParameters();
        assertThat(params.getBrightness()).isEqualTo(90);
        assertThat(params.getColor()).isEqualTo("#FFFFFF");
        assertThat(params.isDimmable()).isTrue();
        assertThat(params.isDynamicColor()).isFalse();
    }
}
