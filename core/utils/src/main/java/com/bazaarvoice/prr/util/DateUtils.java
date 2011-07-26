package com.bazaarvoice.prr.util;

import com.bazaarvoice.cca.model.Day;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.ISODateTimeFormat;

import java.sql.Timestamp;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Date and time utility methods
 */
public abstract class DateUtils extends org.apache.commons.lang.time.DateUtils {

    public static final int CURRENT_YEAR = Calendar.getInstance().get(Calendar.YEAR);

    public static final DateFormatSymbols DATE_FORMAT_SYMBOLS = new DateFormatSymbols();

    public static final Date BEGINNING_OF_TIME = beginningOfTime();
    public static final Date END_OF_TIME = endOfTime();

    public static final Timestamp DB_END_OF_TIME = new Timestamp(endOfTime().getTime());

    public static final int[] DAYS_OF_WEEK = {
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY,
            Calendar.SUNDAY
    };    

    private static final int[][] _sRoundingConstants = {
            {Calendar.MILLISECOND, 1000},
            {Calendar.SECOND, 60},
            {Calendar.MINUTE, 60},
            {Calendar.HOUR, 12},
    };

    public static final String[] MONTH_NAME = {
            "January",
            "February",
            "March",
            "April",
            "May",
            "June",
            "July",
            "August",
            "September",
            "October",
            "November",
            "December"
    };

    public static final String[] MONTH_NAME_SHORT= {
            "Jan",
            "Feb",
            "Mar",
            "Apr",
            "May",
            "Jun",
            "Jul",
            "Aug",
            "Sep",
            "Oct",
            "Nov",
            "Dec"
    };

    /** Returns midnight, the morning of the specified day.  Uses the server's TimeZone. */
    public static Date getStartOfDay(Date date) {
        return truncate(date, Calendar.DATE);
    }

    /** Returns 12pm, the afternoon of the specified day.  Uses the server's TimeZone. */
    public static Date getMiddleOfDay(Date date) {
        // truncate the time fields then set to 12:00pm.  use Calendar methods to ensure we get
        // the right answer even on daylight savings days when there are 23 or 25 hours in a day.
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal = truncate(cal, Calendar.HOUR);
        cal.set(Calendar.HOUR_OF_DAY, 12);
        return cal.getTime();
    }

    /** Returns midnight, the evening of the specified day (actually 12:00am the next day).  Uses the server's TimeZone. */
    public static Date getEndOfDay(Date date) {
        // add an entire day, then truncate down.  use Calendar methods to ensure we get the
        // right answer even on daylight savings days when there are 23 or 25 hours in a day.
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, 1);
        return truncate(cal, Calendar.DATE).getTime();
    }

    public static int getDayOfWeek(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.DAY_OF_WEEK);
    }

    public static int getDayOfMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.DAY_OF_MONTH);
    }

    public static Date getNextDayOfWeek(Date date, Day dayOfWeek) {
        int daysTillNextDayOfWeek = dayOfWeek.getCalendarDay() - getDayOfWeek(date);
        if (daysTillNextDayOfWeek <= 0) {
            daysTillNextDayOfWeek += 7;
        }
        return addDays(date, daysTillNextDayOfWeek);
    }

    public static Date getNextDayOfMonth(Date date, int dayOfMonth) {
        Calendar cal = Calendar.getInstance();
        //if the dayOfMonth(date) < dayOfMonth, then the return date will be later in the same month
        if(getDayOfMonth(date) < dayOfMonth) {
            cal.setTime(date);
        }  else {
            //otherwise send it on the scheduled-day-of-month of the next month
            Date nextMonth = addMonths(date, 1);
            cal.setTime(nextMonth);
        }

        //if the scheduled day of the month is > the number of days in the month (ex: scheduled day = 31, month = February), then use the largest day of the month
        if(dayOfMonth <= cal.getActualMaximum(Calendar.DAY_OF_MONTH)) {
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        } else {
            cal.set(Calendar.DATE, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        }
        return cal.getTime();
    }

    /**
     * Returns the String name of the current month, with regular capitalization (e.g. "January")
     */
    public static String getNameOfCurrentMonth() {
        return getNameOfMonth(Calendar.getInstance().get(Calendar.MONTH));
    }

    /**
     * Returns the String name of the month corresponding to the given integer, with regular capitalization (e.g. "January")
     * Note that the months are zero indexed like in the Calendar constants (i.e. JANUARY=0, FEBRUARY=1, etc.)
     */
    public static String getNameOfMonth(int month) {
        if (month < 0 || month > 11) {
            throw new IndexOutOfBoundsException("month must be between 0 and 11");
        }
        return MONTH_NAME[month];
    }

    /**
     * Returns the short String name of the month corresponding to the given integer, with regular capitalization (e.g. "Jan")
     * Note that the months are zero indexed like in the Calendar constants (i.e. JANUARY=0, FEBRUARY=1, etc.)
     */
    public static String getShortNameOfMonth(int month) {
        if (month < 0 || month > 11) {
            throw new IndexOutOfBoundsException("month must be between 0 and 11");
        }
        return MONTH_NAME_SHORT[month];
    }

    /**
     * Returns the integer corresponding to the last day of the current calendar month.
     */
    public static int getLastDayOfThisMonth() {
        return getLastDayOfMonth(Calendar.getInstance().get(Calendar.MONTH));
    }

    /**
     * Returns the integer corresponding to the last day of the given month.  The int arguments this
     * expects are the zero based month integers as in the Calendar constants (i.e. JANUARY=0, FEBRUARY=1, etc.)
     */
    public static int getLastDayOfMonth(int month) {
        return getLastDayOfMonth(month, Calendar.getInstance().get(Calendar.YEAR));
    }

    /**
     * Returns the integer corresponding to the last day of the given month, in the given year.  The int arguments this
     * expects are the zero based month integers as in the Calendar constants (i.e. JANUARY=0, FEBRUARY=1, etc.), and
     * the int year number
     */
    public static int getLastDayOfMonth(int month, int year) {
        Calendar calendar = Calendar.getInstance();

        // we need to ensure that this Calendar instance is refering to a day that is actually in the given month
        // (otherwise it acts weird...).  We know every month has a first day, so:
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.YEAR, year);
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    public static Calendar combineDateAndTime(Date date, Date time) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        // copy the hour/minute/second/millisecond fields from startTime
        Calendar timeCal = Calendar.getInstance();
        timeCal.setTime(time);
        cal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
        cal.set(Calendar.SECOND, timeCal.get(Calendar.SECOND));
        cal.set(Calendar.MILLISECOND, timeCal.get(Calendar.MILLISECOND));
        
        return cal;
    }
    
    /**
     * Returns a new calendar where the date has been rounded up to the
     * nearest value of the specified time field where the value is a
     * multiple of the specified step.  For example
     * <pre>roundUpToTimeMultiple(cal, Calendar.MINUTE, 15)</pre> will
     * return a calendar with the time rounded up to the nearest 15
     * minute interval.
     * @param cal the time to start with
     * @param field one of Calendar.MILLISECOND, Calendar.SECOND, Calendar.MINUTE, Calendar.HOUR
     * @param step the multiple of the field value to round up to
     * @return a copy of cal, rounded up to the nearest multiple of the specified field and step.
     */
    public static Calendar roundUpToTimeMultiple(Calendar cal, int field, int step) {
        cal = (Calendar) cal.clone();
        for (int[] roundingConstants : _sRoundingConstants) {
            if (roundingConstants[0] == field) {
                roundUpSingleField(cal, field, step);
                return cal;
            }
            roundUpSingleField(cal, roundingConstants[0], roundingConstants[1]);
        }
        throw new IllegalArgumentException("Unexpected time field value: " + field);
    }

    private static void roundUpSingleField(Calendar cal, int field, int step) {
        int value = cal.get(field);
        if (value % step != 0) {
            cal.add(field, step - (value % step));
        }
    }

    /** DB-friendly date comparison function can compare java.util.Date with java.sql.Timestamp. */
    public static boolean equals(Date date1, Date date2) {
        return (date1 == date2) || (date1 != null && date2 != null && isSameInstant(date1, date2));
    }

    public static long getSecondsFromEpoch(Date date) {
        return date.getTime() / 1000;
    }

    private static Date beginningOfTime() {
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse("1970-01-01");
        } catch (ParseException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Date endOfTime() {
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse("2099-12-31");
        } catch (ParseException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns the latest of the provided dates. The provided array of dates may contain null values;
     * null is returned iff all the provided dates are null.
     */
    public static Date getMax(Date... dates) {
        Date result = null;
        for (Date date : dates) {
            if (date == null) {
                continue;
            }
            if (result == null || date.after(result)) {
                result = date;
            }
        }
        return result;
    }

    /**
     * @return the number of partial calendar days elapsed between two dates. Returns negative integer if the second date is earlier than first.
     *
     * E.g.:<br>
     * <ul>
     * <li><code>      getPartialDaysElapsed("2010-04-01",         "2010-04-02") = 1</code></li>
     * <li><nobr><code>getPartialDaysElapsed("2010-04-01 23:59:59","2010-04-02 00:00:00") = 1</code></nobr></li>
     * </ul>
     */
    public static int getPartialDaysElapsed(Date since, Date till) {
        return Days.daysBetween(new DateTime(getStartOfDay(since)), new DateTime(getStartOfDay(till))).getDays();
    }

    /**
     * Calculates the number of full days since a given date.
     */
    public static int getFullDaysElapsed(Date since, Date till) {
        // Here's some interesting pertinent information regarding screwing up date diffs:
        // http://www.xmission.com/~goodhill/dates/deltaDates.html
        return Days.daysBetween(new DateTime(since), new DateTime(till)).getDays();
    }

    public static boolean isValid(Date timestamp) {
        long seconds = timestamp.getTime() / 1000;
        // check the number of seconds is within the integer range so that SOLR handles it appropriately
        if (seconds < Integer.MIN_VALUE || seconds > Integer.MAX_VALUE) {
            return false;// the date is not supported
        }
        return true;
    }

    /**
     * Returns the date in ISO 8601-style format with date, time, timezone.  For example: "2011-01-02T23:45:60.543+06:00".
     */
    public static String toISOTimestamp(Date timestamp) {
        return ISODateTimeFormat.dateTime().print(timestamp.getTime());
    }

    /**
     * Returns the date in ISO 8601-style format with date, time (no milliseconds), timezone.  For example: "2011-01-02T23:45:60+06:00".
     */
    public static String toISOTimestampNoMillis(long timestamp) {
        return ISODateTimeFormat.dateTimeNoMillis().print(timestamp);
    }
}
