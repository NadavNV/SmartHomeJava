package nv.nadav.smart_home.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import nv.nadav.smart_home.model.DeviceType;
import nv.nadav.smart_home.model.parameters.*;

import java.io.IOException;

public class DeviceParametersDeserializer extends JsonDeserializer<DeviceParameters> {
    private final DeviceType type;

    public DeviceParametersDeserializer(DeviceType type) {
        this.type = type;
    }

    @Override
    public DeviceParameters deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        ObjectCodec codec = jsonParser.getCodec();
        JsonNode node = codec.readTree(jsonParser);

        Class<? extends DeviceParameters> targetClass = switch (type) {
            case LIGHT -> LightParameters.class;
            case WATER_HEATER -> WaterHeaterParameters.class;
            case AIR_CONDITIONER -> AirConditionerParameters.class;
            case DOOR_LOCK -> DoorLockParameters.class;
            case CURTAIN -> CurtainParameters.class;
            default -> throw new IllegalArgumentException("Unsupported type: " + type);
        };

        return codec.treeToValue(node, targetClass);
    }
}
