package com.bazaarvoice.cca.util;

import java.util.Date;

public class TimePeriod {

    private Date _startDate;
    private Integer _numDays;

    public TimePeriod(Date startDate, Integer numDays) {
        _startDate = startDate;
        _numDays = numDays;
    }

    public Date getStartDate() {
        return _startDate;
    }

    public void setStartDate(Date startDate) {
        _startDate = startDate;
    }

    public Integer getNumDays() {
        return _numDays;
    }

    public void setNumDays(Integer numDays) {
        _numDays = numDays;
    }
}
