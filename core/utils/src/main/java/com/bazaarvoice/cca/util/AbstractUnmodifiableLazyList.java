package com.bazaarvoice.cca.util;

import java.util.AbstractList;
import java.util.List;

/**
 * Abstract base class for a list that doesn't compute its contents until it is used.
 */
public abstract class AbstractUnmodifiableLazyList<T> extends AbstractList<T> {
    private List<T> _delegateList;

    private List<T> delegateList() {
        if (_delegateList == null) {
            _delegateList = buildList();
        }
        return _delegateList;
    }

    public T get(int index) {
        return delegateList().get(index);
    }

    public int size() {
        return delegateList().size();
    }

    protected abstract List<T> buildList();
}
