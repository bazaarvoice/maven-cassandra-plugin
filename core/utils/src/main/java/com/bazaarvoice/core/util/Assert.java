package com.bazaarvoice.core.util;

import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Map;

/**
 * Assertion utility class that allows authors to validate components with the same performance but without the use of Java language-level assertions.
 * <p/>
 * This class is similar to JUnit, TestNG, and Spring Assert classes, but with better performance characteristics when there are no errors.  All methods that take a message also take a set of
 * parameters.  The message itself can be parameterized using {@link MessageFormat} variable placeholders so that the message String passed in is constant and not built via String concatenation at
 * call time.  This does mean that any single quotes should be doubled to match the MessageFormat requirements.
 */
public class Assert {
    private Assert() {
    }

    /**
     * Checks that the given boolean value is true.
     *
     * @param value The boolean value
     */
    public static void isTrue(boolean value) {
        isTrue(value, "[Assertion failed] - this value must be true");
    }

    /**
     * Checks that the given boolean value is true.
     *
     * @param value         The boolean value
     * @param message       The message
     * @param messageParams The parameters to the message
     */
    public static void isTrue(boolean value, String message, Object... messageParams) {
        if (!value) {
            throw new IllegalArgumentException(MessageFormat.format(message, messageParams));
        }
    }

    /**
     * Checks that the given boolean value is false.
     *
     * @param value The boolean value
     */
    public static void isFalse(boolean value) {
        isFalse(value, "[Assertion failed] - this value must be false");
    }

    /**
     * Checks that the given boolean value is false.
     *
     * @param value         The boolean value
     * @param message       The message
     * @param messageParams The parameters to the message
     */
    public static void isFalse(boolean value, String message, Object... messageParams) {
        if (value) {
            throw new IllegalArgumentException(MessageFormat.format(message, messageParams));
        }
    }

    /**
     * Checks that the given object is null.
     *
     * @param object The object
     */
    public static void isNull(Object object) {
        isNull(object, "[Assertion failed] - the object argument must be null");
    }

    /**
     * Checks that the given object is null.
     *
     * @param object        The object
     * @param message       The message
     * @param messageParams The parameters to the message
     */
    public static void isNull(Object object, String message, Object... messageParams) {
        if (object != null) {
            throw new IllegalArgumentException(MessageFormat.format(message, messageParams));
        }
    }

    /**
     * Checks that the given object is not null.
     *
     * @param object The object
     */
    public static void isNotNull(Object object) {
        isNotNull(object, "[Assertion failed] - this argument is required; it must not be null");
    }

    /**
     * Checks that the given object is not null.
     *
     * @param object        The object
     * @param message       The message
     * @param messageParams The parameters to the message
     */
    public static void isNotNull(Object object, String message, Object... messageParams) {
        if (object == null) {
            throw new IllegalArgumentException(MessageFormat.format(message, messageParams));
        }
    }

    /**
     * Checks that the given text is null or empty.
     *
     * @param text The text
     */
    public static void isEmpty(String text) {
        isEmpty(text, "[Assertion failed] - this String argument must not have length; it must be null or empty");
    }

    /**
     * Checks that the given text is null or empty.
     *
     * @param text          The text
     * @param message       The message
     * @param messageParams The parameters to the message
     */
    public static void isEmpty(String text, String message, Object... messageParams) {
        if (StringUtils.isNotEmpty(text)) {
            throw new IllegalArgumentException(MessageFormat.format(message, messageParams));
        }
    }

    /**
     * Checks that the given text is not null or empty.
     *
     * @param text The text
     */
    public static void isNotEmpty(String text) {
        isNotEmpty(text, "[Assertion failed] - this String argument must have length; it must not be null or empty");
    }

    /**
     * Checks that the given text is not null or empty.
     *
     * @param text          The text
     * @param message       The message
     * @param messageParams The parameters to the message
     */
    public static void isNotEmpty(String text, String message, Object... messageParams) {
        if (StringUtils.isEmpty(text)) {
            throw new IllegalArgumentException(MessageFormat.format(message, messageParams));
        }
    }

    /**
     * Checks that the given text is blank.
     *
     * @param text The text
     */
    public static void isBlank(String text) {
        isBlank(text, "[Assertion failed] - this String argument must not have text; it must be null, empty, or blank");
    }

    /**
     * Checks that the given text is blank.
     *
     * @param text          The text
     * @param message       The message
     * @param messageParams The parameters to the message
     */
    public static void isBlank(String text, String message, Object... messageParams) {
        if (StringUtils.isNotBlank(text)) {
            throw new IllegalArgumentException(MessageFormat.format(message, messageParams));
        }
    }

    /**
     * Checks that the given text is not blank.
     *
     * @param text The text
     */
    public static void isNotBlank(String text) {
        isNotBlank(text, "[Assertion failed] - this String argument must have text; it must not be null, empty, or blank");
    }

    /**
     * Checks that the given text is not blank.
     *
     * @param text          The text
     * @param message       The message
     * @param messageParams The parameters to the message
     */
    public static void isNotBlank(String text, String message, Object... messageParams) {
        if (StringUtils.isBlank(text)) {
            throw new IllegalArgumentException(MessageFormat.format(message, messageParams));
        }
    }

    /**
     * Verifies that the given text contains the given substring.
     *
     * @param textToSearch The text to search
     * @param substring    The substring
     */
    public static void contains(String textToSearch, String substring) {
        contains(textToSearch, substring, "[Assertion failed] - this String argument must contain the substring [{0}]", substring);
    }

    /**
     * Verifies that the given text contains the given substring.
     *
     * @param textToSearch  The text to search
     * @param substring     The substring
     * @param message       The message
     * @param messageParams The parameters to the message
     */
    public static void contains(String textToSearch, String substring, String message, Object... messageParams) {
        if (StringUtils.isNotEmpty(textToSearch) && StringUtils.isNotEmpty(substring) && textToSearch.indexOf(substring) != -1) {
            throw new IllegalArgumentException(MessageFormat.format(message, messageParams));
        }
    }

    /**
     * Verifies that the given text doesn't contain the given substring.
     *
     * @param textToSearch The text to search
     * @param substring    The substring
     */
    public static void doesNotContain(String textToSearch, String substring) {
        doesNotContain(textToSearch, substring, "[Assertion failed] - this String argument must not contain the substring [{0}]", substring);
    }

    /**
     * Verifies that the given text doesn't contain the given substring.
     *
     * @param textToSearch  The text to search
     * @param substring     The substring
     * @param message       The message
     * @param messageParams The parameters to the message
     */
    public static void doesNotContain(String textToSearch, String substring, String message, Object... messageParams) {
        if (StringUtils.isNotEmpty(textToSearch) && StringUtils.isNotEmpty(substring) && textToSearch.indexOf(substring) != -1) {
            throw new IllegalArgumentException(MessageFormat.format(message, messageParams));
        }
    }

    /**
     * Checks that the given array is null or empty.
     *
     * @param array The array
     */
    public static void isEmpty(Object[] array) {
        isEmpty(array, "[Assertion failed] - this array must be empty: it must be null or contain no elements");
    }

    /**
     * Checks that the given array is null or empty.
     *
     * @param array         The array
     * @param message       The message
     * @param messageParams The parameters to the message
     */
    public static void isEmpty(Object[] array, String message, Object... messageParams) {
        if (array != null && array.length > 0) {
            throw new IllegalArgumentException(MessageFormat.format(message, messageParams));
        }
    }

    /**
     * Checks that the given array is not null or empty.
     *
     * @param array The array
     */
    public static void isNotEmpty(Object[] array) {
        isNotEmpty(array, "[Assertion failed] - this array must not be empty: it must contain at least 1 element");
    }

    /**
     * Checks that the given array is not null or empty.
     *
     * @param array         The array
     * @param message       The message
     * @param messageParams The parameters to the message
     */
    public static void isNotEmpty(Object[] array, String message, Object... messageParams) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException(MessageFormat.format(message, messageParams));
        }
    }

    /**
     * Checks that the given array has no elements that are null.
     *
     * @param array         The array
     */
    public static void hasNoNullElements(Object[] array) {
        hasNoNullElements(array, "[Assertion failed] - this array must not contain any null elements");
    }

    /**
     * Checks that the given array has no elements that are null.
     *
     * @param array         The array
     * @param message       The message
     * @param messageParams The message parameters
     */
    public static void hasNoNullElements(Object[] array, String message, Object... messageParams) {
        if (array != null) {
            for (int i = 0; i < array.length; i++) {
                if (array[i] == null) {
                    throw new IllegalArgumentException(MessageFormat.format(message, messageParams));
                }
            }
        }
    }

    /**
     * Checks that the given collection is null or empty.
     *
     * @param collection The collection
     */
    public static void isEmpty(Collection collection) {
        isEmpty(collection, "[Assertion failed] - this collection must be empty: it must be null or contain no elements");
    }

    /**
     * Checks that the given collection is null or empty.
     *
     * @param collection    The collection
     * @param message       The message
     * @param messageParams The parameters to the message
     */
    public static void isEmpty(Collection<?> collection, String message, Object... messageParams) {
        if (collection != null && !collection.isEmpty()) {
            throw new IllegalArgumentException(MessageFormat.format(message, messageParams));
        }
    }

    /**
     * Checks that the given collection is not null or empty.
     *
     * @param collection The collection
     */
    public static void isNotEmpty(Collection collection) {
        isNotEmpty(collection, "[Assertion failed] - this collection must not be empty: it must contain at least 1 element");
    }

    /**
     * Checks that the given collection is not null or empty.
     *
     * @param collection    The collection
     * @param message       The message
     * @param messageParams The parameters to the message
     */
    public static void isNotEmpty(Collection<?> collection, String message, Object... messageParams) {
        if (collection == null || collection.isEmpty()) {
            throw new IllegalArgumentException(MessageFormat.format(message, messageParams));
        }
    }

    /**
     * Checks that the given map is null or empty.
     *
     * @param map The map
     */
    public static void isEmpty(Map map) {
        isEmpty(map, "[Assertion failed] - this map must be empty; it must be null or contain no entries");
    }

    /**
     * Checks that the given map is null or empty.
     *
     * @param map           The map
     * @param message       The message
     * @param messageParams The parameters to the message
     */
    public static void isEmpty(Map<?, ?> map, String message, Object... messageParams) {
        if (map != null && !map.isEmpty()) {
            throw new IllegalArgumentException(MessageFormat.format(message, messageParams));
        }
    }

    /**
     * Checks that the given map is not null or empty.
     *
     * @param map The map
     */
    public static void isNotEmpty(Map map) {
        isNotEmpty(map, "[Assertion failed] - this map must not be empty; it must contain at least one entry");
    }

    /**
     * Checks that the given map is not null or empty.
     *
     * @param map           The map
     * @param message       The message
     * @param messageParams The parameters to the message
     */
    public static void isNotEmpty(Map<?, ?> map, String message, Object... messageParams) {
        if (map == null || map.isEmpty()) {
            throw new IllegalArgumentException(MessageFormat.format(message, messageParams));
        }
    }

    /**
     * Checks that the given object is not null and is an instance of the given class.
     *
     * @param type   The class
     * @param object The object
     */
    public static void isInstanceOf(Class<?> type, Object object) {
        isInstanceOf(type, object, "");
    }

    /**
     * Checks that the given object is not null and is an instance of the given class.
     *
     * @param type          The class
     * @param object        The object
     * @param message       The message
     * @param messageParams The parameters to the message
     */
    public static void isInstanceOf(Class<?> type, Object object, String message, Object... messageParams) {
        isNotNull(type, "Type to check instance against must not be null");
        if (object == null || !type.isInstance(object)) {
            throw new IllegalArgumentException(MessageFormat.format(message, messageParams) +
                    "Object of class [" + (object != null ? object.getClass().getName() : "null") + "] must be an instance of " + type.getName());
        }
    }

    /**
     * Checks that the given object is not null and is an instance of the given class.
     *
     * @param superType The class
     * @param subType   The object
     */
    public static void isAssignableFrom(Class<?> superType, Class<?> subType) {
        isAssignableFrom(superType, subType, "");
    }

    /**
     * Checks that the given object is not null and is an instance of the given class.
     *
     * @param superType     The class
     * @param subType       The object
     * @param message       The message
     * @param messageParams The parameters to the message
     */
    public static void isAssignableFrom(Class<?> superType, Class<?> subType, String message, Object... messageParams) {
        isNotNull(superType, "Type to check subtype against must not be null");
        if (subType == null || !superType.isAssignableFrom(subType)) {
            throw new IllegalArgumentException(MessageFormat.format(message, messageParams) + subType + " is not assignable to " + superType);
        }
    }

    /**
     * Checks that the given state is true.
     * <p/>
     * Throws {@link IllegalStateException} rather than {@link IllegalArgumentException}.
     *
     * @param state         The state to check
     */
    public static void isState(boolean state) {
        isState(state, "[Assertion failed] - this state invariant must be true");
    }

    /**
     * Checks that the given state is true.
     * <p/>
     * Throws {@link IllegalStateException} rather than {@link IllegalArgumentException}.
     *
     * @param state         The state to check
     * @param message       The message
     * @param messageParams The parameters to the message
     */
    public static void isState(boolean state, String message, Object... messageParams) {
        if (!state) {
            throw new IllegalStateException(MessageFormat.format(message, messageParams));
        }
    }

    /**
     * Checks that the given operation is supported.
     * <p/>
     * Throws {@link UnsupportedOperationException} rather than {@link IllegalArgumentException}.
     *
     * @param operation     The operation result to check
     */
    public static void isOperationSupported(boolean operation) {
        isOperationSupported(operation, "Operation is not supported");
    }

    /**
     * Checks that the given operation is supported.
     * <p/>
     * Throws {@link UnsupportedOperationException} rather than {@link IllegalArgumentException}.
     *
     * @param operation     The operation result to check
     * @param message       The message
     * @param messageParams The parameters to the message
     */
    public static void isOperationSupported(boolean operation, String message, Object... messageParams) {
        if (!operation) {
            throw new UnsupportedOperationException(MessageFormat.format(message, messageParams));
        }
    }
}