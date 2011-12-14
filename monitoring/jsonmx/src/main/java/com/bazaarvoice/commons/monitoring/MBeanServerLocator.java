package com.bazaarvoice.commons.monitoring;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;

public class MBeanServerLocator {

    public static MBeanServer findMBeanServer() {
        ArrayList<MBeanServer> servers = MBeanServerFactory.findMBeanServer(null);
        MBeanServer defaultServer;
        if(servers.size() != 0) {
            defaultServer = servers.get(0);
        }
        else {
            defaultServer = ManagementFactory.getPlatformMBeanServer();
        }
        return defaultServer;
    }
}
