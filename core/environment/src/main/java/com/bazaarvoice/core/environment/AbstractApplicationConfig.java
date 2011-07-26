package com.bazaarvoice.core.environment;

import com.bazaarvoice.core.environment.util.PropertiesLoader;
import com.bazaarvoice.core.util.NetworkUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.xml.DOMConfigurator;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The ApplicationConfig class is responsible for reading the configuration when the
 * process starts up.  It configures log4j and stores other configuration values
 * as necessary.  Extending this allow a particular application to add initialization
 * tasks in any order as needed.
 */
public abstract class AbstractApplicationConfig implements ServletContextAware, InitializingBean {
    protected final Log _log = LogFactory.getLog(getClass());

    /** Estimate of the time the JVM started based on when this class was loaded and initialized. */
    public static final long JVM_START_MILLIS = System.currentTimeMillis();

    public static final String HOME_CONTEXT_PARAM = "com.bazaarvoice.home-directory";

    public static final String APP_NAME_CONTEXT_PARAM = "com.bazaarvoice.application-name";

    private static final String LOG4J_XML_CONF = "log4j.xml";

    private static final String ENVIRONMENT_PROPERTIES = "env.xml";

    /**
     * Contains the path to the application's home directory.
     * ${homeDirectory}/config will contain the configuration files.
     */
    private static File _sHomeDirectoryPath;

    /**
     * Optional object to be invoked after Log4J configuration is complete.
     */
    private static Runnable _sLog4jCallback;

    /**
     * The web container ServletContext, if one exists.  This is null for command-line applications.
     */
    private ServletContext _servletContext;

    /**
     * True if the initialization should skip configuring the environment.
     */
    private boolean _skipEnvironment;

    /**
     * Environment properties available to Spring etc. for configuration.
     */
    protected Properties _environment;

    /**
     * Computes the location of the home directory containing our generic configuration files etc.
     */
    private static synchronized void findHomeDirectory(Properties environment, String applicationHomeDefault) {
        File homeDirectory = new File(environment.getProperty(HOME_CONTEXT_PARAM, applicationHomeDefault));
        if (_sHomeDirectoryPath != null && !referToSameFile(_sHomeDirectoryPath, homeDirectory)) {
            throw new UnsupportedOperationException(
                    "ApplicationConfig initialized multiple times with different home directories: " +
                            "'" + _sHomeDirectoryPath + "', '" + homeDirectory + "'");
        }
        _sHomeDirectoryPath = homeDirectory;
        System.out.println(
                "Configured home directory: " + homeDirectory +
                        (homeDirectory.isDirectory() ? "" : " (DOES NOT EXIST!)"));
    }

    /**
     * Default location of the application's config, etc. directories.
     */
    protected abstract String getApplicationHomeDefault();

    /**
     * Null-safe equality on {@link java.io.File}; true iff the two files refer to the same disk location (or both {@link java.io.File} references
     * are null.)
     */
    private static boolean referToSameFile(File first, File second) {
        String firstPath = (first == null) ? null : first.getAbsolutePath();
        String secondPath = (second == null) ? null : second.getAbsolutePath();
        return ObjectUtils.equals(first, second) || StringUtils.equals(firstPath, secondPath);
    }

    public static void setLog4jCallback(Runnable log4jCallback) {
        _sLog4jCallback = log4jCallback;
    }

    /**
     * @return The path to the application's home directory
     */
    public static File getHomeDirectoryPath() {
        return _sHomeDirectoryPath;
    }

    /**
     * Retrieves networking data:
     * <ul>
     * <li> System HostName
     * <li> System IP Address
     * </ul>
     *
     * @return A HashMap containing the previously mentioned values.
     */
    protected Map<String, String> retrieveNetwork() {
        HashMap<String, String> contextValues = new HashMap<String, String>();

        try {
            contextValues.put("hostname", InetAddress.getLocalHost().getHostName());
            contextValues.put("ipAddress", InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            // When unable to retrieve hostname and ip a default string is provided for each.
            _log.debug("UnknownHostException in ApplicationConfig.getServerContext().", e);
            contextValues.put("hostname", "Unable to retrieve Host Name");
            contextValues.put("ipAddress", "Unable to retrieve Host Address");
        }

        return contextValues;
    }

    /**
     * Returns an array of configuration directories.  The first directory is always
     * ${homeDirectory}/config.  It is followed by its immediate subdirectories sorted
     * alphabetically so 10-localhost comes before 20-overrides comes before 30-common.
     */
    public File[] getOrderedConfigDirectories() {
        File configDir = new File(_sHomeDirectoryPath, "config");

        // get all the subdirectories except ones starting with a '.' character
        File[] configSubdirs = configDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory() && !file.isHidden() && !file.getName().startsWith(".");
            }
        });
        Arrays.sort(configSubdirs);

        // build an array w/the top-level config directory first, then its sorted config/* subdirectories
        File[] configDirs = new File[configSubdirs.length + 1];
        configDirs[0] = configDir;
        System.arraycopy(configSubdirs, 0, configDirs, 1, configSubdirs.length);
        return configDirs;
    }

    /**
     * Logs name=value pairs for each entry in the map to info.  Assumes the map keys are Strings.
     */
    protected static void logMap(String title, Map<?, ?> map, Log log) {
        if (log.isInfoEnabled()) {
            log.info(title);

            // sort the map contents by key
            SortedMap<Object, Object> sorted = new TreeMap<Object, Object>(map);
            for (Map.Entry<Object, Object> entry : sorted.entrySet()) {
                String propName = (String) entry.getKey();
                String propValue = String.valueOf(entry.getValue());
                String propNameLower = propName.toLowerCase();
                if (propNameLower.contains("password") || propNameLower.contains("secret")) {
                    // obscure the password, except # of '*'s = # of letters so we have
                    // a chance of identifying password encryption/decryption bugs.
                    propValue = propValue.replaceAll(".", "*");
                }
                log.info(" " + propName + " = " + propValue);
            }
        }
    }

    /**
     * ServletContextAware interface, for webapps
     */
    @Override
    public void setServletContext(ServletContext servletContext) {
        _servletContext = servletContext;

        // log container parameters.  go to System.out since log4j usually isn't initialized yet.
        printWebAppContextInfo(servletContext);
    }

    /**
     * Called by Spring while initializing the ApplicationContext to read
     * configuration information from the System properties, the application's
     * home directory configuration files, and set up JDBC, log4j, etc.
     *
     * @throws InitializationFailedException
     */
    @Override
    public void afterPropertiesSet()
            throws InitializationFailedException {
        // subclasses may do setup work before loading begins
        initConfigurationLoader();

        // base environment properties come from the System properties
        _environment = new Properties(initSystemProperties());

        // add properties from the <context-param> tags in web.xml and from the container configuration
        if (_servletContext != null) {
            for (Enumeration e = _servletContext.getInitParameterNames(); e.hasMoreElements();) {
                String paramName = (String) e.nextElement();
                _environment.put(paramName, _servletContext.getInitParameter(paramName));
            }
        }

        // compute the locations of the directories containing our configuration files
        findHomeDirectory(_environment, getApplicationHomeDefault());

        // initialize logging before we do any more initialization
        initLog4J();

        // subclasses may add more properties to the environment, even when skipEnvironment is true.
        extendMinimalEnvironment();

        // if we don't need to initialize the standard environment variables, we're done
        if (_skipEnvironment) {
            return;
        }

        // initialize the environment properties available to Spring etc.
        initEnvironment();

        // subclasses may add more properties to the environment
        extendFullEnvironment();

        // log server context
        logMap("Network info:", retrieveNetwork(), _log);
    }

    /**
     * May be overridden by a subclass to complete any initialization work just before loading the configuration.
     */
    protected void initConfigurationLoader() {
    }

    /**
     * May be overridden by a subclass to add more properties to the environment, even when skipEnvironment is true.
     */
    protected void extendMinimalEnvironment() {
    }

    /**
     * May be overridden by a subclass to add more properties to the complete environment.
     */
    protected void extendFullEnvironment() {
    }

    /*
     * May be overridden by a subclass to provide a custom log4j xml URL.  If provided, it takes
     * precedent over other log4j xml configurations that are found.
     */
    protected URL getLog4jUrl() {
        return null;
    }

    /**
     * Combines the java system properties with the operating system environment.
     */
    private Properties initSystemProperties() {
        Properties props = new Properties();

        // copy the Java system properties into our Properties object.  ignore
        // system properties that don't have String values (value is null or another type)
        Properties systemProps = System.getProperties();
        for (Enumeration e = systemProps.propertyNames(); e.hasMoreElements();) {
            String name = (String) e.nextElement();
            String value = systemProps.getProperty(name);
            if (value != null) {
                props.put(name, value);
            }
        }

        // copy the operating system environment into our Properties object using the "env." prefix
        props.put("env.HOSTNAME", NetworkUtils.getHostName()); // for operating systems that don't set HOSTNAME
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            props.put("env." + entry.getKey(), entry.getValue());
        }

        // log the basic Java and OS properties
        if (_log.isInfoEnabled()) {
            //noinspection unchecked
            List<String> keys = (List<String>) Collections.list(props.propertyNames());
            Collections.sort(keys);
            for (String key : keys) {
                String value = props.getProperty(key);
                if (key.startsWith("java.") || key.startsWith("os.")) {
                    _log.info("System property \"" + key + "\" = " + value);
                } else if (key.startsWith("env.") && !"env.PATH".equalsIgnoreCase(key)) {
                    _log.info("Operating system property \"" + key + "\" = " + value);
                }
            }
        }

        return props;
    }

    /**
     * Initialize the log4j configuration from the file in ${homeDirectory}/config/log4j.xml.
     * If we don't find it there, we'll also look through the immediate subdirectories of
     * ${homeDirectory}/config.  Optionally, subclasses can provide a custom log4j config Url
     * that will be checked first.
     */
    private void initLog4J() {
        URL customLog4jUrl = getLog4jUrl();
        if (customLog4jUrl != null) {
            DOMConfigurator.configure(customLog4jUrl);            
        } else {
            initLog4jFromConfigDirectories();
        }

        if (_sLog4jCallback != null) {
            _sLog4jCallback.run();
        }
    }

    private void initLog4jFromConfigDirectories() {
        File log4jFile = null;
        File[] configDirs = getOrderedConfigDirectories();
        for (File configDir : configDirs) {
            File file = new File(configDir, LOG4J_XML_CONF);
            if (file.exists()) {
                log4jFile = file;
                break;
            }
        }
        if (log4jFile == null) {
            throw new InitializationFailedException("Unable to find " + LOG4J_XML_CONF +
                    " in the configuration directories: " + Arrays.asList(configDirs));
        }
        DOMConfigurator.configure(log4jFile.getPath());
    }

    /**
     * Initializes the set of top-level application configuration variables used
     * throughout the app.  These variables are available in the Spring
     * applicationContext*.xml files using variable placeholders (ie. ${prop-name}).
     */
    private void initEnvironment() {
        // compute the ${applicationName} property
        _environment.put("applicationName", findApplicationName());

        // load the config files from ${homeDirectory}/config/*/env.xml
        loadConfigFiles(ENVIRONMENT_PROPERTIES, _environment);

        // log the environment properties.  note this won't log Java system or
        // OS environment properties because of the way Properties inheritance is setup.
        // (this is a good thing--too much noise in the log files, otherwise)
        logMap("Environment properties:", _environment, _log);
    }

    /**
     * Computes the ${applicationName} environment property.
     */
    private String findApplicationName() {
        String applicationName;
        if (_servletContext != null) {
            // for webapps use the <display-name> tag from web.xml as the application name
            applicationName = _servletContext.getServletContextName();
        } else {
            // command-line applications should have passed the app name to the init() method
            applicationName = System.getProperty(APP_NAME_CONTEXT_PARAM);
        }
        if (StringUtils.isEmpty(applicationName)) {
            throw new InitializationFailedException("Missing or empty application name.");
        }
        return applicationName;
    }

    /**
     * Initializes parameters from the specified configuration files and adds
     * them to the environment Properties so they're available in the Spring
     * configuration files (ie. ${jdbc.displaydb.url}).
     */
    protected void extendEnvironment(String fileName, String keyPrefix) {
        Properties properties = new Properties(_environment);
        loadConfigFiles(fileName, properties);
        extendEnvironment(keyPrefix, properties);
    }

    protected void extendEnvironment(String keyPrefix, Properties properties) {
        // copy the properties that begin with the key prefix+"." (ie. "jdbc.")
        // into the Properties available to Spring (copying everything doesn't
        // cause problems, but copying only a subset seems cleaner)
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String key = (String) entry.getKey();
            if (key.startsWith(keyPrefix + ".")) {
                _environment.put(key, entry.getValue());
            }
        }

        // Write the properties to the log
        logMap(keyPrefix + " properties :", properties, _log);
    }

    public void setSkipEnvironment(boolean skipEnvironment) {
        _skipEnvironment = skipEnvironment;
    }

    /**
     * @return the set of properties from the application's configuration files
     */
    public Properties getEnvironment() {
        return _environment;
    }

    /**
     * Loads a set of properties from the specified configuration file.
     * <p/>
     * We look for the configuration file in several locations under
     * ${homeDirectory}/config.  For example, in a standard developer
     * setup, this method will combine files in the following places for
     * the config file "jdbc.xml" (.properties files will be loaded only
     * if the corresponding .xml file with the same name does not exist):
     * <ul>
     * <li> ${homeDirectory}/config/jdbc.xml
     * <li> ${homeDirectory}/config/jdbc.properties
     * <li> ${homeDirectory}/config/10-localhost/jdbc.xml
     * <li> ${homeDirectory}/config/10-localhost/jdbc.properties
     * <li> ${homeDirectory}/config/20-overrides-dev/jdbc.xml
     * <li> ${homeDirectory}/config/20-overrides-dev/jdbc.properties
     * <li> ${homeDirectory}/config/30-common/jdbc.xml
     * <li> ${homeDirectory}/config/30-common/jdbc.properties
     * </ul>
     *
     * @param fileName the name of the configuration file.
     * @param props    the set of properties to update with configuration settings.
     */
    public void loadConfigFiles(String fileName, Properties props) {
        parseConfigTemplates(fileName).load(props);
    }

    /**
     * Locates and parses a set of properties files.
     */
    public PropertiesLoader parseConfigTemplates(String fileName) {
        List<PropertiesLoader> loaders = new ArrayList<PropertiesLoader>();

        boolean found = false;

        try {
            // first, look in ${homeDirectory}/config.  then, look in all the immediate
            // subdirectories of ${homeDirectory}/config sorted alphabetically.
            File[] configDirs = getOrderedConfigDirectories();
            for (File configDir : configDirs) {
                if (loadTemplate(new File(configDir, fileName), loaders)) {
                    found = true;
                }
            }

            if (!found) {
                throw new IOException("Unable to find " + fileName + " in the configuration directories: " + Arrays.asList(configDirs));
            }

        } catch (Exception e) {
            String message = "Unable to read configuration file " + fileName + ".  Application cannot be initialized.";
            System.out.println(message);
            _log.fatal(message, e);
            throw new InitializationFailedException(message, e);
        }

        return PropertiesLoader.concat(loaders);
    }

    /**
     * Creates a PropertiesLoader object from a .xml or .properties file.
     */
    private boolean loadTemplate(File file, List<PropertiesLoader> loaders)
            throws IOException, XMLStreamException {
        // normally we prefer .xml config files, but if the .xml variant
        // doesn't exist and the .properties one does, use the .properties
        if (!file.exists() && file.getName().endsWith(".xml")) {
            file = new File(file.getParent(), StringUtils.removeEnd(file.getName(), ".xml") + ".properties");
        }

        // if the file doesn't exist, return false
        if (!file.exists()) {
            return false; // file was not found
        }

        _log.info("Loading properties from " + file);
        loaders.add(PropertiesLoader.parse(file));
        return true;  // file was found
    }

    /**
     * Logs attributes and parameters from ServletContext provided by the web container.
     * Prints to System.out since log4j isn't initialized at the time this is called.
     */
    @SuppressWarnings ({"unchecked"})
    private void printWebAppContextInfo(ServletContext context) {
        System.out.println();
        System.out.println("***************************************************************************");
        System.out.println("Initializing " + context.getServletContextName() + "...");
        System.out.println("Context info: " + context);
        System.out.println("Server info:  " + context.getServerInfo());

        System.out.println("Attributes from ServletContext:");
        List<String> attrNames = Collections.list(context.getAttributeNames());
        for (String attrName : new TreeSet<String>(attrNames)) {
            System.out.println(" " + attrName + " = " + context.getAttribute(attrName));
        }

        System.out.println("Parameters from ServletContext:");
        List<String> paramNames = Collections.list(context.getInitParameterNames());
        for (String paramName : new TreeSet<String>(paramNames)) {
            System.out.println(" " + paramName + " = " + context.getInitParameter(paramName));
        }
    }
}
