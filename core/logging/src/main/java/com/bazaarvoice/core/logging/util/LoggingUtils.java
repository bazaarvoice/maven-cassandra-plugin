package com.bazaarvoice.core.logging.util;

import org.apache.log4j.Appender;
import org.apache.log4j.AsyncAppender;
import org.apache.log4j.LogManager;
import org.apache.log4j.spi.Filter;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public abstract class LoggingUtils {

    public static Set<Filter> findFilters() {
        Set<Filter> filters = new HashSet<Filter>();
        for(Appender appender : findAppenders()) {
            for(Filter filter = appender.getFilter(); filter != null; filter = filter.getNext()) {
                filters.add(filter);
            }
        }
        return filters;
    }

    public static Set<Appender> findAppenders() {
        Set<Appender> allAppenders = new HashSet<Appender>();

        //noinspection unchecked
        for(Appender appender : Collections.list((Enumeration<Appender>) LogManager.getRootLogger().getAllAppenders())) {
            addAppender(appender, allAppenders);
        }

        return allAppenders;
    }

    private static void addAppender(Appender appender, Set<Appender> allAppenders) {
        if(allAppenders.add(appender) && appender instanceof AsyncAppender) {
            //Special case AsyncAppenders because they can contain appenders of their own
            AsyncAppender asyncAppender = (AsyncAppender) appender;
            //noinspection unchecked
            for(Appender subAppender : Collections.list((Enumeration<Appender>) asyncAppender.getAllAppenders())) {
                addAppender(subAppender, allAppenders);
            }
        }
    }
}
