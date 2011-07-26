package com.bazaarvoice.cca.util;

import com.bazaarvoice.core.util.Locale;
import com.bazaarvoice.core.util.BVStringUtils;

import java.text.Collator;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public abstract class LocaleResourceUtils {
    private static final Locale DEFAULT_LOCALE = Locale.ROOT;

    public static String getMessage(Class<?> clazz, Locale locale, String propertyKey) {
        ResourceBundle messages = ResourceBundle.getBundle(clazz.getName(), getNullSafeLocale(locale).unwrap());
        return messages.getString(BVStringUtils.stripCommentedTokens(propertyKey));
    }

    /**
     * Gets message by propertyKey.  If that is not defined, attempts the fallbackPropertyKey.
     *
     * @throws MissingResourceException if neither property keys correspond to an available property.
     */
    public static String getMessage(Class<?> clazz, Locale locale, String propertyKey, String fallbackPropertyKey) {
        try {
            return getMessage(clazz, locale, propertyKey);
        } catch (MissingResourceException mre) {
            return getMessage(clazz, locale, fallbackPropertyKey);
        }
    }

    public static String format(Class<?> clazz, Locale locale, String propertyKey, Object... value) {
        ResourceBundle messages = ResourceBundle.getBundle(clazz.getName(), getNullSafeLocale(locale).unwrap());
        String pattern = BVStringUtils.stripCommentedTokens(messages.getString(propertyKey));
        MessageFormat formatter = new MessageFormat(pattern, Locale.unwrap(locale));
        return formatter.format(value);
    }

    public static void sort(Locale locale, List<String> strings) {
        final Collator collator = getCaseSensitiveComparator(locale);
        Collections.sort(strings, new Comparator<String>(){
            @Override
            public int compare(String o1, String o2) {
                return collator.compare(o1, o2);
            }
        });
    }

    public static Collator getCaseInsensitiveComparator(Locale locale) {
        Collator collator = Collator.getInstance(getNullSafeLocale(locale).unwrap());
        collator.setStrength(Collator.SECONDARY); // use case-insensitive comparison
        return collator;
    }

    public static Collator getCaseSensitiveComparator(Locale locale) {
        return Collator.getInstance(getNullSafeLocale(locale).unwrap());
    }

    public static String dateFormat(Locale locale, Date date) {
        return DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.unwrap(locale)).format(date);
    }

    public static String dateFormat(Locale locale, String pattern, Date date) {
        return new SimpleDateFormat(pattern, Locale.unwrap(locale)).format(date);
    }

    public static void clearResourcesCache() {
        ResourceBundle.clearCache();
    }

    public static Locale getDefaultLocale() {
        return DEFAULT_LOCALE;
    }

    private static Locale getNullSafeLocale(Locale locale) {
        return locale == null ?  getDefaultLocale() : locale;
    }
}
