package com.bazaarvoice.commons.monitoring.core;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Json serializer for {@link javax.management.openmbean.TabularData} attributes used by {@link javax.management.MXBean}s.
 */
public class TabularDataSerializer extends SerializerBase<TabularData> {
    
    public TabularDataSerializer() {
        super(TabularData.class);
    }
    
    @Override
    public void serialize(TabularData value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException {
        List<String> indexNames = value.getTabularType().getIndexNames();
        Set<String> keyNames = new HashSet<String> (value.getTabularType().getRowType().keySet());
        keyNames.removeAll(indexNames);
        String[] indices = indexNames.toArray(new String[indexNames.size()]);
        String[] values = keyNames.toArray(new String[keyNames.size()]);
        Collection rows = value.values();
        IdentityHashMap<Object, Object> map = new IdentityHashMap<Object, Object>();
        for (Object row : rows) {
            CompositeData data = (CompositeData) row;
            map.put(unwrapIfSingular(data.getAll(indices)), unwrapIfSingular(data.getAll(values)));
        }
        jgen.writeObject(map);
    }
    
    private Object unwrapIfSingular(Object[] array) {
        return array.length == 1 ? array[0] : array;
    }
}
