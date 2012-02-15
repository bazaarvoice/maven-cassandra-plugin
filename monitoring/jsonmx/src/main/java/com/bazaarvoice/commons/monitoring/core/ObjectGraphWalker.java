package com.bazaarvoice.commons.monitoring.core;

import org.codehaus.jackson.JsonNode;

/**
 * Wrapper class for {@link JsonNode}. Allows a view to traverse the Json serialization of an {@code Object}
 * without being tightly coupled to Jackson.
 */
public class ObjectGraphWalker {

    private final JsonNode _node;

    protected ObjectGraphWalker(JsonNode node) {
        _node = node;
    }

    public ObjectGraphWalker getChild(String name) {
        if (_node.isArray()) {
            try {
                return new ObjectGraphWalker(_node.path(Integer.parseInt(name)));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        JsonNode newNode = _node.path(name);
        if(newNode.isMissingNode()) {
            return null;
        }
        return new ObjectGraphWalker(newNode);
    }

    public Object getValue() {
        return _node;
    }
}
