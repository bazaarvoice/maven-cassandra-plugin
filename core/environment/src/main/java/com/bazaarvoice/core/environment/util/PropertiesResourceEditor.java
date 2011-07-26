package com.bazaarvoice.core.environment.util;

import com.bazaarvoice.core.environment.AbstractApplicationConfig;
import org.apache.commons.lang.StringUtils;

import java.beans.PropertyEditorSupport;
import java.util.Properties;

/**
 * Editor for PropertiesResource filenames, to automatically convert String
 * config file locations (e.g. "jdbc.xml") to PropertiesResource
 * objects instead of using a String configFile property.
 */
public class PropertiesResourceEditor extends PropertyEditorSupport {

    private final AbstractApplicationConfig _appConfig;

    public PropertiesResourceEditor(AbstractApplicationConfig appConfig) {
        _appConfig = appConfig;
    }

    /**
     * Converts String values to PropertiesLoader.
     */
    @Override
    public void setAsText(String text) {
        if (StringUtils.isNotBlank(text)) {
            setValue(loadConfig(text));
        } else {
            setValue(null);
        }
    }

    private PropertiesResource loadConfig(String fileNamePattern) {
        Properties props = new Properties(_appConfig.getEnvironment());
        _appConfig.loadConfigFiles(fileNamePattern, props);
        return new PropertiesResource(props);
    }
}
