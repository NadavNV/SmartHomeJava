package nv.nadav.smart_home.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import nv.nadav.smart_home.dto.DeviceDto;
import nv.nadav.smart_home.model.DeviceType;
import nv.nadav.smart_home.model.parameters.*;

import java.io.IOException;

public class DeviceDtoDeserializer extends StdDeserializer<DeviceDto> {

    public DeviceDtoDeserializer() {
        super(DeviceDto.class);
    }

    @Override
    public DeviceDto deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        ObjectMapper mapper = (ObjectMapper) parser.getCodec();
        ObjectNode node = mapper.readTree(parser);

        DeviceType type = DeviceType.fromString(node.get("type").asText());

        DeviceDto dto = new DeviceDto();
        dto.setType(type);
        dto.setStatus(node.get("status").asText());
        dto.setId(node.get("id").asText());
        dto.setName(node.get("name").asText());
        dto.setRoom(node.get("room").asText());

        JsonNode parametersNode = node.get("parameters");

        DeviceParameters parameters = switch (type) {
            case LIGHT -> mapper.treeToValue(parametersNode, LightParameters.class);
            case WATER_HEATER -> mapper.treeToValue(parametersNode, WaterHeaterParameters.class);
            case AIR_CONDITIONER -> mapper.treeToValue(parametersNode, AirConditionerParameters.class);
            case DOOR_LOCK -> mapper.treeToValue(parametersNode, DoorLockParameters.class);
            case CURTAIN -> mapper.treeToValue(parametersNode, CurtainParameters.class);
        };

        dto.setParameters(parameters);
        return dto;
    }
}
