package com.bazaarvoice.commons.monitoring;

import com.sun.jersey.spi.container.servlet.ServletContainer;

public class ApplicationMonitorServlet extends ServletContainer {

    public ApplicationMonitorServlet() {
        super(MonitoringApplicationFactory.createMonitoringResourceConfig());
    }
}
