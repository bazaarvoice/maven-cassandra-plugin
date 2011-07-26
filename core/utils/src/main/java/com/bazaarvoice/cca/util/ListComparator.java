package com.bazaarvoice.cca.util;

import org.apache.commons.collections.comparators.ComparableComparator;

import java.util.Comparator;
import java.util.List;

/**
 * Compares two lists analogous to lexigraphical ordering of Strings (see {@link String#compareTo(String)}), so ordering is determined by:
 * <ol>
 * <li>the ordering of objects at the first index for which the lists differ;</li>
 * <li> otherwise, the shorter list will be ordered first.</li>
 * </ol>
 *
 * Here is an example ordering that would result from this comparator:
 * <ul>
 * <li>List[1, 2]</li>
 * <li>List[1, 2, 3]<li>
 * <li>List[1, 3]<li>
 * </ul>
 */
public class ListComparator<T> implements Comparator<List<T>> {

    private final Comparator<T> _comparator;

    public ListComparator(Comparator<T> comparator) {
        _comparator = comparator;
    }

    @Override
    public int compare(List<T> row1, List<T> row2) {
        for (int i = 0; i < Math.max(row1.size(), row2.size()); i++) {
            if (i == row1.size()) {
                return -1;
            }
            if (i == row2.size()) {
                return 1;
            }
            int z = _comparator.compare(row1.get(i), row2.get(i));
            if (z != 0) {
                return z;
            }
        }
        return 0;
    }

    public static <T extends Comparable<T>> ListComparator<T> getInstance() {
        @SuppressWarnings ({"unchecked"}) Comparator<T> comparator = ComparableComparator.getInstance();
        return new ListComparator<T>(comparator);
    }
}
