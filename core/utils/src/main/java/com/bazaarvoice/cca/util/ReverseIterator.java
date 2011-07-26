package com.bazaarvoice.cca.util;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Reverses the order of iteration by mapping Iterator fuctions to a ListIterator
 */
public class ReverseIterator <T> implements Iterator<T>, Iterable<T> {
    private ListIterator<? extends T> _iterator;

    public ReverseIterator(List<? extends T> list) {
        //Start the iterator one past the end of the list
        _iterator = list.listIterator(list.size());
    }

    @Override
    public boolean hasNext() {
        return _iterator.hasPrevious();
    }

    @Override
    public T next() {
        return _iterator.previous();
    }

    @Override
    public void remove() {
        _iterator.remove();
    }

    @Override
    public Iterator<T> iterator() {
        return this;
    }
}
