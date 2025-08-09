package nv.nadav.smart_home.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import nv.nadav.smart_home.model.DeviceType;
import nv.nadav.smart_home.serialization.DeviceDtoDeserializer;
import nv.nadav.smart_home.model.parameters.DeviceParameters;

import java.util.Objects;

@JsonDeserialize(using = DeviceDtoDeserializer.class)
public class DeviceDto {
    @NotBlank(message = "Device ID is required")
    private String id;
    @NotNull(message = "Device type must be specified")
    private DeviceType type;
    @NotBlank(message = "Name must be specified")
    private String name;
    @NotBlank(message = "Room must be specified")
    private String room;
    @NotBlank(message = "Status must be specified")
    private String status;
    @NotNull(message = "Parameters must be provided")
    private DeviceParameters parameters;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public DeviceType getType() {
        return type;
    }

    public void setType(DeviceType type) {
        this.type = type;
    }

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

        DeviceDto other = (DeviceDto) obj;
        return (Objects.equals(this.id, other.getId()) &&
                Objects.equals(this.type, other.getType()) &&
                Objects.equals(this.name, other.getName()) &&
                Objects.equals(this.room, other.getRoom()) &&
                Objects.equals(this.status, other.getStatus()) &&
                Objects.equals(this.parameters, other.getParameters())
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, name, room, status, parameters);
    }

    @Override
    public String toString() {
        return String.format(
                "{ID: %s, Type: %s, Name: %s, Room: %s, Status: %s, Parameters: %s}",
                id, type, name, room, status, parameters
        );
    }
}