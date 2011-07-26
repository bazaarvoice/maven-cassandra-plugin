package com.bazaarvoice.prr.util;

import com.bazaarvoice.core.util.BVStringUtils;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Ivan Luzyanin
 */
public class TextProcessUtils {

    /**
     * US-ASCII definition of whitespace.  Ignores extended Unicode whitespace chars such as non-breaking space.
     */
    private static final Pattern WHITESPACE_SEQUENCE = Pattern.compile("\\p{Space}+");

    /**
     * Normalizes the whitespace within a single line of user input text.  Trims
     * leading and trailing whitespace and converts all sequences of adjacent
     * whitespace (spaces, tabs, newlines, etc) into a single space character.
     */
    public static String normalizeInputText(String value) {
        if (value == null) {
            return null;
        }

        // remove control characters & other chars that aren't valid Unicode
        value = BVStringUtils.stripIllegalCharacters(value);

        return WHITESPACE_SEQUENCE.matcher(value.trim()).replaceAll(" ");
    }

    /**
     * Removes the redundant symbols from user input text.
     *
     * @param value user input text for stripping.
     * @return Returns the stripped user input text from trailing and leading whitespaces.
     *         Also corrects the spaces between lines of review text.
     */
    public static String normalizeMultiLineInputText(String value) {
        if (value == null) {
            return null;
        }

        // remove control characters & other chars that aren't valid Unicode
        value = BVStringUtils.stripIllegalCharacters(value);

        // trim leading and trailing whitespace
        value = value.trim();

        List<String> lines = new ArrayList<String>();
        try {
            BufferedReader lineReader = new BufferedReader(new StringReader(value));
            String line;
            int emptyLineCounter = 0;
            while ((line = lineReader.readLine()) != null) {
                line = normalizeInputText(line);
                if (StringUtils.isEmpty(line)) {
                    emptyLineCounter++;
                    if (emptyLineCounter <= 1) {
                        //Add the first empty line, but not subsequent empty lines
                        lines.add("");
                    }
                } else {
                    emptyLineCounter = 0;
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);  // IOException should never happen with a StringReader
        }
        return StringUtils.join(lines.iterator(), '\n');
    }

    /**
     * @param s      String to verify
     * @param maxLen Maximum length
     * @return true if the given string needs to be abbrevated, false otherwise
     */
    public static boolean needsAbbreviation(String s, int maxLen) {
        return s != null && s.length() > maxLen;
    }

    /**
     * Abbrevates the string if the string needs to be abbrevated
     *
     * @param s      String to abbrevate
     * @param maxLen maximum string length
     * @return Abbrevated string
     */
    public static String abbreviate(String s, int maxLen) {
        return abbreviate(s, maxLen, "...");
    }

    /**
     * Abbrevates the string if the string needs to be abbrevated
     *
     * @param s              String to abbrevate
     * @param maxLen         maximum string length
     * @param suffixToAppend the text to append to the truncated string, e.g. "..."
     * @return Abbrevated string
     */
    public static String abbreviate(String s, int maxLen, String suffixToAppend) {
        if (!needsAbbreviation(s, maxLen)) {
            //Just return the string without abbrevating it
            return s;
        }
        int lastSpace = s.lastIndexOf(' ', maxLen);
        if ((lastSpace == -1)) {
            //No whitespace were found. Just cut first maxLen characters and return the result
            return s.substring(0, maxLen);
        } else {
            //Make abbrevation
            return s.substring(0, lastSpace) + suffixToAppend;
        }
    }
}
