package com.bazaarvoice.core.util;

import java.security.SecureRandom;

/**
 * Extends Apache's NumberUtils class with other useful methods
 */
public abstract class NumberUtils extends org.apache.commons.lang.math.NumberUtils {

    public static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static int defaultInt(Integer integerValue) {
        return defaultInt(integerValue, 0);
    }

    public static int defaultInt(Integer integerValue, int defaultValue) {
        return integerValue == null ? defaultValue : integerValue;
    }

    public static long defaultLong(Long longValue) {
        return defaultLong(longValue, 0L);
    }

    public static long defaultLong(Long longValue, long defaultValue) {
        return longValue == null ? defaultValue : longValue;
    }

    public static float defaultFloat(Number floatValue) {
        return defaultFloat(floatValue, 0f);
    }

    public static float defaultFloat(Number floatValue, float defaultValue) {
        return floatValue == null ? defaultValue : floatValue.floatValue();
    }

    public static Number nullIfZero(Integer value) {
        return (value != null && value.intValue() == 0) ? null : value;
    }

    /**
     * Returns a random integer in the range [minValue, maxValue].
     */
    public static int randomInRange(int minValue, int maxValue) {
        return minValue + SECURE_RANDOM.nextInt(maxValue - minValue + 1);
    }
}
