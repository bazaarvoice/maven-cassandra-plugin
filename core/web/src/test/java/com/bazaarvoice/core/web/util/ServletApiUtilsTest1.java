package com.bazaarvoice.core.web.util;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test
public class ServletApiUtilsTest1 {

    /**
     * Ensures that problematic URLs are returned from toConformantURI() with the expected minimally required amount of encoding.
     */
    public void testConformantURI() {
        String testURL1 = "http://img.bluenile.com/is/image/bluenile/-diamond-engagement-ring-setting-white-gold-/setting_template_main?$295_250$&$diam_shape=is{bluenile/main_RD_standard_100}&$diam_position=-5,-30&$ring_position=0,0&$ring_sku=is{bluenile/DM15402000_setmain}";
        String testURL1Expected = "http://img.bluenile.com/is/image/bluenile/-diamond-engagement-ring-setting-white-gold-/setting_template_main?$295_250$&$diam_shape=is%7Bbluenile/main_RD_standard_100%7D&$diam_position=-5,-30&$ring_position=0,0&$ring_sku=is%7Bbluenile/DM15402000_setmain%7D";
        Assert.assertEquals(ServletApiUtils.toConformantURI(testURL1).toString(), testURL1Expected);
        String testURL2 = "http://image.oriental.com/95_2851.jpg?resize(110x110)";
        Assert.assertEquals(ServletApiUtils.toConformantURI(testURL2).toString(), testURL2);
        String testURL3 = "http://images.roots.com/index.php?imageQuality=0.9&imageWidth=200&imageHeight=200&imageName=enlargements%2Fe18019423_2402.jpg";
        String testURL3Expected = "http://images.roots.com/index.php?imageQuality=0.9&imageWidth=200&imageHeight=200&imageName=enlargements/e18019423_2402.jpg";
        Assert.assertEquals(ServletApiUtils.toConformantURI(testURL3).toString(), testURL3Expected);
    }
}