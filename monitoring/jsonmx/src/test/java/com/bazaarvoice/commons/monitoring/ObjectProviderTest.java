package com.bazaarvoice.commons.monitoring;

import com.google.common.collect.Iterables;
import org.codehaus.jackson.map.ObjectMapper;
import org.testng.annotations.Test;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Map;

import static org.testng.Assert.assertEquals;

@Test
public class ObjectProviderTest {

    public void testObjectProvider() throws Exception {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        MonitoredObjectProvider provider = new MonitoredObjectProvider(server);

        ObjectName name = new ObjectName("test:type=TestBean");
        Test bean = new Test();
        server.registerMBean(bean, name);

        Map.Entry<String, Map<String, Object>> object = provider.getObject(name.getCanonicalName());
        assertEquals(object.getKey(), "test:type=TestBean");
        assertEquals(object.getValue().get("CacheSize"), 100);
    }

    private Attribute getOneAttribute(ObjectMapper mapper, MBeanServer server, ObjectName name) throws Exception {
        AttributeList attributes = server.getAttributes(name, new String[]{ "CacheSize" });
        return Iterables.getFirst(attributes.asList(), null);
    }

    /**
     * Test MBean interface.
     */
    public static interface TestMBean {

        int getCacheSize();

    }

    /** Test MBean. */
    private static class Test implements TestMBean {

        private int _cacheSize = 100;

        @Override
        public int getCacheSize() {
            return _cacheSize;
        }
    }

}
