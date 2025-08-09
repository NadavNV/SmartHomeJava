package nv.nadav.smart_home.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import nv.nadav.smart_home.model.DeviceType;
import nv.nadav.smart_home.model.parameters.DeviceParameters;
import nv.nadav.smart_home.serialization.DelegatingParametersDeserializer;
import nv.nadav.smart_home.serialization.DeviceParametersDeserializer;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JsonIgnoreProperties(ignoreUnknown = false)
public class DeviceUpdateDto {
    private String name;
    private String room;
    private String status;
    @JsonDeserialize(using = DelegatingParametersDeserializer.class)
    private DeviceParameters parameters;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public DeviceParameters getParameters() {
        return parameters;
    }

    public void setParameters(DeviceParameters parameters) {
        this.parameters = parameters;
    }

    public static DeviceUpdateDto fromDto(DeviceDto dto) {
        DeviceUpdateDto updateDto = new DeviceUpdateDto();
        updateDto.setName(dto.getName());
        updateDto.setRoom(dto.getRoom());
        updateDto.setStatus(dto.getStatus());
        updateDto.setParameters(dto.getParameters());
        return updateDto;
    }

    public static DeviceUpdateDto deserialize(String json, DeviceType deviceType) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        DelegatingParametersDeserializer.delegate.set(new DeviceParametersDeserializer(deviceType));
        try {
            return mapper.readValue(json, DeviceUpdateDto.class);
        } finally {
            DelegatingParametersDeserializer.delegate.remove();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            // Same reference
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            // null or different class
            return false;
        }

        DeviceUpdateDto other = (DeviceUpdateDto) obj;
        return (Objects.equals(this.name, other.getName()) &&
                Objects.equals(this.room, other.getRoom()) &&
                Objects.equals(this.status, other.getStatus()) &&
                Objects.equals(this.parameters, other.getParameters())
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, room, status, parameters);
    }

    @Override
    public String toString() {
        String result = Stream.of(
                        Map.entry("Name", name),
                        Map.entry("Room", room),
                        Map.entry("Status", status),
                        Map.entry("Parameters", parameters)
                )
                .filter(e -> e.getValue() != null)
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(", ", "{", "}"));
        return ((result.equals("{}")) ? "{Empty}" : result);
    }
}
