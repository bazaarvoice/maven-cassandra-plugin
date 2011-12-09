package com.bazaarvoice.commons.monitoring;

import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.module.SimpleModule;

public class MonitoringObjectMapperFactory {
    public static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule serializerModule = new SimpleModule("MonitoringDataSerializer", new Version(1, 0, 0, "SNAPSHOT"));
        serializerModule.addSerializer(new CompositeDataSerializer());
        mapper.registerModule(serializerModule);
        mapper.enable(SerializationConfig.Feature.INDENT_OUTPUT);
        return mapper;
    }
}
