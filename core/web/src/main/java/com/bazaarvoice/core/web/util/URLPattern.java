package com.bazaarvoice.core.web.util;

import com.bazaarvoice.core.util.BVStringUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Compares URL path names and parameters with a specified URL pattern string.
 * <p/>
 * The URL pattern string is built up from a combination of Ant-style patterns and
 * regular expression fragments.  The general format of the URL pattern looks like this:
 * <pre>
 *   "<path-name-string-pattern> ['?' <parameter-name> ['=' <parameter-value-pattern>] ['&' ... ] ]"
 * </pre>
 * Each string pattern used for a path name or a parameter value is an Ant-style pattern
 * supporting '*' and '**' (but not '?') with one special extension:  anything inside
 * parentheses is interpreted as a JDK 1.4-style regular expression.
 * <p/>
 * For example, the pattern "/(product|category)/questions.(htm|xml)/" will match the
 * following URLs:
 * <pre>
 *    /product/questions.htm
 *    /product/questions.xml
 *    /category/questions.htm
 *    /category/questions.xml
 * </pre>
 */
public class URLPattern {
    private final StringPattern _pathNamePattern;
    private final ParameterPattern[] _parameterPatterns;

    public URLPattern(String urlPattern, Map<String, URLPatternPredicate> predicateMap) {
        if (!urlPattern.startsWith("/")) {
            throw new IllegalArgumentException("URL patterns must be absolute and start with a leading slash: " + urlPattern);
        }

        Tokenizer tokens = new Tokenizer(urlPattern, predicateMap);

        // compile a pattern for the URL path name.
        _pathNamePattern = tokens.nextPattern("?", true);

        // compile patterns for the URL parameters.
        List<ParameterPattern> parameterPatterns = new ArrayList<ParameterPattern>();
        while (tokens.hasNext()) {
            // there are two cases we handle here.
            // 1. if there's an '=' sign in the "name=value" pattern then we match on the string
            //    value such that a missing parameter value is the same as is the parameter value
            //    is the empty string.  this is convenient for patterns like 'format=(|standalone)'
            //    where the value defaults to 'standalone' if it's missing.
            // 2. if there's no '=' sign (eg. the parameterPair is just "format") then we just test
            //    that the parameter exists.
            // To match against a parameter value that must be present and must be the empty string,
            // you'd have to have two parts to the URL pattern: one to test the existence of the
            // parameter and a second to test its value, like this: "/*/*/reviews.htm?format&format=".
            String parameterName = tokens.nextLiteral("=&");
            StringPattern parameterValuePattern;
            if (tokens.getLastDelimiter() == '=') {
                parameterValuePattern = tokens.nextPattern("&", false);
            } else {
                parameterValuePattern = null;  // test existence of the parameter only
            }
            parameterPatterns.add(new ParameterPattern(parameterName, parameterValuePattern));
        }
        _parameterPatterns = parameterPatterns.toArray(new ParameterPattern[parameterPatterns.size()]);
    }

    /**
     * Returns true if the specified URL path name and parameters match this pattern.
     * @param pathName the URL path name returned by "ServletUtil.getRequestPath(request)".
     * @param parameters the URL parameters returned by "request.getParameterMap()".
     * @return true if the specified URL path name and parameters match this pattern.
     */
    public boolean matches(String pathName, Map<String,String[]> parameters) {
        if (!_pathNamePattern.matches(pathName)) {
            return false;
        }
        for (ParameterPattern parameterPattern : _parameterPatterns) {
            if (!parameterPattern.matches(parameters)) {
                return false;
            }
        }
        return true;  // all components of the URL matched
    }

    private static interface StringPattern {
        boolean matches(String string);
    }

    private static class AlwaysTruePattern implements StringPattern {
        public boolean matches(String string) {
            return true;
        }
    }

    private static class LiteralStringPattern implements StringPattern {
        private final String _literal;

        public LiteralStringPattern(String literal) {
            _literal = literal;
        }

        public boolean matches(String string) {
            return _literal.equals(string);
        }
    }

    private static class RegexStringPattern implements StringPattern {
        private final Pattern _regex;

        public RegexStringPattern(Pattern regex) {
            _regex = regex;
        }

        public boolean matches(String string) {
            return _regex.matcher(string).matches();
        }
    }

    private static class PredicateStringPattern implements StringPattern {
        private final Map<String, URLPatternPredicate> _predicateMap;
        private final List<String> _variableGroups;
        private final Pattern _regex;

        public PredicateStringPattern(Pattern regex, List<String> variableGroups, Map<String, URLPatternPredicate> predicateMap) {
            _regex = regex;
            _variableGroups = variableGroups;
            _predicateMap = predicateMap;
        }

        public boolean matches(String string) {
            Matcher matcher = _regex.matcher(string);
            if (matcher.matches()) {
                if (matcher.groupCount() != _variableGroups.size()) {
                    return false;
                }
                int groupCounter = 1;
                for (String variableName : _variableGroups) {
                    if (!_predicateMap.get(variableName).matches(matcher.group(groupCounter++))) {
                        return false;
                    }
                }

                return true;
            } else {
                return false;
            }
        }
    }

    private static class ParameterPattern {
        private final String _parameterName;
        private final StringPattern _parameterValuePattern;

        public ParameterPattern(String parameterName, StringPattern parameterValuePattern) {
            _parameterName = parameterName;
            _parameterValuePattern = parameterValuePattern;
        }

        public boolean matches(Map<String, String[]> parameterMap) {
            if (_parameterValuePattern == null) {
                // just check for the existence of the parameter.  we don't care what the value is
                return parameterMap.containsKey(_parameterName);

            } else {
                // get the first non-blank value and compare it against our pattern.  if the parameter
                // is missing completely then act as if its value is the empty string.  this lets us
                // handle patterns like "format=(|standalone)" where a missing format parameter is
                // treated the same as if 'format=standalone'.
                String parameterValue = BVStringUtils.firstNonBlank(parameterMap.get(_parameterName));
                return _parameterValuePattern.matches(StringUtils.trimToEmpty(parameterValue));
            }
        }
    }

    private static class Tokenizer {
        // special characters are ant special chars '*', '**' plus regex start/end '(', ')' plus backslash '\'
        private final static Pattern STRING_PATTERN_SPECIAL_CHARS = Pattern.compile("[\\$*()\\\\]");

        private final String _string;
        private final Map<String, URLPatternPredicate> _predicateMap;
        private int _position;
        private char _lastDelimiter;

        public Tokenizer(String string, Map<String, URLPatternPredicate> predicateMap) {
            _string = string;
            _predicateMap = predicateMap;
        }

        public char getLastDelimiter() {
            return _lastDelimiter;
        }

        public boolean hasNext() {
            return _position < _string.length();
        }

        public String nextLiteral(String delimiterChars) {
            StringBuilder buf = new StringBuilder();
            _lastDelimiter = 0;
            for (; _position < _string.length(); _position++) {
                char ch = _string.charAt(_position);
                if (delimiterChars.indexOf(ch) != -1) {
                    _lastDelimiter = ch;
                    _position++;
                    break;
                }
                buf.append(ch);
            }
            return buf.toString();
        }

        public StringPattern nextPattern(String delimiterChars, boolean inPathName) {
            StringBuilder regex = new StringBuilder();
            List<String> variableGroups = new LinkedList<String>();
            int startPosition = _position;
            int regexNesting = 0;
            int characterClassNesting = 0;
            for (; _position < _string.length(); _position++) {
                char ch = _string.charAt(_position);
                if (ch == '\\') {
                    // backslash-escaped character.  don't interpret the next character specially
                    _position++;
                    if (_position == _string.length()) {
                        throw new IllegalArgumentException("Patterns may not end in a trailing backslash unless that backslash is escaped with a backslash: " + _string);
                    }
                    appendEscaped(regex, _string.charAt(_position));

                } else if (regexNesting > 0 || ch == '(') {
                    // this part of the string is using JDK regular expression syntax.  just copy the characters through.
                    // note: anything between parentheses '(' to ')' is assumed to be a regex pattern.

                    // track parenthesis grouping level so we know when the regex is finished and we can switch back to ant patterns.
                    // track square brackets so we know when we're in a character class and parenthesis don't indicate a grouping level change.
                    if (ch == '[') {
                        characterClassNesting++;
                    } else if (ch == ']') {
                        characterClassNesting--;
                        // note: if a character class needs to contain a ']' then it should be escaped with a backslash.
                        if (characterClassNesting < 0) {
                            throw new IllegalArgumentException("Pattern has mismatched square brackets (too many ']' characters) at char " + (_position + 1) + ": " + _string);
                        }
                    } else if (characterClassNesting == 0) {
                        if (ch == '(') {
                            regexNesting++;
                        } else if (ch == ')') {
                            regexNesting--;
                            if (regexNesting < 0) {
                                throw new IllegalArgumentException("Pattern has mismatched parentheses (too many ')' characters) at char " + (_position + 1) + ": " + _string);
                            }
                        }
                    }

                    regex.append(ch);  // no escaping required

                    // convert parenthesis expressions to a non-capturing group so as not to conflict with the variable name callouts
                    if (ch == '(' && characterClassNesting == 0) {
                        regex.append("?:");
                    }

                } else if (delimiterChars.indexOf(ch) != -1) {
                    // found a delimiter while we're not inside a regular expression.  we're done.
                    _lastDelimiter = ch;
                    break;
                    
                } else if (ch == '$' && _position+2 < _string.length() && _string.charAt(_position+1) == '{' && _string.indexOf('}',_position + 1) > -1) {
                    // everything until closing } is a variable name
                    String variableName = _string.substring(_position + 2, _position = _string.indexOf('}', _position+1));
                    if (_predicateMap == null) {
                        throw new IllegalArgumentException("Found variable " + variableName + " in URL pattern: " + _string + " but predicates are not defined. Check that predicateMap property is set before the urlMap property.");
                    }
                    if (!_predicateMap.containsKey(variableName)) {
                        throw new IllegalArgumentException("Variable " + variableName + " is not defined in predicate map in URL pattern: " + _string);
                    }
                    variableGroups.add(variableName);
                    regex.append("([^/]+)");

                } else {
                    // this part of the string is using Ant path-style wildcards.  convert them to regex.

                    if (inPathName && ch == '/' && _string.substring(_position).startsWith("/**")) {
                        // ant path '/**' maps to regex '(/.*)?'
                        regex.append("(?:/.*)?");

                        // the '/**' must be followed by a '/' or by a delimiter ending the pattern
                        if (_position + 3 < _string.length() && _string.charAt(_position + 3) != '/' && delimiterChars.indexOf(_string.charAt(_position + 3)) == -1) {
                            throw new IllegalArgumentException("Pattern '**' must match complete directory names (use '/**/') at char " + (_position + 1) + ": " + _string);
                        }
                        _position += 2;

                    } else if (ch == '*') {
                        // ant path '*' maps to regex '[^/]*'.  but make sure we're not part of an supported '**' pattern
                        if (_string.substring(_position).startsWith("**")) {
                            throw new IllegalArgumentException("Pattern '**' must match complete directory names (use '/**/') at char " + (_position + 1) + ": " + _string);
                        }
                        regex.append(inPathName ? "[^/]*" : ".*");
                        
                    } else {
                        // everything else matches as a literal value
                        appendEscaped(regex, ch);
                    }

                }
            }
            if (characterClassNesting > 0) {
                throw new IllegalArgumentException("Pattern has mismatched square brackets (too many '[' characters) between chars " + (startPosition + 1) + " and " + _position + ": " + _string);
            }
            if (regexNesting > 0) {
                throw new IllegalArgumentException("Pattern has mismatched parentheses (too many '(' characters) between chars " + (startPosition + 1) + " and " + _position + ": " + _string);
            }

            String consumed = _string.substring(startPosition, _position);

            if (_lastDelimiter != 0) {
                _position++;
            }

            // special case for matching everything
            if ((inPathName && "/**".equals(consumed)) || (!inPathName && "*".equals(consumed))) {
                return new AlwaysTruePattern();
            }

            // if no special characters then match the pattern as a literal string (faster than regex)
            if (!STRING_PATTERN_SPECIAL_CHARS.matcher(consumed).find()) {
                return new LiteralStringPattern(consumed);
            }


            // otherwise return the regular expression we've built
            if (variableGroups.isEmpty()) {
                return new RegexStringPattern(Pattern.compile(regex.toString()));
            } else {
                return new PredicateStringPattern(Pattern.compile(regex.toString()), variableGroups, _predicateMap);
            }
        }

        private static void appendEscaped(StringBuilder buf, char ch) {
            if (!Character.isLetterOrDigit(ch)) {
                buf.append('\\');
            }
            buf.append(ch);
        }
    }
}
