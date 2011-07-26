package com.bazaarvoice.cca.util;

import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

//NOTICE:  All regexp definitions and business logic contained in this file should closely match the client side logic defined in passwordValidation.js.

public class PasswordUtils {

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("[\\s]");
    private static final Pattern UPPER_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWER_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SYMBOL_PATTERN = Pattern.compile("[^\\sA-Za-z0-9]");

    public static boolean isLongPassword(String password) {
        // a password must be at least 8 characters long
        return StringUtils.isNotBlank(password) && password.length() >= 8;
    }

    public static boolean isWhitespaceFreePassword(String password) {

        // a password cannot contain whitespace
        return !WHITESPACE_PATTERN.matcher(password).find();
    }

    public static boolean isComplexPassword(String password) {

        int count = 0;
        if (UPPER_PATTERN.matcher(password).find()) {
            count++;
        }
        if (LOWER_PATTERN.matcher(password).find()) {
            count++;
        }
        if (NUMBER_PATTERN.matcher(password).find()) {
            count++;
        }
        if (SYMBOL_PATTERN.matcher(password).find()) {
            count++;
        }

        // a password must match at least 3 of these patterns
        return count >= 3;
    }
}
