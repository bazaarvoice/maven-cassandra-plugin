package com.bazaarvoice.core.jms.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.Collection;
import java.util.Stack;

/**
 * Starts and stops JMS message delivery when the web application is
 * initialized and destroyed.
 */
public class StartStopMessageDelivery implements ServletContextListener {
    private static final Log _sLog = LogFactory.getLog(StartStopMessageDelivery.class);

    private final Stack<PausableServiceController> _startedControllers = new Stack<PausableServiceController>();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        _sLog.info("Starting JMS message delivery for " + sce.getServletContext().getServletContextName() + "...");

        for (PausableServiceController serviceController : getServiceControllers(sce.getServletContext())) {
            serviceController.start();
            _startedControllers.push(serviceController);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        while (!_startedControllers.isEmpty()) {
            _startedControllers.pop().stop();
        }
    }

    private Collection<PausableServiceController> getServiceControllers(ServletContext context) {
        ApplicationContext cxt = WebApplicationContextUtils.getWebApplicationContext(context);
        //noinspection unchecked
        Collection<PausableServiceController> serviceControllers = (Collection<PausableServiceController>)
                cxt.getBeansOfType(PausableServiceController.class, false, true).values();

        if (serviceControllers.isEmpty()) {
            throw new IllegalStateException("Unable to find PausableServiceControllers in Spring ApplicationContext.");
        }
        return serviceControllers;
    }
}
