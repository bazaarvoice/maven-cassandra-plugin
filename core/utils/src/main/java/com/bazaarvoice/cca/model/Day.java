package com.bazaarvoice.cca.model;

import com.bazaarvoice.core.util.Locale;
import com.bazaarvoice.prr.util.StringValuedEnum;

import java.util.Calendar;

public enum Day implements StringValuedEnum {
    SUNDAY,
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY;

    public int getCalendarDay() {
        // rely on Calendar.SUNDAY == 1 ... Calendar.SATURDAY == 7
        return ordinal() + 1;
    }

    public static Day getDay(Calendar cal) {
        // rely on Calendar.SUNDAY == 1 ... Calendar.SUNDAY == 7
        return Day.values()[cal.get(Calendar.DAY_OF_WEEK) - 1];
    }

    public static Day getToday() {
        return getDay(Calendar.getInstance());
    }

    public static Day getNextDay(Day day) {
        Calendar calendar = Calendar.getInstance();
        calendar.roll(Calendar.DATE, true);
        return getDay(calendar);
    }

    public String getDescription(Locale locale) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, getCalendarDay());
        return calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.unwrap(locale));
    }

    public String getValue() {
        return name();
    }
}
