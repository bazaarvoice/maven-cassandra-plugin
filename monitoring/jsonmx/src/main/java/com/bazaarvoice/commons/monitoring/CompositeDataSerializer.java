package com.bazaarvoice.commons.monitoring;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

import javax.management.openmbean.CompositeData;
import java.io.IOException;
import java.util.Set;

public class CompositeDataSerializer extends SerializerBase<CompositeData> {

    protected CompositeDataSerializer(Class<CompositeData> t) {
        super(t);
    }

    public CompositeDataSerializer() {
        this(CompositeData.class);
    }

    @Override
    public void serialize(CompositeData value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException {
        Set<String> keySet = value.getCompositeType().keySet();
        String[] keys = keySet.toArray(new String[keySet.size()]);
        Object[] values = value.getAll(keys);
        jgen.writeStartObject();
        for (int i = 0; i < keys.length; ++i) {
            jgen.writeObjectField(keys[i], values[i]);
        }
        jgen.writeEndObject();
    }
}
