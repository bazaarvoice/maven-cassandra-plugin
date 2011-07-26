package com.bazaarvoice.core.context.util;

import com.bazaarvoice.core.environment.AbstractApplicationConfig;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.util.ArrayList;

/**
 * Utilities for initializing an ApplicationContext, especially from a command-line environment.
 */
public abstract class ApplicationContextUtils {

    public static ConfigurableApplicationContext init(String applicationName, String homeDirectory, String[] springConfigFiles) {
        return init(applicationName, homeDirectory, (File) null, springConfigFiles);
    }

    public static ConfigurableApplicationContext init(String applicationName, String homeDirectory, File springFilesLocation, String[] springConfigFiles) {
        Resource[] springResources = null;
        if (springFilesLocation != null) {
            springResources = toFileSystemResources(springFilesLocation, springConfigFiles);
        }
        return init(applicationName, homeDirectory, springResources, springConfigFiles);
    }

    /**
     * Initializes the Spring ApplicationContext object for command-line applications.
     * <p/>
     * One of the files in the ApplicationContext instantiates a subclass of ApplicationConfig, which
     * reads configuration information from the System properties, the application home directory
     * configuration files, and sets up JDBC, log4j, etc.
     * <p/>
     * Web applications should not call this method.  They should use the Spring
     * ContextLoaderListener in their 'web.xml' instead.
     *
     * @param applicationName The name of the application, for logging and configuration.
     * @param homeDirectory   The application home directory, or null to use default values.
     * @param springResources The locations of the applicationContext*.xml files.  Usually in
     *  production this is null to mean search the classpath, but in tests it points to Spring
     *  files in the "src/web/&lt;web-app>/WEB-INF" directory.
     */
    public static ConfigurableApplicationContext init(String applicationName, String homeDirectory, final Resource[] springResources, final String[] springConfigFiles) {
        initCmdLineEnv(applicationName, homeDirectory);

        // Initialize the Spring application context.  This should create a ApplicationConfig
        // bean which finishes the rest of the initialization process during afterPropertiesSet().
        ConfigurableApplicationContext applicationContext = new AbstractXmlApplicationContext() {
            @Override
            protected Resource[] getConfigResources() {
                return (springResources != null) ? springResources : toClassPathResources(springConfigFiles);
            }
        };
        applicationContext.refresh();
        return applicationContext;
    }

    public static void initCmdLineEnv(String applicationName, String homeDirectory) {
        // if a home directory isn't specified, pull it from the System properties
        if (StringUtils.isEmpty(homeDirectory)) {
            homeDirectory = System.getProperty("bazaarvoice.home");
        }
        if (StringUtils.isEmpty(homeDirectory)) {
            homeDirectory = System.getenv("BAZAARVOICE_HOME");
        }

        // push values into the System properties so Spring initialization can find them.  (hack...)
        if (applicationName != null) {
            System.getProperties().put(AbstractApplicationConfig.APP_NAME_CONTEXT_PARAM, applicationName);
        }
        if (homeDirectory != null) {
            System.getProperties().put(AbstractApplicationConfig.HOME_CONTEXT_PARAM, homeDirectory);
        }
    }

    /**
     * Returns Spring Resource objects for file names relative to the classpath.
     */
    private static Resource[] toClassPathResources(String[] fileNames) {
        ArrayList<Resource> resources = new ArrayList<Resource>();
        for (String fileName : fileNames) {
            ClassPathResource resource = new ClassPathResource(fileName);
            if (resource.exists()) {
                resources.add(resource);
            }
        }
        return resources.toArray(new Resource[resources.size()]);
    }

    /**
     * Returns Spring Resource objects for file names relative to the specified directory.
     */
    private static Resource[] toFileSystemResources(File springFilesLocation, String[] fileNames) {
        ArrayList<Resource> resources = new ArrayList<Resource>();
        for (String fileName : fileNames) {
            FileSystemResource resource = new FileSystemResource(new File(springFilesLocation, fileName));
            if (resource.exists()) {
                resources.add(resource);
            }
        }
        return resources.toArray(new Resource[resources.size()]);
    }
}
