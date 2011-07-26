package com.bazaarvoice.core.environment.util;

import java.util.Properties;

/**
 * A holder of a Properties object containing application configuration settings.
 * Created when necessary by Spring using the {@link PropertiesResourceEditor}. 
 */
public class PropertiesResource {

	private final Properties _props;

	public PropertiesResource(Properties props) {
		_props = props;
	}

	public Properties getProperties() {
		return _props;
	}
}
