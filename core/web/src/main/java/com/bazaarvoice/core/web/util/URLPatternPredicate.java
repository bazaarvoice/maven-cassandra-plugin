package com.bazaarvoice.core.web.util;

/**
 * Handlers for URL Pattern Predicates have to implement this interface.
 */
public interface URLPatternPredicate {
    public boolean matches(String string);
}
