package com.bazaarvoice.prr.util;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

/**
 * Utility class for scaling an image from one size to another
 */
public abstract class ImageScaler {

    /**
     * The default BufferedImage <code>type</code>, used when creating new BufferedImage objects
     * from incompatible source types.
     */
    private static final int DEFAULT_IMAGE_TYPE = BufferedImage.TYPE_INT_RGB;

    /**
     * Returns an image that has been scaled so that the width and height are no larger than the specified maximum
     * The aspect ratio of the image is preserved
     *
     * @param image        the image to scale
     * @param maxDimension the desired maximum width and height
     * @return the scaled image
     */
    public static BufferedImage scale(BufferedImage image, int maxDimension) {
        return scale(image, maxDimension, maxDimension);
    }

    /**
     * Returns an image that has been scaled so that the width and height are no larger than the specified maximums
     * The image will be scaled to a size that meets both maximum specifications while preserving the aspect ratio
     *
     * @param image     the image to scale
     * @param maxWidth  the maximum width
     * @param maxHeight the maximum height
     * @return the scaled image
     */
    public static BufferedImage scale(BufferedImage image, int maxWidth, int maxHeight) {

        double widthScale = getScalingFactor(image.getWidth(), maxWidth);
        double heightScale = getScalingFactor(image.getHeight(), maxHeight);
        double scalingFactor = Math.min(widthScale, heightScale);

        return scale(image, scalingFactor);
    }

    /**
     * Returns an image that has been scaled by the specified scaling factor
     *
     * @param image         the image to scale
     * @param scalingFactor the factor to scale the image by
     * @return the scaled image, or the input image if the scaling factor is 1.0
     */
    public static BufferedImage scale(BufferedImage image, double scalingFactor) {
        //NOTE: Always run the transform even if scalingFactor==1.0
        //By transforming the image, we avoid an issue with the alpha channel corrupting the final image.

        // Some BufferedImages created from PNGs by IOImage.read() have a BufferedImage
        // *type* of 0.  BufferedImage objects in this state don't play well with others.
        // (E.g. creating a new BufferedImage() with this type, as in targetImage below,
        // causes an exception.  Also, trying to apply the transformation fails as well.)
        // To work around this, we'll just copy any such images in memory to a new, friendly
        // buffered image type that can then be scaled.  If image.getType() != TYPE_CUSTOM, then
        // no conversion is necessary.
        image = standardizeBufferedImageType(image);

        // Create target image manually, instead of letting the filter() method create it.  This solves an issue
        // with an alpha channel being created and saved without a JFIF color information when working with JPEG images.
        BufferedImage targetImage = new BufferedImage((int) (image.getWidth() * scalingFactor), (int) (image.getHeight() * scalingFactor), image.getType());

        Graphics2D graphics = targetImage.createGraphics();
        graphics.drawImage(image.getScaledInstance((int) (image.getWidth() * scalingFactor), (int) (image.getHeight() * scalingFactor), Image.SCALE_SMOOTH), null, null);
        graphics.dispose();
        return targetImage;
    }

    private static double getScalingFactor(int current, int max) {
        if (current <= max) {
            return 1.0;
        }

        return ((double) max) / current;
    }

    /**
     * Copies the given BufferedImage to a new in-memory image with the given BufferedImage <code>type</code>.
     *
     * @param source     the source image
     * @param outputType the type of the output image, one of:
     *                   {@link BufferedImage#TYPE_INT_RGB},
     *                   {@link BufferedImage#TYPE_INT_ARGB},
     *                   {@link BufferedImage#TYPE_INT_ARGB_PRE},
     *                   {@link BufferedImage#TYPE_INT_BGR},
     *                   {@link BufferedImage#TYPE_3BYTE_BGR},
     *                   {@link BufferedImage#TYPE_4BYTE_ABGR},
     *                   {@link BufferedImage#TYPE_4BYTE_ABGR_PRE},
     *                   {@link BufferedImage#TYPE_BYTE_GRAY},
     *                   {@link BufferedImage#TYPE_BYTE_BINARY},
     *                   {@link BufferedImage#TYPE_BYTE_INDEXED},
     *                   {@link BufferedImage#TYPE_USHORT_GRAY},
     *                   {@link BufferedImage#TYPE_USHORT_565_RGB},
     *                   {@link BufferedImage#TYPE_USHORT_555_RGB},
     * @return a new BufferedImage, never <code>null</code>.
     */
    public static BufferedImage copyBufferedImage(BufferedImage source, int outputType) {
        BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), outputType);
        out.getGraphics().drawImage(source, 0, 0, null);
        return out;
    }

    /**
     * Converts the given BufferedImage to a known <code>type</code>, if such conversion is necessary.
     * <p>If <code>source.getType()</code> returns a non-zero value (anything but <code>TYPE_CUSTOM</code>),
     * then this method is a no-op and <code>source</code> is returned as-is.
     *
     * @param source the source image.
     * @return <code>source</code> if no conversion is necessary, or a new BufferedImage if source was a
     *         custom type.
     */
    public static BufferedImage standardizeBufferedImageType(BufferedImage source) {
        BufferedImage out;
        if (source.getType() == BufferedImage.TYPE_CUSTOM) {
            out = copyBufferedImage(source, DEFAULT_IMAGE_TYPE);
        } else {
            out = source;
        }
        return out;
    }
}
