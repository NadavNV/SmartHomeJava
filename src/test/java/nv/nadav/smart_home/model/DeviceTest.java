package nv.nadav.smart_home.model;

import nv.nadav.smart_home.dto.DeviceDto;
import nv.nadav.smart_home.model.parameters.DeviceParameters;
import nv.nadav.smart_home.model.parameters.LightParameters;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeviceTest {

    @Test
    void testFromDto() {
        DeviceDto dto = new DeviceDto();
        dto.setId("dev-1");
        dto.setName("Lamp");
        dto.setRoom("Living Room");
        dto.setStatus("ON");
        dto.setType(DeviceType.LIGHT);

        DeviceParameters mockParams = new LightParameters();
        dto.setParameters(mockParams);

        Device device = Device.fromDto(dto);

        assertEquals("dev-1", device.getDeviceId());
        assertEquals("Lamp", device.getName());
        assertEquals("Living Room", device.getRoom());
        assertEquals("ON", device.getStatus());
        assertEquals(DeviceType.LIGHT, device.getType());
        assertEquals(mockParams, device.getParameters());
    }
}
