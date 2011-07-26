package com.bazaarvoice.cca.util;

import java.util.Comparator;
import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Collections;

/**
 */
public class ToStringComparator<T> implements Comparator<T> {

    public int compare(T obj1, T obj2) {
        if (obj1 == null && obj2 == null) {
            return 0;
        }
        if (obj1 == null) {
            return -1;
        }
        if (obj2 == null) {
            return 1;
        }
        return obj1.toString().compareTo(obj2.toString());
    }

    public static <T> List<T> sort(Collection<T> values) {
        List<T> sortedList = new ArrayList<T>(values);
        Collections.sort(sortedList, new ToStringComparator<T>());
        return sortedList;
    }
}
