package nv.nadav.smart_home.validation;

import nv.nadav.smart_home.dto.DeviceDto;
import nv.nadav.smart_home.dto.DeviceUpdateDto;
import nv.nadav.smart_home.model.DeviceType;
import nv.nadav.smart_home.model.parameters.AirConditionerParameters;
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
    void testVerifyTypeAndRange_ValidInt() {
        assertTrue(verifyTypeAndRange(50, "test", int.class, List.of(49, 60)).isValid());
    }

    void testVerifyTypeAndRangeIntValidFromString() {
        assertTrue(verifyTypeAndRange("50", "test", int.class, List.of(49, 60)).isValid());
    }

    @Test
    void testVerifyTypeAndRange_InvalidIntOutOfRange() {
        ValidationResult result = verifyTypeAndRange(70, "test", int.class, List.of(49, 60));
        assertFalse(result.isValid());
        String error = String.format("'%s' must be between %d and %d, got %d instead.", "test", 49, 60, 70);
        assertEquals(error, result.errorMessages().getFirst());

        result = verifyTypeAndRange(70, "test", int.class, new int[]{49, 60});
        assertFalse(result.isValid());
        assertEquals(error, result.errorMessages().getFirst());
        verify(mockLogger, times(2)).error(error);
    }

    @Test
    void testVerifyTypeAndRange_InvalidIntString() {
        assertFalse(verifyTypeAndRange("Steve", "test", int.class, List.of(49, 60)).isValid());
        String error = String.format("'%s' must be a numeric string, got '%s' instead.", "test", "Steve");
        verify(mockLogger).error(error);
    }
    
    @Test
    void testVerifyTypeAndRange_ValidStringFromSet() {
        assertTrue(verifyTypeAndRange("on", "status", String.class, Set.of("on", "off")).isValid());
    }

    @Test
    void testVerifyTypeAndRange_InvalidStringFromSet() {
        ValidationResult result = verifyTypeAndRange("maybe", "status", String.class, Set.of("on", "off"));
        assertFalse(result.isValid());
        String error = String.format(
                "'%s' is not a valid value for %s. Must be one of %s.", "maybe", "status", Set.of("on", "off")
        );;
        assertEquals(error, result.errorMessages().getFirst());
        verify(mockLogger).error(error);
    }

    @Test
    void testVerifyTypeAndRange_Time() {
        assertTrue(verifyTypeAndRange("14:30", "test", String.class, "time").isValid());
        assertFalse(verifyTypeAndRange("25:30", "test", String.class, "time").isValid());
        assertFalse(verifyTypeAndRange("14:69", "test", String.class, "time").isValid());
        assertFalse(verifyTypeAndRange("1:69", "test", String.class, "time").isValid());
        assertFalse(verifyTypeAndRange("14:3", "test", String.class, "time").isValid());
    }

    @Test
    void testVerifyTypeAndRange_Color() {
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
    void testVerifyTypeAndRange_WrongType() {
        assertFalse(verifyTypeAndRange(123, "test", String.class, Set.of("on, off")).isValid());
        String error = "test" + " must be a " + String.class.getSimpleName() + ", got " +
                Integer.valueOf(123).getClass().getSimpleName() + " instead.";
        verify(mockLogger).error(error);
    }

    @Test
    void testValidateNewDeviceData_ValidLight() {
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
    void testValidateNewDeviceData_InvalidStatus() {
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
    void testValidateNewDeviceData_InvalidParameters() {
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
    void testValidateDeviceData_ValidLock() {
        DeviceUpdateDto device = new DeviceUpdateDto();
        DoorLockParameters parameters = new DoorLockParameters();
        device.setStatus("locked");
        parameters.setBatteryLevel((MIN_BATTERY + MAX_BATTERY) / 2);
        device.setParameters(parameters);
        assertTrue(validateDeviceData(device, DeviceType.DOOR_LOCK).isValid());
    }

    @Test
    void testValidateDeviceData_WrongType() {
        DeviceUpdateDto device = new DeviceUpdateDto();
        DoorLockParameters parameters = new DoorLockParameters();
        device.setStatus("locked");
        parameters.setBatteryLevel((MIN_BATTERY + MAX_BATTERY) / 2);
        device.setParameters(parameters);
        assertFalse(validateDeviceData(device, DeviceType.LIGHT).isValid());
    }

    @Test
    void testValidateDeviceData_InvalidStatus() {
        DeviceUpdateDto device = new DeviceUpdateDto();
        DoorLockParameters parameters = new DoorLockParameters();
        device.setStatus("open");
        parameters.setBatteryLevel((MIN_BATTERY + MAX_BATTERY) / 2);
        device.setParameters(parameters);
        assertFalse(validateDeviceData(device, DeviceType.DOOR_LOCK).isValid());
    }

    @Test
    void testValidateDeviceData_InvalidReadOnly() {
        DeviceUpdateDto device = new DeviceUpdateDto();
        LightParameters parameters = new LightParameters();
        parameters.setDimmable(true);
        device.setParameters(parameters);
        assertFalse(validateDeviceData(device, DeviceType.LIGHT).isValid());
    }

    @Test
    void testValidateDeviceData_ValidWaterHeater() {
        DeviceUpdateDto device = new DeviceUpdateDto();
        WaterHeaterParameters parameters = new WaterHeaterParameters();
        parameters.setTargetTemperature((MIN_WATER_TEMP + MAX_WATER_TEMP) / 2);
        parameters.setScheduledOff("10:00");
        device.setStatus("on");
        device.setParameters(parameters);
        assertTrue(validateDeviceData(device, DeviceType.WATER_HEATER).isValid());
    }

    @Test
    void testValidateDeviceData_InvalidWaterHeaterTooHot() {
        DeviceUpdateDto device = new DeviceUpdateDto();
        WaterHeaterParameters parameters = new WaterHeaterParameters();
        parameters.setTargetTemperature(MAX_WATER_TEMP + 1);
        parameters.setScheduledOff("10:00");
        device.setStatus("on");
        device.setParameters(parameters);
        assertFalse(validateDeviceData(device, DeviceType.WATER_HEATER).isValid());
    }

    @Test
    void testValidateDeviceData_InvalidWaterHeaterTooCold() {
        DeviceUpdateDto device = new DeviceUpdateDto();
        WaterHeaterParameters parameters = new WaterHeaterParameters();
        parameters.setTargetTemperature(MIN_WATER_TEMP - 1);
        parameters.setScheduledOff("10:00");
        device.setStatus("on");
        device.setParameters(parameters);
        assertFalse(validateDeviceData(device, DeviceType.WATER_HEATER).isValid());
    }

    @Test
    void testValidateDeviceData_ValidAc() {
        DeviceUpdateDto device = new DeviceUpdateDto();
        AirConditionerParameters parameters = new AirConditionerParameters();
        parameters.setTemperature((MIN_AC_TEMP + MAX_AC_TEMP) / 2);
        parameters.setMode(AirConditionerParameters.Mode.COOL);
        parameters.setFanSpeed(AirConditionerParameters.FanSpeed.MEDIUM);
        device.setStatus("on");
        device.setParameters(parameters);
        assertTrue(validateDeviceData(device, DeviceType.AIR_CONDITIONER).isValid());
    }

    @Test
    void testValidateDeviceData_InvalidAcTooHot() {
        DeviceUpdateDto device = new DeviceUpdateDto();
        AirConditionerParameters parameters = new AirConditionerParameters();
        parameters.setTemperature(MAX_AC_TEMP + 1);
        parameters.setMode(AirConditionerParameters.Mode.COOL);
        parameters.setFanSpeed(AirConditionerParameters.FanSpeed.MEDIUM);
        device.setStatus("on");
        device.setParameters(parameters);
        assertFalse(validateDeviceData(device, DeviceType.AIR_CONDITIONER).isValid());
    }

    @Test
    void testValidateDeviceData_InvalidAcTooCold() {
        DeviceUpdateDto device = new DeviceUpdateDto();
        AirConditionerParameters parameters = new AirConditionerParameters();
        parameters.setTemperature(MIN_AC_TEMP - 1);
        parameters.setMode(AirConditionerParameters.Mode.COOL);
        parameters.setFanSpeed(AirConditionerParameters.FanSpeed.MEDIUM);
        device.setStatus("on");
        device.setParameters(parameters);
        assertFalse(validateDeviceData(device, DeviceType.AIR_CONDITIONER).isValid());
    }
}
