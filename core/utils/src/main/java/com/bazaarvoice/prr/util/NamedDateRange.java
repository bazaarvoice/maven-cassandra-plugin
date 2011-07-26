package com.bazaarvoice.prr.util;

import com.bazaarvoice.cca.model.Day;
import com.bazaarvoice.core.util.Locale;
import com.bazaarvoice.cca.util.LocaleResourceUtils;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

public class NamedDateRange implements Serializable {

    /**
     * Enumeration of named ReportDateRange objects.
     */
    public enum Type implements StringValuedEnum {
        /**
         * If it is "2006-07-13 12:34:56" now then "last 24 hours" is
         * "2006-07-12 13:00:00" to "2006-07-13 13:00:00".
         */
        LAST_24_HOURS(24, Calendar.HOUR_OF_DAY, false, 0),

        /**
         * If it is "2006-07-13 12:34:56" now then "last 2 days" is
         * "2006-07-012 00:00:00" to "2006-07-14 00:00:00".
         */
        LAST_TWO_DAYS(2, Calendar.DATE, false, 0),

        /**
         * If it is "2006-07-13 12:34:56" now then "last 7 days" is
         * "2006-07-07 00:00:00" to "2006-07-14 00:00:00".
         */
        LAST_7_DAYS(7, Calendar.DATE, false, 0),

        /**
         * If it is "2006-07-13 12:34:56" now then "last 7 days" is
         * "2006-07-06 00:00:00" to "2006-07-13 00:00:00".
         */
        LAST_FULL_7_DAYS(7, Calendar.DATE, false, 0, -1),

        LAST_FULL_30_DAYS(30, Calendar.DATE, false, 0, -1),

        LAST_FULL_60_DAYS(60, Calendar.DATE, false, 0, -1),

        LAST_FULL_90_DAYS(90, Calendar.DATE, false, 0, -1),
		
        LAST_FULL_180_DAYS(180, Calendar.DATE, false, 0, -1),
		
        LAST_FULL_365_DAYS(365, Calendar.DATE, false, 0, -1),
		
        /**
         * If it is "2006-07-13 12:34:56" now then "today" is
         * "2006-07-13 00:00:00" to "2006-07-14 00:00:00".
         */
        TODAY(1, Calendar.DATE, false, 0),

        YESTERDAY(TODAY, -1),

        /**
         * If it is "2006-07-13 12:34:56" now then "this week" is
         * "2006-07-09 00:00:00" to "2006-07-16 00:00:00" (Sun to Sun).
         */
        THIS_WEEK(1, Calendar.WEEK_OF_YEAR, false, 0),

        LAST_WEEK(THIS_WEEK, -1),

        /**
         * If it is "2006-07-13 12:34:56" now then "this month" is
         * "2006-07-01 00:00:00" to "2006-08-01 00:00:00".
         */
        THIS_MONTH(1, Calendar.MONTH, false, 0),

        LAST_MONTH(THIS_MONTH, -1),

        /**
         * If it is "2006-07-13 12:34:56" now then this quarter is
         * "2006-06-01 00:00:00" to "2006-09-01 00:00:00".
         */
        THIS_QUARTER(3, Calendar.MONTH, true, 0),

        LAST_QUARTER(THIS_QUARTER, -1),

        /**
         * If it is "2006-07-13 12:34:56" now then this year is
         * "2006-01-01 00:00:00" to "2007-01-01 00:00:00".
         */
        THIS_YEAR(1, Calendar.YEAR, false, 0),

        LAST_YEAR(THIS_YEAR, -1),

        LAST_3_YEARS(1095, Calendar.DATE, false, 0, -1),

        /**
         * No restriction on date/time.
         */
        ALL_DATES;


        private int _duration;
        private int _durationUnit;  // Calendar field
        private boolean _alignInCalendar;
        private int _periodsAhead;
        private int _shift = 0;

        Type(int duration, int durationUnit, boolean alignInCalendar, int periodsAhead, int shift) {
            this(duration, durationUnit, alignInCalendar, periodsAhead);
            _shift = shift;
        }

        Type(int duration, int durationUnit, boolean alignInCalendar, int periodsAhead) {
            _duration = duration;
            _durationUnit = durationUnit;
            _alignInCalendar = alignInCalendar;
            _periodsAhead = periodsAhead;
        }

        /**
         * Creates a date range that's defined as relative to another date range.
         */
        Type(Type range, int periodsAhead) {
            _duration = range._duration;
            _durationUnit = range._durationUnit;
            _alignInCalendar = range._alignInCalendar;
            _periodsAhead = range._periodsAhead + periodsAhead;
        }

        /**
         * All time constructor.
         */
        Type() {
            this(Integer.MAX_VALUE, 0, false, 0);
        }

        @Deprecated
        public String getDescription() {
            return getDescription(null);
        }

        public String getDescription(Locale locale) {
            return LocaleResourceUtils.getMessage(NamedDateRange.class, locale, name() + "__description");
        }

        public String getValue() {
            return name();
        }

    }

    private Type _rangeType;
    private Day _startDayOfWeek;

    /**
     * Calendar fields that we use, in order of smallest unit of time to biggest.
     */
    private static final int[] RELATIVE_ORDER = {
            Calendar.MILLISECOND,
            Calendar.SECOND,
            Calendar.MINUTE,
            Calendar.HOUR_OF_DAY,
            Calendar.DATE,
            Calendar.WEEK_OF_YEAR,
            Calendar.MONTH,
            Calendar.YEAR
    };

    /* default constructor is only used by Hibernate*/
    @SuppressWarnings ({"UnusedDeclaration"})
    private NamedDateRange() {
    }

    public NamedDateRange(NamedDateRange source) {
        this(source.getRangeType(), source.getStartDayOfWeek());
    }

    public NamedDateRange(Type type, Day startDayOfWeek) {
        setRangeType(type);
        setStartDayOfWeek(startDayOfWeek);
    }

    /**
     * Returns a range relative to right now.
     */
    public ReportDateRange range() {
        // special case for all time
        if (_rangeType._duration == Integer.MAX_VALUE) {
            return ReportDateRange.createAllTimeDateRange();
        }

        // get the current date + time.  use server's timezone and locale
        Calendar end = Calendar.getInstance();
        // set to the right start day if the named named date range has a right date. Default is SUNDAY
        if (getStartDayOfWeek() != null) {
            end.setFirstDayOfWeek(getStartDayOfWeek().getCalendarDay());
        }

        // adjust range according to specified shift
        end.setTime(DateUtils.add(end.getTime(), _rangeType._durationUnit, _rangeType._shift));

        // round up to the nearest hour/day/week/month/year
        roundUp(end, _rangeType._durationUnit);

        // usually a value like "last 6 hours" means just that--the previous
        // six hours.  but some units (ie. "this quarter") only make sense if
        // they're aligned within calendar such that their value is an integral
        // number of periods since the first of the year.  move the end date
        // back to make this happen.  for example, if a quarter is defined as
        // 3 months, move "May 1st" (month=5) to "June 1st" (month=6)
        if (_rangeType._alignInCalendar && _rangeType._duration > 1) {
            int offset = end.get(_rangeType._durationUnit) - end.getMinimum(_rangeType._durationUnit);
            if ((offset % _rangeType._duration) != 0) {
                end.add(_rangeType._durationUnit, _rangeType._duration - (offset % _rangeType._duration));
            }
        }

        // move from the current period to the n periods in the future (n is usually negative)
        if (_rangeType._periodsAhead != 0) {
            end.add(_rangeType._durationUnit, _rangeType._periodsAhead * _rangeType._duration);
        }

        // grab the end date
        Date endDate = end.getTime();

        // adjust back to the start date
        end.add(_rangeType._durationUnit, -_rangeType._duration);
        Date startDate = end.getTime();

        // are the start & end dates at midnight?  or do they have a relevant time component?
        boolean includesTime = relativeOrder(_rangeType._durationUnit) < relativeOrder(Calendar.DATE);

        return new ReportDateRange(_rangeType.getDescription(), startDate, endDate, includesTime);
    }

    /**
     * Rounds a calendar time upwards to an integral number of units (day/month/year/...)
     *
     * @param cal  the calendar to adjust
     * @param unit a Calendar field value (Calendar.DATE/MONTH/YEAR/...)
     */
    private void roundUp(Calendar cal, int unit) {
        Date time = cal.getTime();

        // start by rounding down.  for all date/time units that are
        // smaller than 'unit', set the value to 0 (mod special cases below)
        int unitOrder = relativeOrder(unit);
        for (int i = 0; i < unitOrder; i++) {
            int unitToRound = RELATIVE_ORDER[i];

            if (unitToRound == Calendar.DATE) {
                // special case for rounding date (unit == week|month|year)
                if (unit == Calendar.WEEK_OF_MONTH || unit == Calendar.WEEK_OF_YEAR) {
                    // unit == week
                    cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek()); // guaranteed to round date down
                } else {
                    // unit == month|year
                    cal.set(Calendar.DATE, 1);
                }
            } else if (unitToRound == Calendar.WEEK_OF_MONTH || unitToRound == Calendar.WEEK_OF_YEAR) {
                // do nothing.  we never round week values
            } else {
                cal.set(unitToRound, 0);
            }
        }

        // we've rounded down.  if that changed anything, round up one unit
        if (cal.getTime().before(time)) {
            cal.add(unit, 1);
        }
    }

    /**
     * Returns a number that can be used to compare date units to see which
     * is bigger (millisecond < second < minute < hour < day < week < month < year ...)
     */
    private static int relativeOrder(int calendarField) {
        for (int i = 0; i < RELATIVE_ORDER.length; i++) {
            if (calendarField == RELATIVE_ORDER[i]) {
                return i;
            }
        }
        throw new UnsupportedOperationException("Relative order needs to be updated for calendar field: " + calendarField);
    }

    public Day getStartDayOfWeek() {
        return _startDayOfWeek;
    }

    public void setStartDayOfWeek(Day startDayOfWeek) {
        _startDayOfWeek = startDayOfWeek;
    }

    public Type getRangeType() {
        return _rangeType;
    }

    public void setRangeType(Type rangeType) {
        _rangeType = rangeType;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(_rangeType)
                .append(_startDayOfWeek)
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        NamedDateRange other = (NamedDateRange) obj;
        return new EqualsBuilder()
                .append(_rangeType, other.getRangeType())
                .append(_startDayOfWeek, other.getStartDayOfWeek())
                .isEquals();
    }
}
