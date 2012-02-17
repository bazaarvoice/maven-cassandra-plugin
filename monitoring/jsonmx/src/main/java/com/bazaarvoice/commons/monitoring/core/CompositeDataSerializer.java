package com.bazaarvoice.commons.monitoring.core;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

import javax.management.openmbean.CompositeData;
import java.io.IOException;
import java.util.Set;

/**
 * Json serializer for {@link CompositeData} attributes used by {@link javax.management.MXBean MXBean}s.
 */
public class CompositeDataSerializer extends SerializerBase<CompositeData> {

    public CompositeDataSerializer() {
        super(CompositeData.class);
    }

    @Override
    public void serialize(CompositeData value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException {
        Set<String> keySet = value.getCompositeType().keySet();
        String[] keys = keySet.toArray(new String[keySet.size()]);
        Object[] values = value.getAll(keys);
        jgen.writeStartObject();
        for (int i = 0; i < keys.length; ++i) {
            provider.defaultSerializeField(keys[i], values[i], jgen);
        }
        jgen.writeEndObject();
    }
}
