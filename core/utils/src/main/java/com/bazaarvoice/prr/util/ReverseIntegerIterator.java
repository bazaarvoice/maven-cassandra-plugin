package com.bazaarvoice.prr.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Special class for generating reverse sequence of the Integer digits
 */
public class ReverseIntegerIterator implements Iterator<Integer> {
	private int _counter;

	public ReverseIntegerIterator(int maxItems) {
		_counter = maxItems;
	}

	public boolean hasNext() {
		return _counter > 0;
	}

	public Integer next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		return --_counter;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
}

