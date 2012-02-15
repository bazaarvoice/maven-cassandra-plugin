package com.bazaarvoice.commons.monitoring.core;

import org.codehaus.jackson.map.ObjectMapper;

/**
 * Factory class used to create root instances of {@link ObjectGraphWalker}.
 */
public class ObjectGraphWalkerFactory {
    private final ObjectMapper _mapper;

    /**
     * @param mapper the {@code ObjectMapper} that is used for Json serialization.
     */
    public ObjectGraphWalkerFactory(ObjectMapper mapper) {
        _mapper = mapper;
    }

    /**
     * Creates a root {@code ObjectGraphWalker} for a given Java {@code Object}.
     * @param object an {@code Object} that needs its Json view to be traversable.
     * @return an {@code ObjectGraphWalker} that allows traversal of the {@code object}.
     */
    public ObjectGraphWalker create(Object object) {
        return new ObjectGraphWalker(_mapper.valueToTree(object));
    }
}
