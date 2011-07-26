package com.bazaarvoice.core.util;

import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class LocaleUtils extends org.apache.commons.lang.LocaleUtils {

    private static final Log _log = LogFactory.getLog(LocaleUtils.class);

    public static Locale getParentLocale(Locale locale) {
        if (locale.getVariant().length() > 0) {
            // drop variant
            return new Locale(locale.getLanguage(), locale.getCountry(), "");

        } else if (locale.getCountry().length() > 0) {
            // drop country, variant
            return new Locale(locale.getLanguage(), "", "");

        } else {
            // drop language, country, variant leaving nothing left
            return null;
        }
    }

    /**
     * This method is just like the {@link org.apache.commons.lang.LocaleUtils#localeLookupList(java.util.Locale)},
     * except the following:
     * <ul>
     * <li>it will have NULL as the last element</li>
     * <li>it returns list of {@link com.bazaarvoice.core.util.Locale}  instead of {@link java.util.Locale}
     * </ul>
     */
    public static List<Locale> localeLookupListEndingWithNull(Locale locale) {
        List<Locale> hierarchy = Lists.newArrayList(localeLookupList(locale));
        hierarchy.add(null);
        return hierarchy;
    }

    /**
     * This method is just like the {@link org.apache.commons.lang.LocaleUtils#localeLookupList(java.util.Locale)}, 
     * except it returns list of {@link com.bazaarvoice.core.util.Locale}  instead of {@link java.util.Locale}
     */
    public static List<Locale> localeLookupList(Locale locale) {
        List<Locale> hierarchy = Lists.newArrayList();
        for (; locale != null; locale = getParentLocale(locale)) {
            hierarchy.add(locale);
        }
        return hierarchy;
    }

    public static Locale getLocaleFromMicrosoftLCID(int lcid) {
        return LocaleLcidTable.getLocaleFromMicrosoftLCID(lcid);
    }

    public static Locale safeToLocale(String localeStr) {
        Locale locale;
        try {
            locale = Locale.toLocale(localeStr);
        } catch (IllegalArgumentException e) {
            _log.info("Invalid locale format: " + localeStr + ". Null value is used.");
            locale = null;
        }
        return locale;
    }

    /**
     * Convert collection of locales into String list
     */
    public static List<String> convertLocaleList(Collection<Locale> locales) {
        if (CollectionUtils.isEmpty(locales)) {
            return Collections.emptyList();
        }
        List<String> result = Lists.newArrayList();
        for (Locale locale : locales) {
            result.add(locale.toString());
        }
        return result;
    }

    /**
     * Convert array of strings into locales
     */
    public static List<Locale> convertStringArrayToLocaleList(String[] localeCodes) {
        if (localeCodes == null || localeCodes.length == 0) {
            return Collections.emptyList();
        }
        List<Locale> result = Lists.newArrayList();
        for (String localeCode : localeCodes) {
            String trimmedCode = localeCode.trim();
            if (trimmedCode.equals("null")) {
                result.add(null);
            } else {
                result.add(Locale.toLocale(trimmedCode));
            }
        }
        return result;
    }

    public static boolean sameLanguage(Locale locale1, Locale locale2) {
        return locale1.getLanguage().equals(locale2.getLanguage());
    }

    public static boolean isCountry(Locale locale) {
        return locale.getLanguage().length() > 0 &&
                locale.getCountry().length() > 0 &&
                locale.getVariant().length() == 0;
    }

    /**
     * Returns the set of country-specific locales for the specified language.  Excludes variants.
     */
    public static List<Locale> getCountriesForLanguage(Collection<Locale> locales, Locale language) {
        List<Locale> countries = Lists.newArrayList();
        for (Locale locale : locales) {
            if (isCountry(locale) && sameLanguage(language, locale)) {
                countries.add(locale);
            }
        }
        return countries;
    }
}
