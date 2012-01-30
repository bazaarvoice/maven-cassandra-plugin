package com.bazaarvoice.commons.monitoring;

import com.bazaarvoice.commons.monitoring.core.MonitoredObjectProvider;
import com.google.common.collect.Iterables;
import org.codehaus.jackson.map.ObjectMapper;
import org.testng.annotations.Test;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Test
public class ObjectProviderTest {

    public void testObjectProvider() throws Exception {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        MonitoredObjectProvider provider = new MonitoredObjectProvider(server);
        String name = "test:type=TestBean";
        ObjectName objectName = new ObjectName(name);
        Test bean = new Test();
        server.registerMBean(bean, objectName);
        Set<String> objectNames = provider.getObjectNames();
        assertTrue(objectNames.contains(name));
        Set<String> attributeNames = provider.getAttributeNames(name);
        assertTrue(attributeNames.contains("CacheSize"));
        Map<String, Object> object = provider.getAttributes(name);
        assertEquals(object.get("CacheSize"), 100);
        Map<String, Map<String, Object>> objects = provider.getObjects();
        assertEquals(objects.get(name).get("CacheSize"), 100);
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
