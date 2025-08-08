package nv.nadav.smart_home.serialization;

import nv.nadav.smart_home.model.Device;
import nv.nadav.smart_home.model.DeviceType;
import nv.nadav.smart_home.model.parameters.*;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceWriteConverterTest {

    private DeviceWriteConverter converter;

    @BeforeEach
    void setUp() {
        converter = new DeviceWriteConverter();
    }

    @Test
    void convert_shouldWriteBasicFields() {
        Device device = new Device();
        ObjectId id = new ObjectId();
        device.set_id(id);
        device.setDeviceId("device123");
        device.setType(DeviceType.WATER_HEATER);
        device.setName("My Device");
        device.setRoom("Kitchen");
        device.setStatus("on");

        WaterHeaterParameters params = new WaterHeaterParameters();
        params.setTemperature(45);
        params.setTargetTemperature(50);
        params.setHeating(true);
        params.setTimerEnabled(false);
        params.setScheduledOn("06:00");
        params.setScheduledOff("22:00");

        device.setParameters(params);

        Document doc = converter.convert(device);

        assertThat(doc.getObjectId("_id")).isEqualTo(id);
        assertThat(doc.getString("id")).isEqualTo("device123");
        assertThat(doc.getString("type")).isEqualTo("water_heater");
        assertThat(doc.getString("name")).isEqualTo("My Device");
        assertThat(doc.getString("room")).isEqualTo("Kitchen");
        assertThat(doc.getString("status")).isEqualTo("on");

        Document parameters = (Document) doc.get("parameters");
        assertThat(parameters).isNotNull();
        assertThat(parameters.getInteger("temperature")).isEqualTo(45);
        assertThat(parameters.getInteger("target_temperature")).isEqualTo(50);
        assertThat(parameters.getBoolean("is_heating")).isTrue();
        assertThat(parameters.getBoolean("timer_enabled")).isFalse();
        assertThat(parameters.getString("scheduled_on")).isEqualTo("06:00");
        assertThat(parameters.getString("scheduled_off")).isEqualTo("22:00");
    }

    @Test
    void convert_shouldWriteAirConditionerParameters() {
        Device device = new Device();
        device.setDeviceId("ac1");
        device.setType(DeviceType.AIR_CONDITIONER);

        AirConditionerParameters params = new AirConditionerParameters();
        params.setTemperature(24);
        params.setMode(AirConditionerParameters.Mode.COOL);
        params.setSwing(AirConditionerParameters.Swing.ON);
        params.setFanSpeed(AirConditionerParameters.FanSpeed.HIGH);

        device.setParameters(params);

        Document doc = converter.convert(device);

        Document parameters = (Document) doc.get("parameters");
        assertThat(parameters.getInteger("temperature")).isEqualTo(24);
        assertThat(parameters.getString("mode")).isEqualTo("cool");
        assertThat(parameters.getString("swing")).isEqualTo("on");
        assertThat(parameters.getString("fan_speed")).isEqualTo("high");
    }

    @Test
    void convert_shouldWriteCurtainParameters() {
        Device device = new Device();
        device.setDeviceId("curtain1");
        device.setType(DeviceType.CURTAIN);

        CurtainParameters params = new CurtainParameters();
        params.setPosition(75);

        device.setParameters(params);

        Document doc = converter.convert(device);

        Document parameters = (Document) doc.get("parameters");
        assertThat(parameters.getInteger("position")).isEqualTo(75);
    }

    @Test
    void convert_shouldWriteDoorLockParameters() {
        Device device = new Device();
        device.setDeviceId("doorlock1");
        device.setType(DeviceType.DOOR_LOCK);

        DoorLockParameters params = new DoorLockParameters();
        params.setBatteryLevel(85);
        params.setAutoLockEnabled(true);

        device.setParameters(params);

        Document doc = converter.convert(device);

        Document parameters = (Document) doc.get("parameters");
        assertThat(parameters.getInteger("battery_level")).isEqualTo(85);
        assertThat(parameters.getBoolean("auto_lock_enabled")).isTrue();
    }

    @Test
    void convert_shouldWriteLightParameters() {
        Device device = new Device();
        device.setDeviceId("light1");
        device.setType(DeviceType.LIGHT);

        LightParameters params = new LightParameters();
        params.setBrightness(80);
        params.setColor("#FFEEAA");
        params.setDimmable(true);
        params.setDynamicColor(false);

        device.setParameters(params);

        Document doc = converter.convert(device);

        Document parameters = (Document) doc.get("parameters");
        assertThat(parameters.getInteger("brightness")).isEqualTo(80);
        assertThat(parameters.getString("color")).isEqualTo("#FFEEAA");
        assertThat(parameters.getBoolean("is_dimmable")).isTrue();
        assertThat(parameters.getBoolean("dynamic_color")).isFalse();
    }

    @Test
    void convert_shouldWriteEmptyParametersWhenNull() {
        Device device = new Device();
        device.setDeviceId("nullparams");
        device.setType(DeviceType.LIGHT);
        device.setParameters(null);

        Document doc = converter.convert(device);

        Document parameters = (Document) doc.get("parameters");
        assertThat(parameters).isEmpty();
    }
}
