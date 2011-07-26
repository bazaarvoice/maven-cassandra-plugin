package com.bazaarvoice.core.environment.util;

/**
 * Exception thrown by the <error> tag in a PropertiesLoader.dtd XML file.
 */
public class PropertiesLoaderException extends RuntimeException {

	public PropertiesLoaderException(String message) {
		super(message);
	}
}
