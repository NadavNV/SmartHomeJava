package nv.nadav.smart_home.serialization;

import nv.nadav.smart_home.model.DeviceType;
import nv.nadav.smart_home.model.parameters.*;
import org.springframework.core.convert.converter.Converter;
import nv.nadav.smart_home.model.Device;
import org.bson.Document;

public class DeviceReadConverter implements Converter<Document, Device> {
    @Override
    public Device convert(Document source) {
        Device device = new Device();
        device.set_id(source.getObjectId("_id"));
        device.setDeviceId(source.getString("id"));
        device.setType(DeviceType.valueOf(source.getString("type").toUpperCase()));
        device.setName(source.getString("name"));
        device.setRoom(source.getString("room"));
        device.setStatus(source.getString("status"));

        Document paramsDoc = (Document) source.get("parameters");
        DeviceParameters params = switch (device.getType()) {
            case LIGHT -> mapToLightParameters(paramsDoc);
            case WATER_HEATER -> mapToWaterHeaterParams(paramsDoc);
            case AIR_CONDITIONER -> mapToAcParams(paramsDoc);
            case DOOR_LOCK -> mapToLockParameters(paramsDoc);
            case CURTAIN -> mapToCurtainParams(paramsDoc);
        };

        device.setParameters(params);
        return device;
    }

    private AirConditionerParameters mapToAcParams(Document doc) {
        AirConditionerParameters params = new AirConditionerParameters();
        params.setTemperature(doc.getInteger("temperature"));
        params.setFanSpeed(AirConditionerParameters.FanSpeed.fromValue(doc.getString("fan_speed")));
        params.setMode(AirConditionerParameters.Mode.fromValue(doc.getString("mode")));
        params.setSwing(AirConditionerParameters.Swing.fromValue(doc.getString("swing")));
        return params;
    }

    private CurtainParameters mapToCurtainParams(Document doc) {
        CurtainParameters params = new CurtainParameters();
        params.setPosition(doc.getInteger("position"));
        return params;
    }

    private DoorLockParameters mapToLockParameters(Document doc) {
        DoorLockParameters params = new DoorLockParameters();
        params.setAutoLockEnabled(doc.getBoolean("auto_lock_enabled"));
        params.setBatteryLevel(doc.getInteger("battery_level"));
        return params;
    }

    private LightParameters mapToLightParameters(Document doc) {
        LightParameters params = new LightParameters();
        params.setDynamicColor(doc.getBoolean("dynamic_color"));
        params.setDimmable(doc.getBoolean("is_dimmable"));
        params.setColor(doc.getString("color"));
        params.setBrightness(doc.getInteger("brightness"));
        return params;
    }

    private WaterHeaterParameters mapToWaterHeaterParams(Document doc) {
        WaterHeaterParameters params = new WaterHeaterParameters();
        params.setTemperature(doc.getInteger("temperature"));
        params.setTargetTemperature(doc.getInteger("target_temperature"));
        params.setHeating(doc.getBoolean("is_heating"));
        params.setTimerEnabled(doc.getBoolean("timer_enabled"));
        params.setScheduledOn(doc.getString("scheduled_on"));
        params.setScheduledOff(doc.getString("scheduled_off"));
        return params;
    }
}
