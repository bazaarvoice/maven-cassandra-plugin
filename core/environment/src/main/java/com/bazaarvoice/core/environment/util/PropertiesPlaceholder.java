package com.bazaarvoice.core.environment.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Expands a string of the format "abc${def}ghi" where 'def' is a key in a Properties object.
 */
public class PropertiesPlaceholder {

    // interleaved strings & variable references.  _strings.length == _variables.length + 1
    private final String[] _strings;
    private final String[] _variables;

    public PropertiesPlaceholder(String template) {
        List<String> strings = new ArrayList<String>();
        List<String> variables = new ArrayList<String>();

        StringBuilder buf = new StringBuilder();
        int[] pos = new int[1];
        while (pos[0] < template.length()) {
            // append possibly-escaped characters up to the next '$', moving pos[0] to point to the '$'
            if (!appendUntil(template, pos, '$', buf)) {
                // if we're at the end-of-the-line, then we're done
                break;
            }
            // we're guaranteed that template.charAt(pos[0]) == '$'.
            pos[0]++;  // consume the '$'
            // if the next char is a '{' then we're at a "${" which is the beginning of a variable expansion
            if (pos[0] < template.length() && template.charAt(pos[0]) == '{') {
                pos[0]++;    // consume the '{'
                // parse characters up to the next '}', moving pos[0] to point to the '}'
                StringBuilder key = new StringBuilder();
                if (!appendUntil(template, pos, '}', key)) {
                    throw new IllegalArgumentException("Unclosed variable expansion after '${', expected '}': " + template);
                }
                // we're guaranteed that template.charAt(pos[0]) == '}'.
                pos[0]++;    // consume the '}'

                strings.add(buf.toString());
                variables.add(key.toString());
                buf.setLength(0);
            } else {
                // '$' isn't followed by a '{'.  we already consumed the '$' so append it and loop
                buf.append('$');
            }
        }
        strings.add(buf.toString());

        _strings = strings.toArray(new String[strings.size()]);
        _variables = variables.toArray(new String[variables.size()]);
    }

    /** Appends characters from position pos[0] until eoln or encountering delim, unescaping along the way. */
    private static boolean appendUntil(String string, int[] pos, char delim, StringBuilder buf) {
        for (; pos[0] < string.length(); pos[0]++) {
            char ch = string.charAt(pos[0]);
            if (ch == delim) {
                return true;  // found the delimiter
            }
            if (ch == '\\') {
                pos[0]++;  // consume the backslash
                if (pos[0] == string.length()) {
                    throw new IllegalArgumentException("Attribute value with trailing unescaped backslash: " + string);
                }
                ch = string.charAt(pos[0]);
                switch(ch) {
                    case 't': ch = '\t'; break;
                    case 'r': ch = '\r'; break;
                    case 'n': ch = '\n'; break;
                    case 'f': ch = '\f'; break;
                }
            }
            buf.append(ch);
        }
        return false;
    }

    /** Expands the string template using the specified property definitions. */
    public String expand(Properties props) {
        if (_variables.length == 0) {
            return _strings[0];
        }

        StringBuilder buf = new StringBuilder();
        int i = 0;
        for (; i < _variables.length; i++) {
            buf.append(_strings[i]);
            buf.append(props.getProperty(_variables[i], ""));
        }
        buf.append(_strings[i]);
        return buf.toString();
    }
}
