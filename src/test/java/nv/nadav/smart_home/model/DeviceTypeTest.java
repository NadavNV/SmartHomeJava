package nv.nadav.smart_home.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeviceTypeTest {

    @Test
    void testFromStringValid() {
        assertEquals(DeviceType.LIGHT, DeviceType.fromString("light"));
        assertEquals(DeviceType.AIR_CONDITIONER, DeviceType.fromString("AIR_CONDITIONER"));
    }

    @Test
    void testFromStringInvalid() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> DeviceType.fromString("toaster"));
        assertTrue(exception.getMessage().contains("Unknown device type"));
    }

    @Test
    void testToString() {
        assertEquals("curtain", DeviceType.CURTAIN.toString());
    }
}
