package nv.nadav.smart_home.validation;

import nv.nadav.smart_home.dto.DeviceDto;
import nv.nadav.smart_home.dto.DeviceUpdateDto;
import nv.nadav.smart_home.model.DeviceType;
import nv.nadav.smart_home.model.parameters.DoorLockParameters;
import nv.nadav.smart_home.model.parameters.LightParameters;
import nv.nadav.smart_home.model.parameters.WaterHeaterParameters;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;
import static nv.nadav.smart_home.validation.Validators.*;
import static org.junit.jupiter.api.Assertions.*;
import static nv.nadav.smart_home.constants.Constants.*;

public class ValidatorsTest {
    private MockedStatic<LoggerFactory> mockedLoggerFactory;
    private Logger mockLogger;

    private static String intToHexColor(int num) {
        return String.format("#%06x", num);
    }

    @BeforeEach
    void setUp() {
        mockLogger = mock(Logger.class);
        mockedLoggerFactory = Mockito.mockStatic(LoggerFactory.class);
        mockedLoggerFactory.when(() -> LoggerFactory.getLogger("smart_home.validation")).thenReturn(mockLogger);
    }

    @AfterEach
    void tearDown() {
        mockedLoggerFactory.close();
    }

    @Test
    void testVerifyTypeAndRangeIntValid() {
        assertTrue(verifyTypeAndRange(50, "test", int.class, List.of(49, 60)).isValid());
    }

    void testVerifyTypeAndRangeIntValidFromString() {
        assertTrue(verifyTypeAndRange("50", "test", int.class, List.of(49, 60)).isValid());
    }

    @Test
    void testVerifyTypeAndRangeIntInvalidOutOfRange() {
        assertFalse(verifyTypeAndRange(70, "test", int.class, List.of(49, 60)).isValid());
        String error = String.format("'%s' must be between %d and %d, got %d instead.", "test", 49, 60, 70);
        verify(mockLogger).error(error);
    }

    @Test
    void testVerifyTypeAndRangeIntInvalidString() {
        assertFalse(verifyTypeAndRange("Steve", "test", int.class, List.of(49, 60)).isValid());
        String error = String.format("'%s' must be a numeric string, got '%s' instead.", "test", "Steve");
        verify(mockLogger).error(error);
    }
    
    @Test
    void testVerifyTypeAndRangeStringValid() {
        assertTrue(verifyTypeAndRange("on", "status", String.class, Set.of("on", "off")).isValid());
    }

    @Test
    void testVerifyTypeAndRangeStringInvalid() {
        assertFalse(verifyTypeAndRange("maybe", "status", String.class, Set.of("on", "off")).isValid());
        String error = String.format(
                "'%s' is not a valid value for %s. Must be one of %s.", "maybe", "status", Set.of("on", "off")
        );
        verify(mockLogger).error(error);
    }

    @Test
    void testVerifyTypeAndRangeTime() {
        assertTrue(verifyTypeAndRange("14:30", "test", String.class, "time").isValid());
        assertFalse(verifyTypeAndRange("25:30", "test", String.class, "time").isValid());
        assertFalse(verifyTypeAndRange("14:69", "test", String.class, "time").isValid());
        assertFalse(verifyTypeAndRange("1:69", "test", String.class, "time").isValid());
        assertFalse(verifyTypeAndRange("14:3", "test", String.class, "time").isValid());
    }

    @Test
    void testVerifyTypeAndRangeColor() {
        assertTrue(verifyTypeAndRange("#FFF", "color", String.class, "color").isValid());
        assertTrue(verifyTypeAndRange("#ffcc00", "color", String.class, "color").isValid());
        assertFalse(verifyTypeAndRange("blue", "color", String.class, "color").isValid());
    }

    @TestFactory
    Stream<DynamicTest> testManyColorValues() {
        Random rand = new Random();

        return Stream.iterate(0, n -> n < (1 << 24), n -> n + rand.nextInt(600) + 400)
                .flatMap(num -> {
                    List<DynamicTest> tests = new ArrayList<>(2);

                    String hexColor = intToHexColor(num);
                    tests.add(DynamicTest.dynamicTest("Hex: " + hexColor, () ->
                            assertTrue(verifyTypeAndRange(hexColor, "test", String.class, "color").isValid())
                    ));

                    if (num < (1 << 12)) {
                        String shortHex = "#" + String.format("%03X", num);
                        tests.add(DynamicTest.dynamicTest("Short: " + shortHex, () ->
                                assertTrue(verifyTypeAndRange(shortHex, "test", String.class, "color").isValid())
                        ));
                    }

                    return tests.stream();
                });
    }

    @Test
    void testVerifyTypeAndRangeWrongType() {
        assertFalse(verifyTypeAndRange(123, "test", String.class, Set.of("on, off")).isValid());
        String error = "test" + " must be a " + String.class.getSimpleName() + ", got " +
                Integer.valueOf(123).getClass().getSimpleName() + " instead.";
        verify(mockLogger).error(error);
    }

    @Test
    void testValidateNewDeviceDataValidLight() {
        DeviceDto device = new DeviceDto();
        device.setId("light01");
        device.setName("Ceiling Light");
        device.setRoom("Kitchen");
        device.setType(DeviceType.LIGHT);
        device.setStatus("on");
        LightParameters parameters = new LightParameters();
        parameters.setBrightness((MIN_BRIGHTNESS + MAX_BRIGHTNESS) / 2);
        parameters.setColor("#FFAA00");
        parameters.setDimmable(true);
        parameters.setDynamicColor(false);
        device.setParameters(parameters);
        assertTrue(validateNewDeviceData(device).isValid());
    }

    @Test
    void testValidateNewDeviceDataInvalidStatus() {
        DeviceDto device = new DeviceDto();
        device.setId("light01");
        device.setName("Ceiling Light");
        device.setRoom("Kitchen");
        device.setType(DeviceType.LIGHT);
        device.setStatus("Steve");
        LightParameters parameters = new LightParameters();
        parameters.setBrightness((MIN_BRIGHTNESS + MAX_BRIGHTNESS) / 2);
        parameters.setColor("#FFAA00");
        parameters.setDimmable(true);
        parameters.setDynamicColor(false);
        device.setParameters(parameters);
        ValidationResult result = validateNewDeviceData(device);
        assertFalse(result.isValid());
        assertEquals(1, result.errorMessages().size());
    }

    @Test
    void testValidateNewDeviceDataInvalidParameters() {
        DeviceDto device = new DeviceDto();
        device.setId("light01");
        device.setName("Ceiling Light");
        device.setRoom("Kitchen");
        device.setType(DeviceType.LIGHT);
        device.setStatus("off");
        LightParameters parameters = new LightParameters();
        parameters.setBrightness(MAX_BRIGHTNESS + 2);
        parameters.setColor("#DAMIAN");
        parameters.setDimmable(true);
        parameters.setDynamicColor(false);
        device.setParameters(parameters);
        ValidationResult result = validateNewDeviceData(device);
        assertFalse(result.isValid());
        assertEquals(2, result.errorMessages().size());
    }


    @Test
    void testTimeRegex() {
        assertTrue("23:59".matches(TIME_REGEX));
        assertFalse("24:59".matches(TIME_REGEX));
    }

    @Test
    void testColorRegex() {
        assertTrue("#abc".matches(COLOR_REGEX));
        assertTrue("#A1B2C3".matches(COLOR_REGEX));
        assertFalse("abc".matches(COLOR_REGEX));
    }

    @Test
    void testValidateDeviceDataValidLock() {
        DeviceUpdateDto device = new DeviceUpdateDto();
        DoorLockParameters parameters = new DoorLockParameters();
        device.setStatus("locked");
        parameters.setBatteryLevel((MIN_BATTERY + MAX_BATTERY) / 2);
        device.setParameters(parameters);
        assertTrue(validateDeviceData(device, DeviceType.DOOR_LOCK).isValid());
    }

    @Test
    void testValidateDeviceDataWrongType() {
        DeviceUpdateDto device = new DeviceUpdateDto();
        DoorLockParameters parameters = new DoorLockParameters();
        device.setStatus("locked");
        parameters.setBatteryLevel((MIN_BATTERY + MAX_BATTERY) / 2);
        device.setParameters(parameters);
        assertFalse(validateDeviceData(device, DeviceType.LIGHT).isValid());
    }

    @Test
    void testValidateDeviceDataInvalidStatus() {
        DeviceUpdateDto device = new DeviceUpdateDto();
        DoorLockParameters parameters = new DoorLockParameters();
        device.setStatus("open");
        parameters.setBatteryLevel((MIN_BATTERY + MAX_BATTERY) / 2);
        device.setParameters(parameters);
        assertFalse(validateDeviceData(device, DeviceType.DOOR_LOCK).isValid());
    }

    @Test
    void testValidateDeviceDataInvalidReadOnly() {
        DeviceUpdateDto device = new DeviceUpdateDto();
        LightParameters parameters = new LightParameters();
        parameters.setDimmable(true);
        device.setParameters(parameters);
        assertFalse(validateDeviceData(device, DeviceType.LIGHT).isValid());
    }

    @Test
    void testValidateDeviceDataValidWaterHeater() {
        DeviceUpdateDto device = new DeviceUpdateDto();
        WaterHeaterParameters parameters = new WaterHeaterParameters();
        parameters.setTargetTemperature((MIN_WATER_TEMP + MAX_WATER_TEMP) / 2);
        parameters.setScheduledOff("10:00");
        device.setStatus("on");
        device.setParameters(parameters);
        assertTrue(validateDeviceData(device, DeviceType.WATER_HEATER).isValid());
    }



    def test_invalid_water_heater_too_hot(self):
    device = {
        "status": "on",
                "parameters": {
            "temperature": (MIN_WATER_TEMP + MAX_WATER_TEMP) // 2,
            "target_temperature": MAX_WATER_TEMP + 1,
                    "is_heating": True,
                    "timer_enabled": False,
                    "scheduled_on": "08:00",
                    "scheduled_off": "10:00"
        }
    }
    result = validate_device_data(device, device_type="water_heater")
        self.assertFalse(result[0])
            self.assertEqual(len(result[1]), 1, "Incorrect number of errors")
            self.assertIn(f"'target_temperature' must be between {MIN_WATER_TEMP} and {MAX_WATER_TEMP}, got "
    f"{MAX_WATER_TEMP + 1} instead.", result[1], "Incorrect error message")
            self.mock_logger.error.assert_called()

    def test_invalid_water_heater_too_cold(self):
    device = {
        "status": "on",
                "parameters": {
            "temperature": (MIN_WATER_TEMP + MAX_WATER_TEMP) // 2,
            "target_temperature": MIN_WATER_TEMP - 1,
                    "is_heating": True,
                    "timer_enabled": False,
                    "scheduled_on": "08:00",
                    "scheduled_off": "10:00"
        }
    }
    result = validate_device_data(device, device_type="water_heater")
        self.assertFalse(result[0])
            self.assertEqual(len(result[1]), 1, "Incorrect number of errors")
            self.assertIn(f"'target_temperature' must be between {MIN_WATER_TEMP} and {MAX_WATER_TEMP}, got "
    f"{MIN_WATER_TEMP - 1} instead.", result[1], "Incorrect error message")
            self.mock_logger.error.assert_called()

    def test_valid_ac(self):
    device = {
        "status": "on",
                "parameters": {
            "temperature": (MIN_AC_TEMP + MAX_AC_TEMP) // 2,
            "mode": "cool",
                    "fan_speed": "medium",
                    "swing": "auto"
        }
    }
    result = validate_device_data(device, device_type="air_conditioner")
        self.assertEqual(result, (True, []))

    def test_invalid_ac_too_hot(self):
    device = {
        "status": "on",
                "parameters": {
            "temperature": MAX_AC_TEMP + 1,
                    "mode": "cool",
                    "fan_speed": "medium",
                    "swing": "auto"
        }
    }
    result = validate_device_data(device, device_type="air_conditioner")
        self.assertFalse(result[0])
            self.assertEqual(len(result[1]), 1, "Incorrect number of errors")
            self.assertIn(f"'temperature' must be between {MIN_AC_TEMP} and {MAX_AC_TEMP}, got "
    f"{MAX_AC_TEMP + 1} instead.", result[1], "Incorrect error message")
            self.mock_logger.error.assert_called()

    def test_invalid_ac_too_cold(self):
    device = {
        "status": "on",
                "parameters": {
            "temperature": MIN_AC_TEMP - 1,
                    "mode": "cool",
                    "fan_speed": "medium",
                    "swing": "auto"
        }
    }
    result = validate_device_data(device, device_type="air_conditioner")
        self.assertFalse(result[0])
            self.assertEqual(len(result[1]), 1, "Incorrect number of errors")
            self.assertIn(f"'temperature' must be between {MIN_AC_TEMP} and {MAX_AC_TEMP}, got "
    f"{MIN_AC_TEMP - 1} instead.", result[1], "Incorrect error message")
            self.mock_logger.error.assert_called()

    def test_update_id_and_type(self):
    device = {
        "id": "test",
                "type": "light",
    }
    result = validate_device_data(device, device_type="light")
        self.assertFalse(result[0])
            self.assertEqual(len(result[1]), 2, "Incorrect number of errors")
            self.assertIn("Cannot update read-only parameter 'id'", result[1], "Incorrect error message")
            self.assertIn("Cannot update read-only parameter 'type'", result[1], "Incorrect error message")

}
