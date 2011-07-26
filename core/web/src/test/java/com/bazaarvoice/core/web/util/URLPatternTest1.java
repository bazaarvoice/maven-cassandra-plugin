package com.bazaarvoice.core.web.util;

import org.apache.commons.lang.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Map;

@Test
public class URLPatternTest1 {

    public void testPatterns() {
        testPattern("/*/*/reviews.htm", "/2795/12345/reviews.htm?format=embedded", true);
        testPattern("/*/*/reviews.htm", "/2795/reviews.htm?format=embedded", false);
        testPattern("/*/*/reviews.htm", "/2795/12345/xyz/reviews.htm?format=embedded", false);
        testPattern("/**/reviews.htm", "/2795/12345/xyz/reviews.htm?format=embedded", true);

        testPattern("/**", "", true);
        testPattern("/**", "/", true);
        testPattern("/**", "/abc", true);
        testPattern("/**", "/abc/", true);

        testPattern("/*", "", false);
        testPattern("/*", "/", true);
        testPattern("/*", "/abc", true);
        testPattern("/*", "/abc/", false);
        testPattern("/*/", "/abc/", true);

        testPattern("/*/*/reviews.htm?format=(|standalone)", "/x/y/reviews.htm", true);
        testPattern("/*/*/reviews.htm?format=(|standalone)", "/x/y/reviews.htm?format=standalone", true);
        testPattern("/*/*/reviews.htm?format=(|standalone)", "/x/y/reviews.htm?format=embedded", false);
        testPattern("/*/*/reviews.htm?format", "/x/y/reviews.htm", false);
        testPattern("/*/*/reviews.htm?format", "/x/y/reviews.htm?format=", true);
        testPattern("/*/*/reviews.htm?format", "/x/y/reviews.htm?format=abc", true);

        testException("/**.htm"); // not supported
        testPattern("/**/*.htm", "/a/b/c/d/e.htm", true); // ok
        testException("/a**"); // not supported
        testException("/a**b"); // not supported
        testPattern("/a/**?format=xyz", "/a/b/c/d?format=xyz", true);
        testPattern("/z/**?format=xyz", "/a/b/c/d?format=xyz", false);

        testPattern("/foo.(xml|htm(l)?)", "/foo.csv", false);
        testPattern("/foo.(xml|htm(l)?)", "/foo.xml", true);
        testPattern("/foo.(xml|htm(l)?)", "/foo.htm", true);
        testPattern("/foo.(xml|htm(l)?)", "/foo.html", true);
        testPattern("/foo.(xml|htm(l)?)", "/foo.htmlx", false);
        testPattern("/foo.(xml|htm(l)?)", "/foo-htmlx", false);
        testPattern("/foo.(xml|htm(l)?)?format", "/foo.html", false);
        testPattern("/foo.(xml|htm(l)?)?format", "/foo.html?format=_", true);
    }

    private void testPattern(String pattern, String url, boolean expected) {
        String urlPath = StringUtils.substringBefore(url, "?");
        String urlParams = StringUtils.substringAfter(url, "?");

        Map<String, URLPatternPredicate> predicateMap = Collections.emptyMap();
        URLPattern matcher = new URLPattern(pattern, predicateMap);
        boolean actual = matcher.matches(urlPath, ServletApiUtils.parseURLParameters(urlParams));
        Assert.assertEquals(actual, expected, "Pattern '" + pattern + "' and URL '" + urlPath + "?" + urlParams + "' should " + (expected ? "" : "not ") + "have matched.");
    }

    private void testException(String pattern) {
        try {
            Map<String, URLPatternPredicate> predicateMap = Collections.emptyMap();
            new URLPattern(pattern, predicateMap);
            Assert.fail("Pattern should have thrown an exception: " + pattern);
        } catch (IllegalArgumentException e) {
            // ok
        }
    }
}
