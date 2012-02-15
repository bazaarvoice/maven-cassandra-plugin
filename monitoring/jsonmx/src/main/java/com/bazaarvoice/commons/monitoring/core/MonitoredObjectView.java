package com.bazaarvoice.commons.monitoring.core;

import com.google.common.collect.ImmutableMap;
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

    private final MonitoredObjectProvider _provider;
    private final ObjectGraphWalkerFactory _walkerFactory;

    public MonitoredObjectView(MonitoredObjectProvider provider, ObjectGraphWalkerFactory walkerFactory) {
        this._provider = provider;
        _walkerFactory = walkerFactory;
    }
    
    @GET
    @Produces("application/json")
    public Map<String, String> getRoot() {
        return ImmutableMap.of("data", "objects/", "metadata", "info/");
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

    @Path("objects/{objectName}/{attributeName}")
    public TraversableObjectView getAttribute(@PathParam("objectName") String objectName, @PathParam("attributeName") String attributeName) {
        return new TraversableObjectView(_walkerFactory.create(getAttributes(objectName).get(attributeName)));
    }

    @GET
    @Path("objects/{objectName}")
    @Produces("application/json")
    public Map<String, Object> getAttributes(@PathParam("objectName") String objectName) {
        return _provider.getAttributes(objectName);
    }

    public class TraversableObjectView {
        private final ObjectGraphWalker _walker;

        private TraversableObjectView(ObjectGraphWalker walker) {
            _walker = walker;
        }

        @GET
        @Produces("application/json")
        public Object getValue() {
            return _walker.getValue();
        }

        @Path("{child}")
        public TraversableObjectView getChild(@PathParam("child") String name) {
            if(_walker.getChild(name) == null) {
                return null;
            }
            return new TraversableObjectView(_walker.getChild(name));
        }
    }
}
