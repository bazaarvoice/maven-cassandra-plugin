package com.bazaarvoice.core.util;

import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Simple class for encoding a nested structure of strings, numbers, booleans, arrays,
 * collections and maps as JSON strings.
 */
public abstract class JsonFormatter {

    public interface ToJsonObject {
        /**
         * Returns the current object represented as primitives or arrays or collections
         * or maps etc. that can be serialized by this JsonFormatter.
         */
        Object toJsonObject();
    }

    public interface ToJsonString {
        /**
         * Returns the current object as a JSON string.  This MUST return valid JSON!
         * To avoid the responsibility of creating valid JSON, it's better to use {@link ToJsonObject}.
         */
        String toJsonString();
    }

    /**
     * Returns a JSON representation of the given object.
     */
    public static String toJson(Object obj) {
        return toJson(obj, false);
    }

    /**
     * Fast path variation of {@link #toJson(Object)} for a String object.
     */
    public static String toJson(String string) {
        return string != null ? JSONObject.quote(string) : "null";
    }

    /**
     * Pass true for insertPeriodicNewlines to insert line breaks to increase readability.
     */
    public static String toJson(Object obj, boolean insertPeriodicNewlines) {
        StringBuilder buf = new StringBuilder();
        append(buf, obj, insertPeriodicNewlines);
        return buf.toString();
    }

    /**
     * Appens a JSON representation of the given object to the specified buffer.
     */
    public static void append(StringBuilder buf, Object obj) {
        append(buf, obj, false);
    }

    /**
     * Pass true for insertPeriodicNewlines to insert line breaks to increase readability.
     */
    public static void append(StringBuilder buf, Object obj, boolean insertPeriodicNewlines) {
        try {
            append(buf, obj, new LineBreak(insertPeriodicNewlines ? buf : null));
        } catch (IOException e) {
            throw new RuntimeException(e); // StringBuilder shouldn't throw IOException
        }
    }

    /**
     * Appendable interface can be used with a Writer.
     */
    public static void append(Appendable buf, Object obj) throws IOException {
        append(buf, obj, new LineBreak(buf));
    }

    private static void append(Appendable buf, Object obj, LineBreak lineBreak) throws IOException {
        while (obj instanceof ToJsonObject) {
            obj = ((ToJsonObject) obj).toJsonObject();
        }
        if (obj == null) {
            buf.append("null");

        } else if (obj instanceof Boolean || obj instanceof Number) {
            buf.append(obj.toString());

        } else if (obj.getClass().isArray()){
            buf.append('[');
            int len = Array.getLength(obj);
            for (int i = 0; i < len; i++) {
                if (i > 0) {
                    buf.append(',').append(lineBreak.next());
                }
                append(buf, Array.get(obj, i), lineBreak);  // works w/both Object[]s and primitive[]s
            }
            buf.append(']');

        } else if (obj instanceof Iterable){
            buf.append('[');
            for (Iterator it = ((Iterable) obj).iterator(); it.hasNext();) {
                append(buf, it.next(), lineBreak);
                if (it.hasNext()) {
                    buf.append(',').append(lineBreak.next());
                }
            }
            buf.append(']');

        } else if (obj instanceof Map) {
            buf.append('{');
            @SuppressWarnings({"unchecked"})
            Set<Map.Entry> entrySet = ((Map) obj).entrySet();
            for (Iterator<Map.Entry> it = entrySet.iterator(); it.hasNext();) {
                Map.Entry entry = it.next();
                append(buf, entry.getKey(), lineBreak);
                buf.append(':');
                append(buf, entry.getValue(), lineBreak);
                if (it.hasNext()) {
                    buf.append(',').append(lineBreak.next());
                }
            }
            buf.append('}');

        } else if (obj instanceof ToJsonString) {
            buf.append(((ToJsonString) obj).toJsonString());

        } else {
            // note: the json spec (http://json.org/) specifies that a json string is Java-style, not JavaScript-style.
            // In particular, single quotes aren't allowed as delimiters and a single quote can't be escaped using backslash.
            // Facebook actually rejects strings that contain an escaped single quote such as "I\'ve".  So use the
            // JSONObject quote method from json.org.
            buf.append(JSONObject.quote(obj.toString()));
        }
    }

    /** Adds line breaks about every 80 chars to improve readability without adding much to the overall size. */
    private static class LineBreak {
        private final CharSequence _buf;
        private int _lastLineBreak;

        private LineBreak(Appendable buf) {
            _buf = (buf instanceof CharSequence) ? (CharSequence) buf : null;
        }

        public String next() {
            if (_buf != null) {
                int pos = _buf.length();
                if (pos - _lastLineBreak > 80) {
                    _lastLineBreak = pos;
                    return "\n";
                }
            }
            return "";
        }
    }
}

