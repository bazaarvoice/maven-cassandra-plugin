package com.bazaarvoice.core.environment.util;

import com.bazaarvoice.core.environment.AbstractApplicationConfig;
import org.apache.commons.lang.StringUtils;

import java.beans.PropertyEditorSupport;

/**
 * Editor for PropertiesLoader filenames, to automatically convert String
 * config file locations (e.g. "jdbc.xml") to PropertiesLoader
 * objects instead of using a String configFile property.
 */
public class PropertiesLoaderEditor extends PropertyEditorSupport {

    private final AbstractApplicationConfig _appConfig;

    public PropertiesLoaderEditor(AbstractApplicationConfig appConfig) {
        _appConfig = appConfig;
    }

    /**
     * Converts String values to PropertiesLoader.
     */
    @Override
    public void setAsText(String text) {
        if (StringUtils.isNotBlank(text)) {
            setValue(_appConfig.parseConfigTemplates(text));
        } else {
            setValue(null);
        }
    }
}
