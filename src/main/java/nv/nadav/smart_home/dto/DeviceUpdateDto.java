package nv.nadav.smart_home.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import nv.nadav.smart_home.model.parameters.DeviceParameters;
import nv.nadav.smart_home.serialization.DelegatingParametersDeserializer;

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

    public static DeviceUpdateDto from(DeviceDto dto) {
        DeviceUpdateDto updateDto = new DeviceUpdateDto();
        updateDto.setName(dto.getName());
        updateDto.setRoom(dto.getRoom());
        updateDto.setStatus(dto.getStatus());
        updateDto.setParameters(dto.getParameters());
        return updateDto;
    }
}
