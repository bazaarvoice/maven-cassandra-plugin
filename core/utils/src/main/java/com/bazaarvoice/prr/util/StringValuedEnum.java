package com.bazaarvoice.prr.util;

/**
 * Utility class designed to allow dinamic fidding and manipulation of Enum
 * instances which hold a string value.<br>
 * Taken from http://www.hibernate.org/273.html
 */
public interface StringValuedEnum {

    /**
     * Current string value stored in the enum.
     * @return string value.
     */
    public String getValue();
    
}