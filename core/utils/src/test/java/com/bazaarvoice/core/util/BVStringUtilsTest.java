package com.bazaarvoice.core.util;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.regex.Pattern;

/**
 * Tests the functions in com.bazaarvoice.core.util.BVStringUtils.
 */
@Test
public class BVStringUtilsTest {
    public void testSplitOnWhitespaceAndSemicolon() {
        assertSplit("http://singleline/test", new String[] {"http://singleline/test"});
        assertSplit("http://twoentries http://here/test", new String[] {"http://twoentries", "http://here/test"});
        assertSplit("http://threeentries\nwithAnewline andaspace", new String[] {"http://threeentries", "withAnewline", "andaspace"});
        assertSplit("http://fourentries;http://with;semis;test", new String[] {"http://fourentries", "http://with", "semis", "test"});
        assertSplit("", new String[] {""});
        assertSplit(null, new String[] {});
    }

    private void assertSplit(String input, String[] output) {
        Assert.assertEquals(BVStringUtils.splitOnWhitespaceAndSemicolon(input), output, input);
    }

    public void testRemoveIllegalChars1() {
        String k1 = "A Wonderful Gift that is a Great Accent to your Wrist.<BR>&#26;A wonderf&#25;ul gift. Three &#55295; &#55296; different looks to accent your wrist and complement any wardrobe.";
        String k2 = BVStringUtils.stripHtmlTagsAndEntities(k1);
        String k3 = BVStringUtils.stripIllegalCharacters(k2);

        Assert.assertEquals(k1.length(), 175); // unescaped version
        Assert.assertEquals(k2.length(), 150); // escape &#26; &#25; &#55295; &#55296; to unicode equivalent
        Assert.assertEquals(k3.length(), 147); // remove &#26; and &#25; &#55296; unicode values. (&#55295; is within unicode range)
    }

    public void testRemoveIllegalChars2() {
        String k1 = "Officially &#55295; &#55296; licensed by Sony Computer Entertainment America, NYKO&#39;s 8Mb Memory Card for PlayStation&#174;2. can store more than twice the data of ordinary memory cards.  Built with Sony&#39;s authentication and encryption technology, MagicGate&#26;.";
        String k2 = BVStringUtils.stripHtmlTagsAndEntities(k1);
        String k3 = BVStringUtils.stripIllegalCharacters(k2);

        Assert.assertEquals(k1.length(), 270); // unescaped version
        Assert.assertEquals(k2.length(), 239); // escape &#26; &#25; &#55295; &#55296; to unicode equivalent
        Assert.assertEquals(k3.length(), 237); // remove &#26; and &#25; &#55296; unicode values. (&#55295; is within unicode range)
    }

    public void testIsValidURL() {
        String validUrl = "http://products.proflowers.com/OrganicFruitSamplerExtra60FREEFruit-5617?ssid=4&ref=BVreviews";
        String validUrl2 = "http://www.burpee.com/images/us//local/products/detail/b91900.jpg";
        String validUrl3 = "http://images.americangolf.co.uk/ComponentImages/Products/ListingThumbnail/Listing-footjoy-shoe-care-kit-l(1).jpg";
        String invalidUrl = "http://products.proflowers.com/OrganicFruitSamplerExtra60%FREEFruit-5617?ssid=4&ref=BVreviews";
        Assert.assertTrue(BVStringUtils.isValidURL(validUrl));
        Assert.assertTrue(BVStringUtils.isValidURL(validUrl2));
        Assert.assertTrue(BVStringUtils.isValidURL(validUrl3)); // Testing the patched version of UrlValidator.
        Assert.assertFalse(BVStringUtils.isValidURL(invalidUrl));
    }
    
    public void testRemoveMySQLRegexCharacters() {
        String[][] tests = {
                {"*test*", " test "},
                {"\\test", " test"},
                {"^rest$", " rest "},
                {"^sets", " sets"},
                {"t$est*tes^t", "t est tes t"}
        };
        for(String[] test: tests) {
            Assert.assertEquals(test.length, 2);
            Assert.assertEquals(BVStringUtils.removeMySQLRegexpCharacters(test[0]), test[1]);
        }
    }

    public void testReplaceMySQLLikeWildcards() {
        String[][] tests = {
                {"*test*", "*test*"},
                {"\\test", "\\test"},
                {"^rest$", "^rest$"},
                {"flo%er", "flo\\%er"},
                {"_ower", "\\_ower"}
        };
        for(String[] test: tests) {
            Assert.assertEquals(test.length, 2);
            Assert.assertEquals(BVStringUtils.replaceMySQLLikeWildcards(test[0]), test[1]);
        }
    }

    public void testUserPatternToMysqlAndJavaPattern() {
        String[][] tests = {
                // user pattern, expected mysql pattern, expected java regex pattern, java test match

                // general test, ensure that * works as expected
                {"*test*", ".*test.*", ".*test.*", "an interesting test here"},
                // test beginning of string (^) match
                {"^test", "^test[[:>:]]", "^test\\b", "test here"},
                // test end of string ($) match
                {"test$", "[[:<:]]test$", "\\btest$", "interesting test"},
                // test space character class
                {"test test", "[[:<:]]test[ \u00a0\t\r\n]+test[[:>:]]", "\\btest\\s+test\\b", "test          test"},
                // test tab character class
                {"test\ttest", "[[:<:]]test[ \u00a0\t\r\n]+test[[:>:]]", "\\btest\\s+test\\b", "test\t     \t\t   test"},
                // test unicode character match
                {"Trademark: \u2122 xyz", "[[:<:]]Trademark\\:[ \u00a0\t\r\n]+\u2122[ \u00a0\t\r\n]+xyz[[:>:]]", "\\bTrademark\\:\\s+\u2122\\s+xyz\\b", "Trademark: \u2122 xyz"},
                // test useless backslash is removed
                {"some\\thing", "[[:<:]]something[[:>:]]", "\\bsomething\\b", "something"},
                // test meaningful backslash is retained
                {"some\\:thing", "[[:<:]]some\\:thing[[:>:]]", "\\bsome\\:thing\\b", "some:thing"},

        };

        for (String[] test : tests) {
            Assert.assertEquals(4, test.length);
            String testcase = test[0];
            String mysql = test[1];
            String java = test[2];
            String expectedMatch = test[3];

            String mysqlRegexString = BVStringUtils.userPatternToMysqlRegexp(testcase, true, true, true);
            Assert.assertEquals(mysqlRegexString, mysql, "mySql String incorrect for testcase " + testcase);

            String javaRegexpString = BVStringUtils.userPatternToJavaRegexpString(testcase, true, true, true);
            Assert.assertEquals(javaRegexpString, java, "java String incorrect for testcase " + testcase);
            Assert.assertTrue(Pattern.compile(javaRegexpString).matcher(expectedMatch).find(),
                    "Expected to find regexp '" + javaRegexpString + "' in test string '" + expectedMatch + "', but didn't.");

        }

    }

    public void testRemoveSingleLineWordFromMultiLineString() {
        String multiLineString = "avon\nbestbuy\nhomedepot\nsundance\nhomedepot-canada\ndoitbest";
        String expectedStringWithWordRemoved = "avon\nbestbuy\n\nsundance\nhomedepot-canada\ndoitbest";
        String actualStringWithWordRemoved = BVStringUtils.removeSingleLineWordFromMultiLineString(multiLineString, "homedepot");
        Assert.assertEquals(actualStringWithWordRemoved, expectedStringWithWordRemoved);
    }

    public void testRemoveSingleLineWordFromMultiLineStringWithWildcard() {
        String multiLineString = "avon*\nbestbuy*\nhomedepot*\nsundance*\nhomedepot-canada*\ndoitbest*";
        String expectedStringWithWordRemoved = "avon*\nbestbuy*\n\nsundance*\nhomedepot-canada*\ndoitbest*";
        String actualStringWithWordRemoved = BVStringUtils.removeSingleLineWordFromMultiLineString(multiLineString, "homedepot*");
        Assert.assertEquals(actualStringWithWordRemoved, expectedStringWithWordRemoved);
    }

    public void testCollapseMultipleNewLines() {
        String inputWithMultipleNewLines = "avon\n\nbestbuy\n\n\nsundance\nhomedepot-canada\ndoitbest";
        String expectedStringWithCollapsedNewLines = "avon\nbestbuy\nsundance\nhomedepot-canada\ndoitbest";
        String actualStringWithCollapsedNewLines = BVStringUtils.collapseNewLines(inputWithMultipleNewLines);
        Assert.assertEquals(actualStringWithCollapsedNewLines, expectedStringWithCollapsedNewLines);
    }

    public void testDoesMultiLineStringContainWord() {
        String inputThatContainsWord = "avon\nbestbuy\nhomedepot\nsundance\nhomedepot-canada\ndoitbest";
        String inputThatDoesNotContainWord = "avon\nbestbuy\nsundance\nhomedepot-canada\ndoitbest";
        Assert.assertTrue(BVStringUtils.doesMultiLineStringContainWord(inputThatContainsWord, "homedepot"));
        Assert.assertFalse(BVStringUtils.doesMultiLineStringContainWord(inputThatDoesNotContainWord, "homedepot"));
    }

    public void testDoesMultiLineStringContainWordWithWildcard() {
        String inputThatContainsWord = "avon*\nbestbuy*\nhomedepot*\nsundance*\nhomedepot-canada*\ndoitbest*";
        String inputThatDoesNotContainWord = "avon*\nbestbuy*\nsundance*\nhomedepot-canada*\ndoitbest*";
        Assert.assertTrue(BVStringUtils.doesMultiLineStringContainWord(inputThatContainsWord, "homedepot*"));
        Assert.assertFalse(BVStringUtils.doesMultiLineStringContainWord(inputThatDoesNotContainWord, "homedepot*"));
    }

    public void testCompactUUID() {
        // test 100 IDs.  if this were to fail it could fail non-deterministically.  but, in practice, 100
        // appears to be plenty of iterations to detect basic bugs in generateCompactRandomUUID().
        for (int i = 0; i < 100; i++) {
            String uuid = BVStringUtils.generateCompactRandomUUID();
            Assert.assertEquals(uuid.length(), 25, "Compact UUIDs should always be 25 characters long: " + uuid);
            Assert.assertTrue(uuid.matches("[0-9a-z]+"), uuid);
        }
    }

    public void testStringToDivID() {
        //test null input
        String testStrIn = null;
        String testStrOut = BVStringUtils.stringToDivID(testStrIn);
        Assert.assertNull(testStrOut);
        testStrOut = BVStringUtils.divIDToString(testStrOut);
        Assert.assertEquals(testStrIn, testStrOut);
        //test empty string
        testStrIn = "";
        testStrOut = BVStringUtils.stringToDivID(testStrIn);
        Assert.assertEquals(testStrOut, "");
        testStrOut = BVStringUtils.divIDToString(testStrOut);
        Assert.assertEquals(testStrIn, testStrOut);
        //basic character test
        testStrIn = "ab cd_:-.1234";
        testStrOut = BVStringUtils.stringToDivID(testStrIn);
        Assert.assertEquals(testStrOut, "ab_32_cd_95_:-.1234");
        testStrOut = BVStringUtils.divIDToString(testStrOut);
        Assert.assertEquals(testStrIn, testStrOut);
        //test all the odd keyboard characters
        testStrIn = "abcd!@#$%^&*()+=~{}[]|?/<,>8";
        testStrOut = BVStringUtils.stringToDivID(testStrIn);
        testStrOut = BVStringUtils.divIDToString(testStrOut);
        Assert.assertEquals(testStrIn, testStrOut);
        //test escape & control characters
        testStrIn = "ab\\cd e-f g.h\ni4j\rk:l\fmn\top";
        testStrOut = BVStringUtils.stringToDivID(testStrIn);
        testStrOut = BVStringUtils.divIDToString(testStrOut);
        Assert.assertEquals(testStrIn, testStrOut);
        //test digit boundaries
        testStrIn = "abcd 4 top_5_z";
        testStrOut = BVStringUtils.stringToDivID(testStrIn);
        testStrOut = BVStringUtils.divIDToString(testStrOut);
        Assert.assertEquals(testStrIn, testStrOut);
    }
}
