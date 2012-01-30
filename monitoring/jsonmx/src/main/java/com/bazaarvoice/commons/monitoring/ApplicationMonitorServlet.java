package com.bazaarvoice.commons.monitoring;

import com.bazaarvoice.commons.monitoring.core.MBeanServerUtils;
import com.bazaarvoice.commons.monitoring.core.MonitoredObjectProvider;
import com.bazaarvoice.commons.monitoring.core.MonitoringApplication;
import com.bazaarvoice.commons.monitoring.core.MonitoringApplicationFactory;
import com.bazaarvoice.commons.monitoring.transform.ObjectNameTransformFactory;
import com.google.common.base.Strings;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.util.HashMap;
import java.util.Map;

/**
 * Entry point for starting an application monitoring servlet. Accepts one initial parameter, "transforms". It's value should be a comma separated list
 * of other initial parameter names. Each value for those parameters should be a semi-colon separated list specifying an
 * {@link com.bazaarvoice.commons.monitoring.transform.ObjectNameTransform ObjectNameTransform}. It may be best seen with an example:
 * <p>
<blockquote><pre>
{@code <servlet>
    <servlet-name>Monitor</servlet-name>
    <servlet-class>com.bazaarvoice.commons.monitoring.ApplicationMonitorServlet</servlet-class>
    <init-param>
        <param-name>transforms</param-name>
        <param-value>JVM,filterStuff,groupStuff</param-value>
    </init-param>
    <init-param>
        <param-name>JVM</param-name>
        <param-value>type groupJVM</param-value>
    </init-param>
    <init-param>
        <param-name>filterStuff</param-name>
        <param-value>type filter;domain SomeDomain</param-value>
    </init-param>
    <init-param>
        <param-name>groupStuff</param-name>
        <param-value>type group;pattern OtherDomain:keyProp=*,*;baseName Other.;distinguishingProperty keyProp;attributeNamingProperty name</param-value>
    </init-param>
</servlet>}
</pre></blockquote>
 * </p>
 * This will create the monitoring servlet with three transforms.
 * <ol>
 *     <li>Grouping JVM MXBeans into aggregate objects that contain MXBeans of a single type.</li>
 *     <li>Filtering out MBeans with {@link javax.management.ObjectName ObjectName}s in the "{@code SomeDomain}" domain.</li>
 *     <li>Grouping MBeans with {@code ObjectName}s in the domain "{@code OtherDomain}" and with key property "{@code keyProp}.
 *     The name of the created object will be "{@code Other.[keyProp]}" where {@code [keyProp]} is the value of
 *     {@link javax.management.ObjectName#getKeyProperty(String) getKeyProperty}{@code ("keyProp")} in the grouped {@code ObjectName}s.
 *     The created object will have attribute names equal to the values of its members' "{@code name}" key property.</li>
 * </ol>
 */
public final class ApplicationMonitorServlet extends ServletContainer {
    
    private static final Logger _logger = LoggerFactory.getLogger(ApplicationMonitorServlet.class);

    /**
     * {@code param-name} to be used to specify transform {@code param-name}s in the servlet's initial parameters.
     */
    public static final String TRANSFORM_INIT_PARAM = "transforms";
    
    private ObjectNameTransformFactory _transformFactory;
    private MonitoringApplication _application;
            

    public ApplicationMonitorServlet() {
        this(MonitoringApplicationFactory.createMonitoringApplication(MBeanServerUtils.findMBeanServer()));
    }
    
    private ApplicationMonitorServlet(MonitoringApplication application) {
        super(application);
        _application = application;
        _transformFactory = new ObjectNameTransformFactory(_application.getProvider().getServer());
    }

    @Override
    public void init()
            throws ServletException {
        super.init();
        String transformsList = getInitParameter(TRANSFORM_INIT_PARAM);
        if (transformsList == null) {
            return;
        }
        MonitoredObjectProvider provider = _application.getProvider();
        for (String transform : transformsList.split(",")) {
            String specString = getInitParameter(transform);
            if (Strings.isNullOrEmpty(specString)) {
                _logger.warn("Transform \"{}\" listed in init-params but not specified", transform);
            }
            Map<String, String> specMap = specStringToMap(specString);
            provider.addTransform(_transformFactory.create(specMap));
        }
    }

    private Map<String, String> specStringToMap(String specString) {
        HashMap<String, String> specMap = new HashMap<String, String>();
        for (String  spec : specString.split("\\s*;\\s*")) { // TODO: Handle quoted/escaped semicolons?
            String[] config = spec.split("\\s*", 2);
            specMap.put(config[0], config.length == 2 ? config[1] : null);
        }
        return specMap;
    }

}
