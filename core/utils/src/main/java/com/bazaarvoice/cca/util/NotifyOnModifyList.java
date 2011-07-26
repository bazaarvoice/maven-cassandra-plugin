package com.bazaarvoice.cca.util;

import com.google.common.collect.ForwardingList;
import com.google.common.collect.ForwardingListIterator;

import java.util.List;
import java.util.Collection;
import java.util.ListIterator;
import java.util.Iterator;

/**
 * Decorates a java.util.List to provide notifications when the list is modified.
 *
 * Currently, the only supported notification is when the list size changes. 
 */
public class NotifyOnModifyList<E> extends ForwardingList<E> {

    public interface SizeChangeListener {
        /**
         * Called after the size of the inner list has changed
         */
        void sizeChanged(int sizeDelta);
    }

    private final List<E> _delegate;

    private SizeChangeListener _sizeChangeListener;

    public NotifyOnModifyList(List<E> delegate) {
        _delegate = delegate;
    }

    public NotifyOnModifyList<E> withSizeChangeListener(SizeChangeListener sizeChangeListener) {
        _sizeChangeListener = sizeChangeListener;
        return this;
    }

    @Override
    protected List<E> delegate() {
        return _delegate;
    }

    private void sizeChanged(int sizeDelta) {
        if (_sizeChangeListener != null) {
            _sizeChangeListener.sizeChanged(sizeDelta);
        }
    }

    @Override
    public void add(int i, E e) {
        super.add(i, e);
        sizeChanged(+1);
    }

    @Override
    public boolean addAll(int i, Collection<? extends E> es) {
        boolean sizeChanged = super.addAll(i, es);
        if (sizeChanged) {
            sizeChanged(+es.size());
        }
        return sizeChanged;
    }

    @Override
    public ListIterator<E> listIterator() {
        return decorate(super.listIterator());
    }

    @Override
    public ListIterator<E> listIterator(int i) {
        return decorate(super.listIterator(i));
    }

    private ListIterator<E> decorate(final ListIterator<E> delegate) {
        return new ForwardingListIterator<E>() {
            @Override
            protected ListIterator<E> delegate() {
                return delegate;
            }

            @Override
            public void add(E e) {
                super.add(e);
                sizeChanged(+1);
            }

            @Override
            public void remove() {
                super.remove();
                sizeChanged(-1);
            }
        };
    }

    @Override
    public E remove(int i) {
        E removedItem = super.remove(i);
        sizeChanged(-1);
        return removedItem;
    }

    @Override
    public List<E> subList(int from, int to) {
        return new NotifyOnModifyList<E>(super.subList(from, to)).withSizeChangeListener(_sizeChangeListener);
    }

    @Override
    public Iterator<E> iterator() {
        return listIterator();
    }

    @Override
    public boolean removeAll(Collection<?> objects) {
        int previousSize = size();
        boolean sizeChanged = super.removeAll(objects);
        if (sizeChanged) {
            sizeChanged(size() - previousSize);
        }
        return sizeChanged;
    }

    @Override
    public boolean add(E e) {
        boolean sizeChanged = super.add(e);
        sizeChanged(+1);
        return sizeChanged; // always true
    }

    @Override
    public boolean remove(Object o) {
        boolean sizeChanged = super.remove(o);
        if (sizeChanged) {
            sizeChanged(-1);
        }
        return sizeChanged;
    }

    @Override
    public boolean addAll(Collection<? extends E> es) {
        boolean sizeChanged = super.addAll(es);
        if (sizeChanged) {
            sizeChanged(+es.size());
        }
        return sizeChanged;
    }

    @Override
    public boolean retainAll(Collection<?> objects) {
        int previousSize = size();
        boolean sizeChanged = super.retainAll(objects);
        if (sizeChanged) {
            sizeChanged(size() - previousSize);
        }
        return sizeChanged;
    }

    @Override
    public void clear() {
        int previousSize = size();
        super.clear();
        if (previousSize > 0) {
            sizeChanged(-previousSize);
        }
    }
}
