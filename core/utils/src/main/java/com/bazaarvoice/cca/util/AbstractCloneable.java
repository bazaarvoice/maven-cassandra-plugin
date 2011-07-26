package com.bazaarvoice.cca.util;

/**
 * Base class for Cloneable objects that declares a public clone() method
 * that doesn't throw a checked exception.
 */
public abstract class AbstractCloneable implements Cloneable {

	@SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException"})
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new IllegalStateException(e);  // Cloneable objects should always be cloneable
		}
	}
}
