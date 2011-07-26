package com.bazaarvoice.core.util;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.UrlValidator;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * String utility methods
 */
public abstract class BVStringUtils {

    private static final String LSQR = "\\[";
    private static final String RSQR = "\\]";
    private static final String LCURLY = "\\{";
    private static final String RCURLY = "\\}";
    // Defines comment pattern like " [any text] ". Also we should support comments with mistakes
    // like "{0 [comment} literal text {1}" that should throw an exception rather then to be ignored.
    private static final String COMMENT = "\\s*" + LSQR + "[^" + RSQR + RCURLY + "]*" + RSQR + "\\s*";
    // Defines result regular expression that match patters like "{0 [any text] ,choice,...}"
    private static final String COMMENTED_TOKEN_REGEX = LCURLY + "(\\d+)" + COMMENT + "([^" + RCURLY + "]*)" + RCURLY;

    private static final Pattern COMMENTED_TOKEN_PATTERN = Pattern.compile(COMMENTED_TOKEN_REGEX);

    public static final Pattern SPACES = Pattern.compile(" +");

    public static final Pattern EOLN = Pattern.compile("\r?\n");

    private static final Pattern HTML_LINEBREAK_PATTERN = Pattern.compile("<br/?>|</?li>", Pattern.CASE_INSENSITIVE);

    // the same as "[0-9A-Za-z_]"
    private static final Pattern NON_ALPHA_NUMERIC_PATTERN = Pattern.compile("\\W");

    /**
     * Attribute name is either \w or '-' characters, then there could be space(s), then '=', then spaces again,
     * then "(') character or without it (could be once or not at all # character), then any characters except "(') - . : characters or \w characters,
     * and finally "(') character to close attribute value or without it
     */
    private static final String HTML_ATTRIBUTE_PATTERN_STRING = "([\\w\\-]+)\\s*=\\s*(\"[^\"]*\"|\'[^\']*\'|(#?)[-a-zA-Z0-9._:]+)";

    /**
     * Comment pattern string is &lt followed by "!-- " followed by any character or white space combination and 
	 finally "--" followed by &gt to close. Note that this is different from the form of comments in the HTML 4.01 
	 spec. The spec indicates that there can be a white space between the -- and the &gt at the close. *However*
	 none of the major browsers (Firefox, IE, Opera, Safari) follow this behavior. They all require a --&gt (no white
	 space) to close a comment. We are following the defacto standard implemented by these browsers.
     */
    private static final Pattern HTML_COMMENT_PATTERN = Pattern.compile("<!--(.)*?-->", Pattern.DOTALL);

    /**
     * HTML tag starts with '&lt;' character, then there could be a space(s), then '/' character (for closing tag),
     * then attribute name (either \w or '-' characters), then any amount of attributes, then there could be a space(s),
     * then could be a '/' character if the tag is an empty tag (&lt;div/&gt;, for example), then could be a spaces and
     * finally the character '>' which closes the tag
     */
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile(
            "<\\s*/?\\s*[\\w\\-]+(\\s+" + HTML_ATTRIBUTE_PATTERN_STRING + ")*\\s*/?\\s*>");

    /**
     * Wrapper around String.getBytes() that enforces use of UTF-8 encoding.
     */
    public static byte[] getBytesUTF8(String string) {
        try {
            return string.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Cannot convert to bytes using UTF-8", e);  // UTF-8 should always be supported
        }
    }

    /**
     * Wrapper around new String(byte[]) that enforces use of UTF-8 encoding.
     */
    public static String getStringUTF8(byte[] bytes) {
        try {
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Cannot convert from bytes using UTF-8", e);  // UTF-8 should always be supported
        }
    }

    /**
     * Escapes a Javascript-style string.  Note that this does NOT create valid JSON according to the JSON
     * specification at http://json.org.  Valid JSON does not allow single quotes to delimit string values and
     * it does not allow backslash-escaped single quotes.  Use the JsonFormatter class to produce valid JSON.
     */
    public static String escapeJavaScript(String string) {
        // There are several choices of library routines for escaping Javascript.
        // Apache Commons Lang, FreeMarker and Spring all have methods:
        //  - org.apache.commons.lang.StringEscapeUtils.escapeJavaScript()
        //  - freemarker.template.utility.StringUtil.javaScriptStringEnc()
        //  - org.springframework.web.util.JavaScriptUtils.javaScriptEscape()
        //
        // Pick the Apache Commons Lang implementation for two reasons:
        //  (a) It escapes the '/' character so that literals like "</script>" will be
        //      escaped to "<\/script>" so browsers won't interpret it as the end of a
        //      script tag.  (All three libraries do this in one way or another.)
        //      Note that commons-lang-2.3 doesn't escape '/'--it was added in 2.4.
        //  (b) It escapes all non-ascii characters >= 128 using "\u1234" syntax.  This
        //      helps ensure that Unicode characters are always interpreted correctly.
        //      (Of the three libraries, only apache-commons does this.  Tapestry's
        //      DatePicker also does this, but its Javascript escape method is private.)
        return StringEscapeUtils.escapeJavaScript(string);
    }

    // used by Tapestry
    @SuppressWarnings( {"UnusedDeclaration"})
    public static String getJsStringLiteralArray(List<String> strings) {
        return JsonFormatter.toJson(strings);
    }

    public static boolean needsAbbreviation(String s, int maxLen) {
        return s != null && s.length() > maxLen;
    }

    /**
     * Abbreviates the given string to at most maxLen characters, then appends "..."
     *
     * This methods cuts the string off at a word break near maxLen if possible.
     * {@link org.apache.commons.lang.StringUtils#abbreviate} can be used if you need exact abbreviation.
     */
    public static String abbreviate(String s, int maxLen) {
        return abbreviate(s, maxLen, "...");
    }

    /**
     * Abbreviates the given string to at most maxLen characters, then appends the given dotdotdot string
     *
     * This method cuts off at a word break near maxLen if possible
     */
    public static String abbreviate(String s, int maxLen, String dotdotdot) {
        if (!needsAbbreviation(s, maxLen)) {
            return s;
        }
        int lastSpace = s.lastIndexOf(' ', maxLen);
        if (lastSpace == -1 || lastSpace < (maxLen / 2)) {
            lastSpace = maxLen;  // no good point to abbreviate
        }
        return s.substring(0, lastSpace).trim() + dotdotdot;
    }

    /**
     * Remove regular expression meta character from a string
     * Characters to be removed are *, ^, $ and \
     */
    public static String removeMySQLRegexpCharacters(String str) {
        return str.replaceAll("[\\$\\^\\*\\\\]", " ");
    }

    /**
     * Replaces % with \% and _ with \_
     */
    public static String replaceMySQLLikeWildcards(String str) {
        return str.replace("%", "\\%").replace("_", "\\_");
    }

    /**
     * Converts a user-friendly pattern of literal characters and '*' wildcard
     * characters to a mysql-ready regular expression string.  There are 5 special cases:
     * <ol>
     * <li> '*' matches anything.
     * <li> space or tab match each other, one or more times.  For example, ' ' will match '\t\t\t'.
     * <li> '^' as the first char matches the beginning of the line if supportCaret is true.
     * <li> '$' as the last char matches the end of the line if supportDollar is true.
     * <li> '\' escapes the following character, so you can match '*', '$' etc. literally.
     * </ol>
     */
    public static String userPatternToMysqlRegexp(String userPattern, boolean wholeWord, boolean supportCaret, boolean supportDollar) {
        return userPatternToSpecificRegexp(PatternType.MYSQL, userPattern, wholeWord, supportCaret, supportDollar);
    }

    /**
     * Converts a user-friendly pattern of literal characters and '*' wildcard
     * characters to a java-ready regular expression string.  There are 5 special cases:
     * <ol>
     * <li> '*' matches anything.
     * <li> space or tab match each other, one or more times.  For example, ' ' will match '\t\t\t'.
     * <li> '^' as the first char matches the beginning of the line if supportCaret is true.
     * <li> '$' as the last char matches the end of the line if supportDollar is true.
     * <li> '\' escapes the following character, so you can match '*', '$' etc. literally.
     * </ol>
     */
    public static String userPatternToJavaRegexpString(String userPattern, boolean wholeWord, boolean supportCaret, boolean supportDollar) {
        return userPatternToSpecificRegexp(PatternType.JAVA, userPattern, wholeWord, supportCaret, supportDollar);
    }

    /**
     * Returns true if we are able to find any of the user entered patterns in any of the text passed in.
     *
     * @param multilinePatterns multiple lines of user-entered patterns (e.g., '*test*', 'awesome product$')
     * @param textToCheck       the user-entered text to check (i.e., review text, title, etc)
     * @return true if any one of the patterns matches any one of the <code>textToCheck</code> elements
     * @see #findAnyPatternInText(java.util.Collection, java.util.Collection)
     */
    public static boolean findAnyPatternInText(String multilinePatterns, Collection<String> textToCheck) {
        if (StringUtils.isEmpty(multilinePatterns)) {
            return false;
        }

        if (CollectionUtils.isEmpty(textToCheck)) {
            return false;
        }
        
        return findAnyPatternInText(Collections.singleton(multilinePatterns), textToCheck);
    }

    /**
     * Returns true if we are able to find any of the user entered patterns in any of the text passed in.
     *
     * @param multilinePatterns a collection of multiple lines (each) of user-entered patterns (e.g., '*test*', etc)
     * @param textToCheck       the user-entered text to check (i.e., review text, title, etc)
     * @return true if any one of the patterns matches any one of the <code>textToCheck</code> elements
     * @see #findAnyPatternInText(String, java.util.Collection)
     */
    public static boolean findAnyPatternInText(Collection<String> multilinePatterns, Collection<String> textToCheck) {
        if (!CollectionUtils.isEmpty(textToCheck)) {

            // build a set of distinct patterns to find
            Set<String> patternsToFind = new LinkedHashSet<String>();
            for (String multilinePattern : multilinePatterns) {
                // split the multi-line pattern on eol and add each to the set of distinct patterns
                patternsToFind.addAll(Arrays.asList(EOLN.split(multilinePattern)));
            }

            // check too see if any of the patterns are found in any of the entered text
            for (String userPattern : patternsToFind) {
                Pattern pattern = Pattern.compile(userPatternToJavaRegexpString(userPattern, true, true, true), Pattern.CASE_INSENSITIVE);
                for (String text : textToCheck) {
                    if (!StringUtils.isEmpty(text)) {
                        if (pattern.matcher(text).find()) {
                            return true;
                        }
                    }
                }
            }
        }

        // no match found
        return false;
    }

    /**
     * Gets the trimmed length of the string, returning 0 if the string is null.
     */
    public static int nullSafeTrimmedLength(String text) {
        return StringUtils.trimToEmpty(text).length();
    }

    public static List<String> splitIntoSentences(String text, Locale locale) {
        BreakIterator sentenceIterator;
        if(locale == null) {
            sentenceIterator = BreakIterator.getSentenceInstance();
        } else {
            sentenceIterator = BreakIterator.getSentenceInstance(locale.unwrap());
        }

        return splitIntoTokens(text, sentenceIterator);
    }
    
    public static List<String> splitIntoWordsAndRemovePunctuation(String text, Locale locale) {
        BreakIterator wordIterator;

        if(locale == null) {
            wordIterator = BreakIterator.getWordInstance();
        } else {
            wordIterator = BreakIterator.getWordInstance(locale.unwrap());
        }

        wordIterator.setText(text);
        int start = wordIterator.first();
        int end = wordIterator.next();

        List<String> tokens = new ArrayList<String>();
        while (end != BreakIterator.DONE) {
            String token = text.substring(start, end);

            // Check whether the Unicode code point at the beginning of this token is alphanumeric
            // If not, we assume it's punctuation and skip it
            if(!StringUtils.isBlank(token)
                && Character.isLetterOrDigit(Character.codePointAt(token, 0))) {

                tokens.add(token);
            }

            start = end;
            end = wordIterator.next();
        }

        return tokens;

    }
    
    private static List<String> splitIntoTokens(String text, BreakIterator breakIterator) {
        breakIterator.setText(text);
        int start = breakIterator.first();
        int end = breakIterator.next();

        List<String> tokens = new ArrayList<String>();
        while (end != BreakIterator.DONE) {
            String token = text.substring(start, end);

            tokens.add(token);
            start = end;
            end = breakIterator.next();
        }

        return tokens;
    }

    public static String escapeHtmlAndApostrophe(String string) {
        String htmlEscapedString = StringEscapeUtils.escapeHtml(string);

        return htmlEscapedString.replaceAll("\'", "&apos;");
    }

    public static String unescapeHtmlAndApostrophe(String string) {
        String htmlUnescapedString = string.replaceAll("&apos;", "\'");

        return StringEscapeUtils.escapeHtml(htmlUnescapedString);
    }

    public static String toLocaleBasedLowerCase(String string, Locale locale) {
        if(locale == null) {
            return string.toLowerCase();
        } else {
            return string.toLowerCase(locale.unwrap());
        }
    }

    /**
     * Represents which type of pattern we will be generating from a user-entered pattern.
     */
    private enum PatternType {MYSQL, JAVA,}

    /**
     * Converts a user-friendly pattern of literal characters and '*' wildcard
     * characters to a java- or mysql-ready regular expression string.
     *
     * There are a couple of differences between {@link PatternType#MYSQL}
     * and {@link PatternType#JAVA}:
     * <ol>
     * <li>Word boundaries are <code>[[:<:]]</code> and <code>[[:>:]]</code> in mySQL and <code>\b</code> in Java.</li>
     * <li>Space character class is <code>[ \u00a0\t\r\n]</code> in mySQL and <code>[\s]</code> in Java.</li>
     *
     * @see #userPatternToMysqlRegexp(String, boolean, boolean, boolean)
     * @see #userPatternToJavaRegexpString(String, boolean, boolean, boolean)
     */
    private static String userPatternToSpecificRegexp(PatternType patternType, String userPattern, boolean wholeWord, boolean supportCaret, boolean supportDollar) {
        if (userPattern.length() == 0) {
            return "$.^";  // nonsensical pattern that never matches anything (empty string causes a Mysql exception)
        }
        // escape all non-alpha numeric chars except '*'.
        // replace all '*' with regular expression '.*'
        StringBuilder regexp = new StringBuilder(userPattern.length() + 32);
        for (int i = 0; i < userPattern.length(); i++) {
            char ch = userPattern.charAt(i);

            String alnumPrefix = "", alnumSuffix = "";

            // special case for the first and last characters.  word boundary
            // checks only work if adjacent to alphanumeric chars.  so make sure
            // we don't combine them with punctuation or special chars like ^, $
            if (wholeWord && i == 0) {
                if (patternType == PatternType.MYSQL) alnumPrefix = "[[:<:]]";
                else if (patternType == PatternType.JAVA) alnumPrefix = "\\b";
            }
            if (wholeWord && i + 1 == userPattern.length()) {
                if (patternType == PatternType.MYSQL) alnumSuffix = "[[:>:]]";
                else if (patternType == PatternType.JAVA) alnumSuffix = "\\b";
            }

            if (ch >= 128 || Character.isLetterOrDigit(ch)) {
                // copy a-zA-Z0-9 and all non-ascii characters as-is
                regexp.append(alnumPrefix).append(ch).append(alnumSuffix);
            } else if (ch == '*') {
                // translate '*' to "match everything"
                regexp.append(".*");
            } else if (ch == ' ' || ch == '\t') {
                // translate space or tab to match 1 or more spaces or tabs
                if (patternType == PatternType.MYSQL) regexp.append("[ \u00a0\t\r\n]+");
                if (patternType == PatternType.JAVA) regexp.append("\\s+");
            } else if (ch == '^' && i == 0 && supportCaret) {
                // first char == ^ means match the beginning of the string
                regexp.append('^');
            } else if (ch == '$' && i + 1 == userPattern.length() && supportDollar) {
                // last char == $ means match the end of the string
                regexp.append('$');
            } else if (ch == '\\' && i + 1 < userPattern.length()) {
                // backslash means escape the next character
                ch = userPattern.charAt(++i);
                if (ch >= 128 || Character.isLetterOrDigit(ch)) {
                    regexp.append(alnumPrefix).append(ch).append(alnumSuffix);
                } else {
                    regexp.append('\\').append(ch);
                }
            } else {
                // escape all punctuation so the chars are matched literally
                regexp.append('\\').append(ch);
            }
        }
        return regexp.toString();
    }

    /**
     * Takes a string with potentially more than one line (separated by \n).
     */
    public static String sortMultiLinePattern(String pattern) {
        if (pattern != null) {
            String[] lines = EOLN.split(pattern);
            Arrays.sort(lines, new Comparator<String>() {
                public int compare(String string1, String string2) {
                    // ignore leading punctuation characters when comparing the
                    // strings for a more user-friendly sort order
                    String alnum1 = stripLeadingPunctuation(string1);
                    String alnum2 = stripLeadingPunctuation(string2);
                    int result = alnum1.compareToIgnoreCase(alnum2);
                    if (result != 0) {
                        return result;
                    }
                    return string1.compareTo(string2);
                }
            });
            pattern = StringUtils.join(lines, '\n');
        }
        return pattern;
    }

    private static String stripLeadingPunctuation(String string) {
        int pos = 0;
        while (pos < string.length() && !Character.isLetterOrDigit(string.charAt(pos))) {
            pos++;
        }
        return string.substring(pos);
    }

    public static String stripIllegalCharacters(String string) {
        if (string == null) {
            return null;
        }
        StringBuilder buf = new StringBuilder(string.length());
        boolean prevWasHighByte = false;
        for (int i = 0; i < string.length(); i++) {
            char ch = string.charAt(i);
            boolean highByte = ch >= 128 && ch <= 255;

            // look for common mistakes and recover valid characters, if possible
            if (highByte && !prevWasHighByte) {
                // if the previous char was a high byte (0x80-0xff) we might be
                // in the middle of an incorrectly encoded multibyte UTF-8
                // character so don't try to interpret it.  otherwise...
                if (ch >= 0x80 && ch <= 0x9f) {
                    // this range has no valid Unicode chars, but it does have common
                    // Windows cp1252 chars.  assume cp1252 and convert to Unicode.
                    String cp1252string;
                    try {
                        cp1252string = new String(new byte[] {(byte) ch}, "cp1252");
                    } catch (UnsupportedEncodingException e) {
                        throw new IllegalStateException(e);
                    }
                    ch = cp1252string.charAt(0);
                    highByte = false;
                }
            }

            if (isLegalCharacter(ch)) {
                buf.append(ch);
            }

            prevWasHighByte = highByte;
        }
        return buf.toString();
    }

    public static boolean containsIllegalCharacters(String string) {
        if (string == null) {
            return false;
        }
        for (int i = 0; i < string.length(); i++) {
            char ch = string.charAt(i);
            if (!isLegalCharacter(ch)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isLegalCharacter(char ch) {
        // according to the XML 1.0 specification these are the legal unicode characters:
        //    #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
        // we don't support the last range containing characters > 16 bits
        return ch == '\t' || ch == '\n' || ch == '\r' || (ch >= 0x20 && ch <= 0xd7ff) || (ch >= 0xe000 && ch <= 0xfffd);
    }

    /**
     * Replaces HTML tags with newlines or spaces and expands HTML entities such as &amp;amp; &amp;quot; &amp;lt; etc.
     */
    public static String stripHtmlTagsAndEntities(String string) {
        if (StringUtils.isBlank(string)) {
            return string;
        }

        // remove HTML tags and replace them with newlines or spaces
        if (string.indexOf('<') != -1) {
            string = HTML_LINEBREAK_PATTERN.matcher(string).replaceAll("\n");
            string = HTML_TAG_PATTERN.matcher(string).replaceAll(" ");
            string = HTML_COMMENT_PATTERN.matcher(string).replaceAll(" ");
        }
        // unescape HTML entities such as &amp; &lt; &quot; &nbsp; etc.
        if (string.indexOf('&') != -1) {
            string = StringEscapeUtils.unescapeHtml(string);
            string = string.replace("&nsub;", "\u2284");
        }
        return string;
    }

    public static boolean isValidURL(String url) {
        // Use UrlValidator to check for null, invalid schemes, etc.
        boolean isValid = new UrlValidator(UrlValidator.ALLOW_2_SLASHES).isValid(url);

        if (isValid) {
            try {
                // Check for invalid esacape sequences.
                URLDecoder.decode(url, "UTF-8");

                // Sanity check that we'll be able to create a new URL object with this string
                new URL(url);

                isValid = true;
            } catch (MalformedURLException mue) {
                isValid = false;
            } catch (UnsupportedEncodingException e) {
                isValid = false; // UTF-8 should always be supported.
            } catch (IllegalArgumentException e) {
                isValid = false; // Couldn't decode url.
            }
        }

        return isValid;
    }

    public static String firstNonBlank(String[] strings) {
        if (strings != null) {
            for (String string : strings) {
                string = StringUtils.trimToNull(string);
                if (string != null) {
                    return string;
                }
            }
        }
        return null;
    }

    /**
     * Returns a MySQL literal containing the specified string, or NULL if the string is null.
     */
    public static String quoteMySQLString(String string) {
        if (string == null) {
            return "NULL";
        }
        // MySQL is different from some other databases in that backslashes must be escaped.
        return "'" + StringUtils.replace(StringUtils.replace(string, "'", "''"), "\\", "\\\\") + "'";
    }

    /**
     * Returns the number of times the specified character appears in the specified string.
     */
    public static int count(String string, char ch) {
        int count = 0, pos = -1;
        while ((pos = string.indexOf(ch, pos + 1)) != -1) {
            count++;
        }
        return count;
    }

    /**
     * Scrubs a string to remove anything that's not "[0-9A-Za-z_]" *
     */
    public static String removeAllNonAlphaNumeric(String string) {
        if (string == null) {
            return string;
        }

        return NON_ALPHA_NUMERIC_PATTERN.matcher(string).replaceAll("");
    }

    /**
     * Trims leading and trailing whitespace and ignoring empty values.
     */
    public static List<String> trimStrings(String[] strings) {
        if (strings == null) {
            return new ArrayList<String>();
        }
        List<String> results = new ArrayList<String>(strings.length);
        for (String string : strings) {
            if (!StringUtils.isEmpty(string)) {
                string = string.trim();
                if (string.length() > 0) {
                    results.add(string);
                }
            }
        }
        return results;
    }

    /**
     * Splits a string based on the specified character, trimming leading and trailing whitespace and ignoring empty values.
     */
    public static List<String> splitAndTrim(String string, char separatorChar) {
        return trimStrings(StringUtils.split(string, separatorChar));
    }

    /**
     * Splits a string based on the specified regular expression, trimming leading and trailing whitespace and ignoring empty values.
     */
    public static List<String> splitAndTrim(String string, Pattern separator) {
        return trimStrings(separator.split(string));
    }

    /**
     * Splits a string based on end-of-line characters, trimming leading and trailing whitespace and ignoring empty lines.
     */
    public static List<String> splitIntoLinesAndTrim(String string) {
        return splitAndTrim(string, EOLN);
    }

    /**
     * Trims, then splits a string on newlines, tabs, spaces, and semicolons and returns elements as a list.
     * If elements is null, returns an empty array.
     * If elements is blank, returns a one-element array new String[] {""}
     */
    public static String[] splitOnWhitespaceAndSemicolon(String elements) {
        if (elements == null) {
            return new String[0];
        }
        return elements.trim().split("[ \t\n;]+");
    }

    public static List<String> splitContentCodes(String contentCodes) {
        if (StringUtils.isEmpty(contentCodes)) {
            return Collections.emptyList();
        }
        return Arrays.asList(SPACES.split(contentCodes));
    }

    public static String joinContentCodes(Collection<String> codes) {
        if (CollectionUtils.isEmpty(codes)) {
            return null;
        }
        return StringUtils.trimToNull(StringUtils.join(new LinkedHashSet<String>(codes), ' ')); // use LinkedHashSet to dedup codes
    }

    /**
     * @return collapses repeating whitespace characters into a single space
     */
    public static String collapseWhitespace(String s) {
        return s.replaceAll("\\s+", " ");
    }

    /**
     * Removes all helpers for message tokens like {0 [Number]} which clarifies what does token mean
     */
    public static String stripCommentedTokens(String message) {
        return COMMENTED_TOKEN_PATTERN.matcher(message).replaceAll("{$1$2}");
    }

    /**
     * Generates a random 128-bit number.  Rather rather than using the standard 36 digit long hexadecimal representation,
     * this returns the value in a more compact form 25 digit form using [0-9a-z] like this: 5k1w4kww5mgtiodn3ydotx5ts
     */
    public static String generateCompactRandomUUID() {
        byte[] bytes = new byte[16];
        NumberUtils.SECURE_RANDOM.nextBytes(bytes); // generate 128 random bits

        // move bit 125 to bit 129 and set bit 125 to one.  this results in a number where
        // we're guaranteed 36^24 < 2^125 <= n < 2^129 < 36^25 which, when formatted as base 36,
        // gives us 128-bits of randomness encoded in a 25 character string using 0-9, a-z.
        byte bit = (byte)(bytes[0] & 0x20);
        bytes[0] |= 0x20;
        if (bit != 0) {
            byte[] temp = new byte[17];
            temp[0] = 1;
            System.arraycopy(bytes, 0, temp, 1, 16);
            bytes = temp;
        }

        // Base 36 (lowercase, alphanumeric characters) is used to generate the compact UUID representation.  While a
        // version using base 62 (adding uppercase letters) would be even more compact, MySQL's non-binary string types
        // (char, varchar, text) are case insensitive by default making base 62 is less useful.  BigInteger.toString()
        // is faster using base 32 than base 64, but it would result in string of length 26 not 25 and the difference
        // in speed is insignificant next to the cost of SecureRandom.nextBytes(), which dominates.
        String string = new BigInteger(1, bytes).toString(36);
        Assert.isTrue(string.length() == 25, string);
        return string;
    }

    public static String generateAnonymousUserUUID() {
        return "z" + generateCompactRandomUUID();  // prepend 'z' so anonymous ids are easily distinguished from normal ids, sort last
    }

    public static boolean doesMultiLineStringContainWord(String multiLineString, String word) {
        Pattern p = Pattern.compile("^" + Pattern.quote(word) + "$", Pattern.MULTILINE);
        return p.matcher(multiLineString).find();
    }

    public static String removeSingleLineWordFromMultiLineString(String multiLineString, String wordToBeRemoved) {
        Pattern p = Pattern.compile("^" + Pattern.quote(wordToBeRemoved) + "$", Pattern.MULTILINE);
        return p.matcher(multiLineString).replaceAll("");
    }

    /**
     * Collapses multiple newlines into a single newline
     */
    public static String collapseNewLines(String s) {
        return s.replaceAll("\n+", "\n");
    }

    /**
     * In formatting we sometimes need to use external data such as product
     * or brand external ID's as id's to identify HTML elements such as divs
     * and spans, etc. The problem is that these strings often contain characters
     * such as white space that is illegal in a DOM id. Legal characters in DOM ids
     * are alphanumeric, period, hyphen, underscore, and colon. This method
     * identifies all characters not in this set  and the underscore, which is
     * used as a delineating character, and replaces them with their numeric
     * equivalent.offeset by underscores. eg. a white space becomes _32_
     * the companion method divIDToString reverses this.
     *
     *
     * @param input string
     * @return the string modified so that it is suitable for use as a DOM id
     */
    public static String stringToDivID(String input) {
        if(input == null) return null;
        StringBuffer divStr =  new StringBuffer();
        Pattern pat = Pattern.compile("[^a-zA-Z0-9\\.:-]");
        Matcher mat = pat.matcher(input);
        while (mat.find()) {
            Integer val = (int)input.charAt(mat.start());
            String insertString = "_" + val.toString() + "_";
            mat.appendReplacement(divStr, insertString);
        }
        mat.appendTail(divStr);
        return divStr.toString();
    }

    /**
     *
     * The companion method stringToDivID (see above)
     *
     * @param input string, a DOM ID
     * @return the original string
     */
    public static String divIDToString(String input)
            throws NumberFormatException {
        if(input == null) return null;
        StringBuffer originalStr =  new StringBuffer();
        Pattern pat = Pattern.compile("_\\d+_");
        Matcher mat = pat.matcher(input);
        while (mat.find()) {
            String valString = input.substring(mat.start() + 1, mat.end() - 1);
            Character insertChar = (char)Integer.parseInt(valString, 10);
            String newStr = insertChar.toString();
            //special cases: $ and \ have special meaning to appendReplacement and must be escaped
            if(insertChar.equals(new Character('$')) || insertChar.equals(new Character('\\'))) {
                newStr = "\\" + newStr;
            }
            mat.appendReplacement(originalStr, newStr);
        }
        mat.appendTail(originalStr);
        return originalStr.toString();
    }

}
