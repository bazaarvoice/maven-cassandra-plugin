package com.bazaarvoice.prr.util;

/**
 * Strategy for transforming an object of type T into an object of type V
 */
public interface Transformer<T, V> {
	public V transform(T object);
}
