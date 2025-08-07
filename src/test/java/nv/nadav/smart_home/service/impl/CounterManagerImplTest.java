package nv.nadav.smart_home.service.impl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Map;

import static org.mockito.Mockito.*;

class CounterManagerImplTest {

    private MeterRegistry registry;
    private CounterManagerImpl counterManager;
    private Counter mockCounter;

    @BeforeEach
    void setUp() {
        registry = mock(MeterRegistry.class);
        mockCounter = mock(Counter.class);
        counterManager = new CounterManagerImpl(registry);
    }

    @Test
    void testIncrement() {
        try (MockedStatic<Counter> mockedCounter = mockStatic(Counter.class)) {
            Counter.Builder builder = mock(Counter.Builder.class);

            // Set up the fluent API behavior
            mockedCounter.when(() -> Counter.builder("my.counter")).thenReturn(builder);
            when(builder.description("Test counter")).thenReturn(builder);
            when(builder.tag(anyString(), anyString())).thenReturn(builder);
            when(builder.register(registry)).thenReturn(mockCounter);

            // Call method
            counterManager.increment("my.counter", "Test counter", Map.of("device", "sensor1"));

            // Verify
            verify(mockCounter).increment();
            mockedCounter.verify(() -> Counter.builder("my.counter"));
            verify(builder).description("Test counter");
            verify(builder).tag("device", "sensor1");
            verify(builder).register(registry);
        }
    }

    @Test
    void testIncrementBy() {
        try (MockedStatic<Counter> mockedCounter = mockStatic(Counter.class)) {
            Counter.Builder builder = mock(Counter.Builder.class);

            mockedCounter.when(() -> Counter.builder("my.counter")).thenReturn(builder);
            when(builder.description("Increment By")).thenReturn(builder);
            when(builder.tag(anyString(), anyString())).thenReturn(builder);
            when(builder.register(registry)).thenReturn(mockCounter);

            counterManager.incrementBy("my.counter", "Increment By", Map.of("type", "temp"), 3.5);

            verify(mockCounter).increment(3.5);
        }
    }
}
