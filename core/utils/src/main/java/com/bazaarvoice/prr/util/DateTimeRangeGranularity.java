package com.bazaarvoice.prr.util;

import java.util.Calendar;

import com.bazaarvoice.core.util.Locale;
import com.bazaarvoice.cca.util.LocaleResourceUtils;

/**
 * Granularity of datetime range
 */
public enum DateTimeRangeGranularity implements StringValuedEnum {
    HOUR(1, Calendar.HOUR_OF_DAY),

    DAY(24, Calendar.DATE),

    MONTH(24*30, Calendar.MONTH),

    YEAR(24*30*365, Calendar.YEAR);

    private final long _hours;
    private int _calendarConst;

    DateTimeRangeGranularity(long hours, int calendarConst) {
        _hours = hours;
        _calendarConst = calendarConst;
    }

    public String getValue() {
        return name();
    }

    public String getDescription(Locale locale) {
        return LocaleResourceUtils.getMessage(DateTimeRangeGranularity.class, locale, name() + "__description");
    }

    private long getHours() {
        return _hours;
    }

    public int getCalendarConst() {
        return _calendarConst;
    }

    public int compare(DateTimeRangeGranularity dateTimeRangeGranularity) {
        return new Long(_hours).compareTo(dateTimeRangeGranularity.getHours());
    }
}
