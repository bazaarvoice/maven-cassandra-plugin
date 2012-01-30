package com.bazaarvoice.commons.monitoring.core;

import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.module.SimpleModule;

/**
 * Factory class for producing {@link ObjectMapper} instances with serializers and features useful for serializing monitoring data.
 */
public class MonitoringObjectMapperFactory {

    private MonitoringObjectMapperFactory() {
    }

    public static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule serializerModule = new SimpleModule("MonitoringDataSerializer", new Version(1, 0, 0, "SNAPSHOT"));
        serializerModule.addSerializer(new CompositeDataSerializer());
        serializerModule.addSerializer(new TabularDataSerializer());
        mapper.registerModule(serializerModule);
        mapper.enable(SerializationConfig.Feature.INDENT_OUTPUT);
        mapper.disable(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS);
        return mapper;
    }
}
