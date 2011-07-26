package com.bazaarvoice.cca.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Very simple template that can produce rendered versions of itself.
 * Templates are simple strings with substitutable variables, for example:
 * <pre>
 *   This {variable} is substitutable.
 * </pre>
 */
public final class SimpleTemplateUtil {

    private static final Pattern VARS_PATTERN = Pattern.compile("\\{([\\w]+)\\}");

    /**
     * Interface defining String to String lookup capability.
     */
    public interface Lookup {

        String valueFor(String name);

    }

    public static String renderTemplate(String template, Lookup lookup) {
        StringBuffer rendered = new StringBuffer();
        Matcher m = VARS_PATTERN.matcher(template);
        while (m.find()) {
            String value = lookup.valueFor(m.group(1));
            if (value != null) {
                m.appendReplacement(rendered, Matcher.quoteReplacement(value));
            }
        }
        m.appendTail(rendered);
        return rendered.toString();
    }
}
