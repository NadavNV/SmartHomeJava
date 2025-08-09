package nv.nadav.smart_home.model;

import nv.nadav.smart_home.dto.DeviceDto;
import nv.nadav.smart_home.dto.DeviceUpdateDto;
import nv.nadav.smart_home.model.parameters.DeviceParameters;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Objects;

@Document(collection = "devices")
public class Device {
    @Id
    private ObjectId _id;
    @Indexed(unique = true)
    @Field("id")
    private String deviceId;
    private DeviceType type;
    private String name;
    private String room;
    private String status;
    private DeviceParameters parameters;

    public ObjectId get_id() {
        return _id;
    }

    public void set_id(ObjectId _id) {
        this._id = _id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
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

    public static Device fromDto(DeviceDto deviceDto) {
        Device newDevice = new Device();
        newDevice.setDeviceId(deviceDto.getId());
        newDevice.setName(deviceDto.getName());
        newDevice.setParameters(deviceDto.getParameters());
        newDevice.setRoom(deviceDto.getRoom());
        newDevice.setStatus(deviceDto.getStatus());
        newDevice.setType(deviceDto.getType());
        return newDevice;
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
        Device other = (Device) obj;
        return (Objects.equals(this._id, other.get_id()) &&
                Objects.equals(this.deviceId, other.getDeviceId()) &&
                Objects.equals(this.name, other.getName()) &&
                Objects.equals(this.type, other.getType()) &&
                Objects.equals(this.room, other.getRoom()) &&
                Objects.equals(this.status, other.getStatus()) &&
                Objects.equals(this.parameters, other.getParameters())
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(_id, deviceId, name, room, type, status, parameters);
    }
}