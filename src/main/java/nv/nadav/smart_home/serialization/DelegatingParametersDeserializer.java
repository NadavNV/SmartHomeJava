package nv.nadav.smart_home.serialization;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import nv.nadav.smart_home.model.parameters.DeviceParameters;

import java.io.IOException;

public class DelegatingParametersDeserializer extends JsonDeserializer<DeviceParameters> {

    public static final ThreadLocal<JsonDeserializer<DeviceParameters>> delegate = new ThreadLocal<>();

    @Override
    public DeviceParameters deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        JsonDeserializer<DeviceParameters> injected = delegate.get();
        if (injected == null) {
            throw new IllegalStateException("No delegate deserializer set");
        }

        return injected.deserialize(jsonParser, deserializationContext);
    }
}
