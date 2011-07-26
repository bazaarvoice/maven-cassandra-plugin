package com.bazaarvoice.core.util;

import org.apache.commons.lang.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test
public class JsonFormatterTest1 {

    public void testStringEscape() {
        // validate that string escaping matches the spec at http://json.org/
        // this fixes a bug where we were generating JSON for Facebook that escaped apostrophe chars (') which doesn't match the JSON spec. 

        StringBuilder buf = new StringBuilder();
        StringBuilder expectedBuf = new StringBuilder();
        expectedBuf.append('"');  // spec says strings are delimited by double quotes, not apostrophes
        buf.append("</");
        expectedBuf.append("<\\/"); // special case for "</" so "</script>" is escaped as "<\/script>" 
        for (char ch = 0; ch < 2048; ch++) {
            buf.append(ch);

            switch(ch) {
                // these cases match exactly the list of escapes supported by the "string" production at http://json.org/ except for '/' which is only escaped when it appears like '</'
                case '\"':
                case '\\':
                    expectedBuf.append('\\').append(ch);
                    break;
                case '\b':
                    expectedBuf.append("\\b");
                    break;
                case '\f':
                    expectedBuf.append("\\f");
                    break;
                case '\n':
                    expectedBuf.append("\\n");
                    break;
                case '\r':
                    expectedBuf.append("\\r");
                    break;
                case '\t':
                    expectedBuf.append("\\t");
                    break;
                default:
                    // otherwise, we have a choice of unicode char or \u1234-style unicode escape.  JSON.org escapes unicode control characters
                    if (ch < ' ' || (ch >= '\u0080' && ch < '\u00a0') ||
                                    (ch >= '\u2000' && ch < '\u2100')) {
                        expectedBuf.append("\\u");
                        expectedBuf.append(StringUtils.leftPad(Integer.toHexString(ch), 4, '0'));
                    } else {
                        expectedBuf.append(ch);
                    }
                    break;
            }
        }
        expectedBuf.append('"');  // spec says strings are delimited by double quotes, not apostrophes
        String expected = expectedBuf.toString().toLowerCase();  // json spec doesn't specify upper or lower case

        Assert.assertEquals(JsonFormatter.toJson(buf).toLowerCase(), expected);  // toJson(Object)
        Assert.assertEquals(JsonFormatter.toJson(buf.toString()).toLowerCase(), expected);  // toJson(String)
        Assert.assertEquals(JsonFormatter.toJson((Object) null), JsonFormatter.toJson((String) null));  // toJson(String)

        // test that ToJsonString allows bypassing the JsonFormatter formatting
        final String json = "{\"<script>\": \"<\\/script>\"}";
        Assert.assertEquals(JsonFormatter.toJson(new JsonFormatter.ToJsonString() {
            public String toJsonString() {
                return json;
            }
        }), json);

        // test that ToJsonObject correctly treats a string as a JavaScript string that needs to be quoted and escaped
        Assert.assertEquals(JsonFormatter.toJson(new JsonFormatter.ToJsonObject() {
            public Object toJsonObject() {
                return json;
            }
        }), "\"{\\\"<script>\\\": \\\"<\\\\/script>\\\"}\"");
    }
}
