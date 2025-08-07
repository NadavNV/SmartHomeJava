package nv.nadav.smart_home.service.impl;

import com.google.common.util.concurrent.AtomicDouble;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class GaugeManagerImplTest {

    private MeterRegistry registry;
    private GaugeManagerImpl gaugeManager;
    private MockedStatic<Gauge> mockedGauge;

    @BeforeEach
    void setUp() {
        registry = mock(MeterRegistry.class);
        gaugeManager = new GaugeManagerImpl(registry);
        mockedGauge = mockStatic(Gauge.class);
    }

    @AfterEach
    void tearDown() {
        mockedGauge.close();
    }

    @Test
    void testSetNumericGauge_FirstTimeRegistersGauge() {
        @SuppressWarnings("unchecked")
        Gauge.Builder<AtomicDouble> builder = mock(Gauge.Builder.class);
        AtomicDouble[] registeredRef = new AtomicDouble[1];

        // Setup mocked fluent API
        mockedGauge.when(() -> Gauge.builder(eq("temperature"), any(AtomicDouble.class), any()))
                .thenAnswer(invocation -> {
                    registeredRef[0] = invocation.getArgument(1);
                    return builder;
                });

        when(builder.description("Room temperature")).thenReturn(builder);
        when(builder.tag(anyString(), anyString())).thenReturn(builder);
        when(builder.strongReference(true)).thenReturn(builder);
        when(builder.register(registry)).thenReturn(mock(Gauge.class));

        // Call method
        gaugeManager.setNumericGauge("temperature", "Room temperature", 23.5, Map.of("room", "kitchen"));

        // Verify value set
        assertNotNull(registeredRef[0]);
        assertEquals(23.5, registeredRef[0].get());

        // Set new value
        gaugeManager.setNumericGauge("temperature", "Room temperature", 26.5, Map.of("room", "kitchen"));
        assertEquals(26.5, registeredRef[0].get());
    }

    @Test
    void testSetBooleanGaugeTrue() {
        AtomicDouble[] ref = new AtomicDouble[1];
        @SuppressWarnings("unchecked")
        Gauge.Builder<AtomicDouble> builder = mock(Gauge.Builder.class);
        when(builder.description(any())).thenReturn(builder);
        when(builder.tag(anyString(), anyString())).thenReturn(builder);
        when(builder.strongReference(anyBoolean())).thenReturn(builder);
        when(builder.register(any())).thenReturn(mock(Gauge.class));

        mockedGauge.when(() -> Gauge.builder(eq("bool_gauge"), any(AtomicDouble.class), any()))
                .thenAnswer(inv -> {
                    ref[0] = inv.getArgument(1);
                    return builder;
                });

        gaugeManager.setBooleanGauge("bool_gauge", "desc", true, Map.of("tag", "v"));

        assertNotNull(ref[0]);
        assertEquals(1.0, ref[0].get());
    }

    @Test
    void testSetBooleanGaugeFalse() {
        AtomicDouble[] ref = new AtomicDouble[1];
        @SuppressWarnings("unchecked")
        Gauge.Builder<AtomicDouble> builder = mock(Gauge.Builder.class);
        when(builder.description(any())).thenReturn(builder);
        when(builder.tag(anyString(), anyString())).thenReturn(builder);
        when(builder.strongReference(anyBoolean())).thenReturn(builder);
        when(builder.register(any())).thenReturn(mock(Gauge.class));

        mockedGauge.when(() -> Gauge.builder(eq("bool_gauge"), any(AtomicDouble.class), any()))
                .thenAnswer(inv -> {
                    ref[0] = inv.getArgument(1);
                    return builder;
                });

        gaugeManager.setBooleanGauge("bool_gauge", "desc", false, Map.of("tag", "v"));

        assertNotNull(ref[0]);
        assertEquals(0.0, ref[0].get());
    }

    @Test
    void testSetEnumGauge() {
        enum Mode { COOL, HEAT, FAN }

        AtomicDouble[] values = new AtomicDouble[3];
        int[] index = {0};
        @SuppressWarnings("unchecked")
        Gauge.Builder<AtomicDouble> builder = mock(Gauge.Builder.class);
        when(builder.description(any())).thenReturn(builder);
        when(builder.tag(anyString(), anyString())).thenReturn(builder);
        when(builder.strongReference(true)).thenReturn(builder);
        when(builder.register(any())).thenReturn(mock(Gauge.class));

        mockedGauge.when(() -> Gauge.builder(eq("mode_gauge"), any(AtomicDouble.class), any()))
                .thenAnswer(inv -> {
                    values[index[0]++] = inv.getArgument(1);
                    return builder;
                });

        gaugeManager.setEnumGauge("mode_gauge", "desc", Mode.HEAT, Map.of("device_id", "123"));

        assertEquals(0.0, values[0].get()); // COOL
        assertEquals(1.0, values[1].get()); // HEAT
        assertEquals(0.0, values[2].get()); // FAN
    }

    @Test
    void testSetScheduleGauge() {
        AtomicDouble[] initial = new AtomicDouble[1];
        AtomicDouble[] updated = new AtomicDouble[1];
        @SuppressWarnings("unchecked")
        Gauge.Builder<AtomicDouble> builder = mock(Gauge.Builder.class);
        when(builder.description(any())).thenReturn(builder);
        when(builder.tag(anyString(), anyString())).thenReturn(builder);
        when(builder.strongReference(true)).thenReturn(builder);
        when(builder.register(any())).thenReturn(mock(Gauge.class));

        AtomicInteger call = new AtomicInteger(0);

        mockedGauge.when(() -> Gauge.builder(eq("schedule_gauge"), any(AtomicDouble.class), any()))
                .thenAnswer(inv -> {
                    if (call.getAndIncrement() == 0)
                        initial[0] = inv.getArgument(1);
                    else
                        updated[0] = inv.getArgument(1);
                    return builder;
                });

        // First call creates a gauge with specific schedule
        gaugeManager.setScheduleGauge("schedule_gauge", "desc", "dev123", "08:00", "10:00");
        assertEquals(1.0, initial[0].get());

        // Second call should reset previous to 0 and add new one
        gaugeManager.setScheduleGauge("schedule_gauge", "desc", "dev123", "12:00", "14:00");
        assertEquals(0.0, initial[0].get()); // old one reset
        assertEquals(1.0, updated[0].get()); // new one set
    }
}

