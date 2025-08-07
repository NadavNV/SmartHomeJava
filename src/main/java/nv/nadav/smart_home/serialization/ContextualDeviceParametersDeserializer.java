package nv.nadav.smart_home.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import nv.nadav.smart_home.model.DeviceType;
import nv.nadav.smart_home.model.parameters.*;

import java.io.IOException;

public class ContextualDeviceParametersDeserializer extends JsonDeserializer<DeviceParameters> implements ContextualDeserializer {
    private final DeviceType type;

    public ContextualDeviceParametersDeserializer() {
        type = null;
    }

    public ContextualDeviceParametersDeserializer(DeviceType type) {
        this.type = type;
    }

    @Override
    public DeviceParameters deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        ObjectCodec codec = jsonParser.getCodec();
        JsonNode node = codec.readTree(jsonParser);

        if (type == null) {
            throw new IllegalStateException("Device type is not set for DeviceParameters deserialization.");
        }

        Class<? extends DeviceParameters> targetClass = switch (type) {
            case LIGHT -> LightParameters.class;
            case WATER_HEATER -> WaterHeaterParameters.class;
            case AIR_CONDITIONER -> AirConditionerParameters.class;
            case DOOR_LOCK -> DoorLockParameters.class;
            case CURTAIN -> CurtainParameters.class;
            default -> throw new IllegalArgumentException("Unsupported device type: " + type);
        };

        return codec.treeToValue(node, targetClass);
    }

    @Override
    public JsonDeserializer<?> createContextual(
            DeserializationContext deserializationContext,
            BeanProperty beanProperty
    ) throws JsonMappingException {
        // Extract parent object
        JsonNode rootNode = null;
        try {
            JsonParser parser = deserializationContext.getParser();
            rootNode = parser.readValueAsTree();
        } catch (IOException e) {
            throw JsonMappingException.from(deserializationContext, "Failed to read parent JSON tree");
        }

        if (rootNode.has("type")) {
            DeviceType type = DeviceType.fromString(rootNode.get("type").asText());
            return new DeviceParametersDeserializer(type);
        }

        throw JsonMappingException.from(
                deserializationContext,
                "Device type field missing when deserializing DeviceParameters"
        );
    }
}
