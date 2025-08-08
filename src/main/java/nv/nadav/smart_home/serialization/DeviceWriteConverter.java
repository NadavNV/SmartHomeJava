package nv.nadav.smart_home.serialization;

import nv.nadav.smart_home.model.parameters.*;
import org.springframework.core.convert.converter.Converter;
import nv.nadav.smart_home.model.Device;
import org.bson.Document;

public class DeviceWriteConverter implements Converter<Device, Document> {
    @Override
    public Document convert(Device device) {
        Document doc = new Document();
        if (device.get_id() != null) {
            doc.put("_id", device.get_id());
        }
        doc.put("id", device.getDeviceId());
        doc.put("type", device.getType().name().toLowerCase());
        doc.put("name", device.getName());
        doc.put("room", device.getRoom());
        doc.put("status", device.getStatus());

        Document paramsDoc = new Document();
        DeviceParameters params = device.getParameters();

        if (params instanceof WaterHeaterParameters whp) {
            paramsDoc.put("temperature", whp.getTemperature());
            paramsDoc.put("target_temperature", whp.getTargetTemperature());
            paramsDoc.put("is_heating", whp.isHeating());
            paramsDoc.put("timer_enabled", whp.isTimerEnabled());
            paramsDoc.put("scheduled_on", whp.getScheduledOn());
            paramsDoc.put("scheduled_off", whp.getScheduledOff());
        } else if (params instanceof AirConditionerParameters acp) {
            paramsDoc.put("temperature", acp.getTemperature());
            paramsDoc.put("mode", acp.getMode().getValue());
            paramsDoc.put("swing", acp.getSwing().getValue());
            paramsDoc.put("fan_speed", acp.getFanSpeed().getValue());
        } else if (params instanceof CurtainParameters cp) {
            paramsDoc.put("position", cp.getPosition());
        } else if (params instanceof DoorLockParameters dlp) {
            paramsDoc.put("battery_level", dlp.getBatteryLevel());
            paramsDoc.put("auto_lock_enabled", dlp.isAutoLockEnabled());
        } else if (params instanceof LightParameters lp) {
            paramsDoc.put("brightness", lp.getBrightness());
            paramsDoc.put("color", lp.getColor());
            paramsDoc.put("is_dimmable", lp.isDimmable());
            paramsDoc.put("dynamic_color", lp.isDynamicColor());
        }

        doc.put("parameters", paramsDoc);
        return doc;
    }
}
