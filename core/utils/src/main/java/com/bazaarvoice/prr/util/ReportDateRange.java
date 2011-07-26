package com.bazaarvoice.prr.util;

import java.io.Serializable;
import java.util.Date;

/**
 * A date range from a start date (inclusive) to an end date (exclusive).
 */
public class ReportDateRange implements Serializable {

    private String _description;
    private Date _start;
    private Date _end;
    private boolean _includesTime;

    /**
     *  Static factory for special case of all dates
     */
     public static ReportDateRange createAllTimeDateRange() {
        return new ReportDateRange(NamedDateRange.Type.ALL_DATES.getDescription(), null, null, false);
    }

    public ReportDateRange() {
    }

    public ReportDateRange(ReportDateRange source) {
        this(source.getDescription(),
             source.getStart() == null ? null : (Date) source.getStart().clone(),
             source.getEnd() == null ? null : (Date) source.getEnd().clone(),
             source.getIncludesTime());
    }

    public ReportDateRange(String description, Date start, Date end, boolean includesTime) {
        _description = description;
        _start = start;
        _end = end;
        _includesTime = includesTime;
    }

    public String getDescription() {
        return _description;
    }

    public void setDescription(String description) {
        _description = description;
    }

    public Date getStart() {
        return _start;
    }

    public void setStart(Date start) {
        _start = start;
    }

    public Date getEnd() {
        return _end;
    }

    public void setEnd(Date end) {
        _end = end;
    }

    /**
     * Wrapper for the getEndDate() which is used in the web ui.  See also {@link #setDisplayEnd(java.util.Date)}
     *
     * Ths users interact with inclusive dates; this takes care of translating the exclusive date
     * we store for the backend into that format.
     */
    public Date getDisplayEnd() {
        if (getEnd() != null) {
            return new Date(getEnd().getTime() - 1);
        }

        return null;
    }

    /**
     * Wrapper for setEndDate() which is used in the web ui.  See also {@link #getDisplayEnd}
     *
     * The users interact with inclusive dates; this takes care of translating that date
     * into the exclusive dates preferred by the backend.
     */
    public void setDisplayEnd(Date inclusiveEndDate) {
        Date endDate = null;

        if (inclusiveEndDate != null) {
            endDate = DateUtils.getEndOfDay(inclusiveEndDate);
        }

        setEnd(endDate);
    }

    /**
     * Returns true if the start and end dates include a relevant time component.
     */
    public boolean getIncludesTime() {
        return _includesTime;
    }

    public void setIncludesTime(boolean includesTime) {
        _includesTime = includesTime;
    }

    public boolean contains(Date date) {
        return _start.compareTo(date) <= 0 && date.compareTo(_end) < 0;
    }

    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("{description = ").append(_description).
                append(", start = ").append(_start).
                append(", end = ").append(_end).
                append("}");
        return s.toString();
    }
}
