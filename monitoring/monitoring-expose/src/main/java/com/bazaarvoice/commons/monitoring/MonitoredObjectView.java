package com.bazaarvoice.commons.monitoring;

import com.sun.jersey.spi.resource.Singleton;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.util.Map;
import java.util.Set;

@Singleton
@Path ("/")
public class MonitoredObjectView {

    private MonitoredObjectProvider _provider;

    public void setProvider(MonitoredObjectProvider provider) {
        this._provider = provider;
    }

    @GET
    @Path("objects")
    @Produces("application/json")
    public Map<String, Map<String, Object>> getObjects() {
        return _provider.getObjects();
    }

    @GET
    @Path("info")
    @Produces("application/json")
    public Set<String> getObjectNames() {
        return _provider.getObjectNames();
    }

    @GET
    @Path("info/{objectName}")
    @Produces("application/json")
    public Set<String> getAttributeNames (@PathParam("objectName") String objectName) {
        return _provider.getAttributeNames(objectName);
    }

    @GET
    @Path("objects/{objectName}")
    @Produces("application/json")
    public Map<String, Object> getAttributes(@PathParam("objectName") String objectName) {
        return _provider.getAttributes(objectName);
    }
}
