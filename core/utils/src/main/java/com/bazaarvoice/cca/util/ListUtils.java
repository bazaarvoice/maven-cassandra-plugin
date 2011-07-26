package com.bazaarvoice.cca.util;

import com.google.common.base.Predicate;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * List utilities.
 */
public class ListUtils {

    private ListUtils() {}

    public static <T> boolean moveToFirst(List<T> values, T value) {
        return move(values, value, 0);
    }

    /**
     * Move the provided value to the location indicated by the provided index in the list
     * (only if the value exists in the list based on {@link Object#equals(Object)} as defined
     * for the provided value type).
     * @return true if a move was made and the list was mutated; false otherwise.
     */
    public static <T> boolean move(List<T> values, T value, int index) {
        T found = null;
        for (Iterator<T> i = values.iterator(); i.hasNext();) {
            T candidate = i.next();
            if (ObjectUtils.equals(candidate, value)) {
                found = candidate;
                i.remove();
            }
        }
        if (found != null) {
            values.add(index, found);
            return true;
        }
        return false;
    }

    /**
     * Return a mutable list that contains all of the values from the provided list starting at the provided
     * from index, inclusive. Note that the provided from index can be out of bounds; the behavior is as if
     * the list returns 'no value' when accessed out of bounds:
     * <pre>
     * slice( [1, 2, 3], -1) =&gt; [1, 2, 3]
     * slice( [1, 2, 3], 1) =&gt; [2, 3]
     * </pre>
     */
    public static <T> List<T> slice(List<T> list, int from) {
        return slice(list, from, list.size());
    }

    /**
     * Return a mutable list that contains all of the values from the provided list starting at the provided
     * from index, inclusive, and up to the to index, exclusive. Note that the provided from and to indexes
     * can be out of bounds; the behavior is as if the list returned no values when accessed out of bounds:
     * <pre>
     * slice( [1, 2, 3], -1, 0) =&gt; []
     * slice( [1, 2, 3], -1, 1) =&gt; [1]
     * </pre> 
     */
    public static <T> List<T> slice(List<T> list, int from, int to) {
        List<T> slice = new ArrayList<T>();
        int safeFrom = Math.max(from, 0);
        int safeTo = Math.min(to, list.size());
        for (int i = safeFrom; i < safeTo; ++i) {
            slice.add(list.get(i));
        }
        return slice;
    }

    /**
     * Return a {@link List} of elements from the provided {@link List} whose values match the provided {@link Predicate}. 
     */
    public static <T> List<T> retain(List<T> list, Predicate<T> predicate) {
        return retain(list, predicate, null);
    }

    /**
     * Same behavior as {@link #retain(List, Predicate)} but if a value in the list cannot be retained a log at level warn is
     * omitted with full stack trace.
     * NOTE. To avoid warnings to the logging system, this method should only be used when ALL values in the provided {@link List}
     * are expected to match the provided {@link Predicate} but, in the rare case that a value fails the predicate, the application
     * may safely continue with the reduced list.
     */
    public static <T> List<T> retainOrWarn(List<T> list, Predicate<T> predicate, Log log) {
        return retain(list, predicate, log);
    }

    private static <T> List<T> retain(List<T> list, Predicate<T> predicate, Log log) {
        List<T> excluded = new ArrayList<T>(list.size());
        List<T> result = new ArrayList<T>(list.size());
        for (T item : list) {
            if (!predicate.apply(item)) {
                excluded.add(item);
                continue;
            }
            result.add(item);
        }
        if ((log != null) && !excluded.isEmpty()) {
            String msg = "Could not retain items: " + excluded;
            log.warn(msg);
        }
        return result;
    }

    /**
     * Filters a list in place (in reverse order) by removing all elements for which the given predicate
     * returns false.
     */
    public static <T> void filterInPlaceReverseOrder(List<T> list, Predicate<T> predicate) {
        if (list != null && predicate != null) {
            for (ListIterator<T> it = list.listIterator(list.size()); it.hasPrevious();) {
                if (!predicate.apply(it.previous())) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Trims a list in place to some size.
     */
    public static <T> void trimToSizeInPlace(final List<T> list, final int size) {
        if (size >= list.size()) {
            return;
        }

        filterInPlaceReverseOrder(list, new Predicate<T>() {
            private int elementsToTrim = list.size() - size;
            private int currentIndex = 0;

            @Override
            public boolean apply(T input) {
                if (currentIndex < elementsToTrim) {
                    currentIndex++;
                    return false;
                }
                return true;
            }
        });
    }
}
