package com.bazaarvoice.core.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Drop replacement for Locale with correction of language methods
 */
public class Locale implements Cloneable, Serializable {

    private final java.util.Locale _instance;

    public static final Locale ENGLISH = new Locale("en");
    public static final Locale US = new Locale("en", "US");
    public static final Locale GERMANY = new Locale("de", "DE");
    public static final Locale FRANCE = new Locale("fr", "FR");
    public static final Locale CHINESE = new Locale("zh");
    public static final Locale ROOT = new Locale("");

    public Locale(java.util.Locale instance) {
        Assert.isNotNull(instance);
        _instance = instance;
    }

    public Locale(String language, String country, String variant) {
        _instance = new java.util.Locale(language, country, variant);
    }

    public Locale(String language, String country) {
        _instance = new java.util.Locale(language, country);
    }

    public Locale(String language) {
        _instance = new java.util.Locale(language);
    }

    public java.util.Locale unwrap() {
        return _instance;
    }

    public static java.util.Locale unwrap(Locale locale) {
        return locale == null ? null : locale.unwrap();
    }

    public static Locale wrap(java.util.Locale locale) {
        return locale == null ? null : new Locale(locale);
    }

    private static List<Locale> wrap(List<java.util.Locale> list) {
        List<Locale> result = new ArrayList<Locale>(list.size());
        for (java.util.Locale locale : list) {
            result.add(Locale.wrap(locale));
        }
        return result;
    }

    public static Locale getDefault() {
        return new Locale(java.util.Locale.getDefault());
    }

    public static void setDefault(Locale newLocale) {
        java.util.Locale.setDefault(newLocale.unwrap());
    }

    public static Locale[] getAvailableLocales() {
        return wrap(Arrays.asList(java.util.Locale.getAvailableLocales())).toArray(new Locale[4]);
    }

    public static String[] getISOCountries() {
        return java.util.Locale.getISOCountries();
    }

    public String getLanguage() {
        String string = _instance.getLanguage();
        if ("in".equals(string)) {
            string = "id";
        } else if ("iw".equals(string)) {
            string = "he";
        } else if ("ji".equals(string)) {
            string = "yi";
        }
        return string;
    }

    public String getCountry() {
        return _instance.getCountry();
    }

    public String getVariant() {
        return _instance.getVariant();
    }

    @Override
    public String toString() {
        String string = _instance.toString();
        if ("in".equals(_instance.getLanguage())) {
            string = "id" + string.substring(2);
        } else if ("iw".equals(_instance.getLanguage())) {
            string = "he" + string.substring(2);
        } else if ("ji".equals(_instance.getLanguage())) {
            string = "yi" + string.substring(2);
        }
        return string;
    }

    public final String getDisplayLanguage() {
        return _instance.getDisplayLanguage();
    }

    public String getDisplayLanguage(Locale inLocale) {
        return _instance.getDisplayLanguage(inLocale.unwrap());
    }

    public final String getDisplayCountry() {
        return getDisplayCountry(getDefault());
    }

    public String getDisplayCountry(Locale inLocale) {
        return _instance.getDisplayCountry(inLocale.unwrap());
    }

    public final String getDisplayVariant() {
        return getDisplayVariant(getDefault());
    }

    public String getDisplayVariant(Locale inLocale) {
        return _instance.getDisplayVariant(inLocale.unwrap());
    }

    public final String getDisplayName() {
        return getDisplayName(getDefault());
    }

    public String getDisplayName(Locale inLocale) {
        return _instance.getDisplayName(inLocale.unwrap());
    }

    @Override
    public int hashCode() {
        return _instance.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof Locale && _instance.equals(((Locale) obj).unwrap());
    }

    public static List<Locale> localeLookupList(Locale locale) {
        //noinspection unchecked
        return wrap(LocaleUtils.localeLookupList(Locale.unwrap(locale)));
    }

    public static Locale toLocale(String string) {
        return wrap(org.apache.commons.lang.LocaleUtils.toLocale(string));
    }
}
