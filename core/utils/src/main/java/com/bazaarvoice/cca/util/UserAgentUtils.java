package com.bazaarvoice.cca.util;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserAgentUtils {
    private static final Pattern CHROME_PATTERN = Pattern.compile("chrome[ \\/]([\\w.]+)");
    private static final Pattern SAFARI_PATTERN = Pattern.compile("safari[ \\/]([\\w.]+)");
    private static final Pattern WEBKIT_PATTERN = Pattern.compile("webkit[ \\/]([\\w.]+)");
    private static final Pattern IE_PATTERN = Pattern.compile("msie ([\\w.]+)");
    private static final Pattern OPERA_PATTERN = Pattern.compile("opera(?:.*version)?[ \\/]([\\w.]+)");
    private static final Pattern FIREFOX_PATTERN = Pattern.compile("firefox[ \\/]([\\w.]+)");

    private static final List<UserAgentBrowserPattern> USER_AGENT_BROWSER_PATTERNS = Arrays.asList(
            new UserAgentBrowserPattern(BrowserInfo.BrowserType.CHROME, CHROME_PATTERN),
            new UserAgentBrowserPattern(BrowserInfo.BrowserType.SAFARI, SAFARI_PATTERN),
            new UserAgentBrowserPattern(BrowserInfo.BrowserType.WEBKIT, WEBKIT_PATTERN),
            new UserAgentBrowserPattern(BrowserInfo.BrowserType.OPERA, OPERA_PATTERN),
            new UserAgentBrowserPattern(BrowserInfo.BrowserType.IE, IE_PATTERN),
            new UserAgentBrowserPattern(BrowserInfo.BrowserType.FIREFOX, FIREFOX_PATTERN),
            new MozillaUserAgentBrowserPattern()
    );

    /**
     * Returns the browser information based on the user agent string.  Never returns <tt>null</tt>
     *
     * @param userAgent user agent string
     *
     * @return the browser information
     */
    public static BrowserInfo getBrowserInfo(String userAgent) {
        if(userAgent == null) {
            return BrowserInfo.UNKNOWN_BROWSER;
        }

        String lowerUserAgent = userAgent.toLowerCase();

        for (UserAgentBrowserPattern userAgentBrowserPattern: USER_AGENT_BROWSER_PATTERNS) {
            BrowserInfo browserInfo = userAgentBrowserPattern.match(lowerUserAgent);
            if (browserInfo != null) {
                return browserInfo;
            }
        }

        return new BrowserInfo(BrowserInfo.BrowserType.UNKNOWN, BrowserInfo.UNKNOWN_VERSION, userAgent);
    }

    private static class UserAgentBrowserPattern {
        private BrowserInfo.BrowserType _browserType;
        private Pattern _pattern;

        public UserAgentBrowserPattern(BrowserInfo.BrowserType browserType, Pattern pattern) {
            _browserType = browserType;
            _pattern = pattern;
        }

        /**
         * If the user agent string matches this browser pattern, return the browser information.
         *
         * @param userAgent The user agent string
         *
         * @return The browser information if the pattern matches, otherwise <tt>null</tt>
         */
        public BrowserInfo match(String userAgent) {
            Matcher matcher = _pattern.matcher(userAgent);
            if (!matcher.find()) {
                return null;
            }

            String version = BrowserInfo.UNKNOWN_VERSION;
            if (matcher.groupCount() > 0) {
                version = matcher.group(1);
            }

            return new BrowserInfo(_browserType, version, userAgent);
        }
    }

    private static class MozillaUserAgentBrowserPattern extends UserAgentBrowserPattern {
        private static final Pattern MOZILLA_PATTERN = Pattern.compile("mozilla(?:.*? rv:([\\w.]+))?");
        private static final Pattern COMPATIBLE_TEST_PATTERN = Pattern.compile("compatible");

        public MozillaUserAgentBrowserPattern() {
            super(BrowserInfo.BrowserType.MOZILLA, MOZILLA_PATTERN);
        }

        @Override
        public BrowserInfo match(String userAgent) {
            // Don't want compatible browsers
            if (COMPATIBLE_TEST_PATTERN.matcher(userAgent).matches()) {
                return null;
            }

            return super.match(userAgent);
        }
    }
}
