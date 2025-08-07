package nv.nadav.smart_home.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import nv.nadav.smart_home.model.DeviceType;
import nv.nadav.smart_home.model.parameters.DeviceParameters;

public class DeviceDto {
    @NotBlank(message = "Device ID is required")
    private String id;
    @NotNull(message = "Device type must be specified")
    @JsonProperty("type")
    private DeviceType type;
    @NotBlank(message = "Name must be specified")
    private String name;
    @NotBlank(message = "Room must be specified")
    private String room;
    @NotBlank(message = "Status must be specified")
    private String status;
    @JsonProperty("parameters")
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
}