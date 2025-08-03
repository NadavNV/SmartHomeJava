package nv.nadav.smart_home.model.parameters;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import nv.nadav.smart_home.validation.Validators.ValidationResult;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = WaterHeaterParameters.class, name = "water_heater"),
        @JsonSubTypes.Type(value = LightParameters.class, name = "light"),
        @JsonSubTypes.Type(value = AirConditionerParameters.class, name = "air_conditioner"),
        @JsonSubTypes.Type(value = CurtainParameters.class, name = "curtain"),
        @JsonSubTypes.Type(value = DoorLockParameters.class, name = "door_lock"),
})
public abstract class DeviceParameters {
    public abstract ValidationResult validate(boolean isUpdate);
}
