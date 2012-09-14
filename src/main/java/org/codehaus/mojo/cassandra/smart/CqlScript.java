package org.codehaus.mojo.cassandra.smart;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileReader;
import java.util.Map;

public class CqlScript {

    private String skipIfKeyspaceIsPresent;

    /** The CQL script which will be executed. */
    private File script;

    /** Properties to substitute in the configured script. */
    private Map<String, String> filteringProperties;

    public boolean hasSkipIfKeyspaceIsPresent() {
        return !StringUtils.isEmpty(skipIfKeyspaceIsPresent);
    }

    public String getSkipIfKeyspaceIsPresent() {
        return skipIfKeyspaceIsPresent;
    }

    public File getScript() {
        return script;
    }

    public Map<String, String> getFilteringProperties() {
        return filteringProperties;
    }

    public String toCqlString() throws MojoExecutionException {
        FileReader fr = null;
        try
        {
            fr = new FileReader(script);
            return substituteFilteringProperties(IOUtil.toString(fr));
        } catch (Exception e)
        {
            throw new MojoExecutionException("couldn't load " + script.getPath(), e);
        } finally
        {
            IOUtil.close(fr);
        }
    }

    private String substituteFilteringProperties(String cql) {
        String substituted = cql;
        if (filteringProperties != null) {
            for(String key : filteringProperties.keySet()) {
                substituted = substituted.replace("${" + key + "}", filteringProperties.get(key));
            }
        }
        return substituted;
    }

}
