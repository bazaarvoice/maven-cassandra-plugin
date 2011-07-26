package com.bazaarvoice.cca.util;

import com.bazaarvoice.core.util.BVStringUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Purpose of this class is to provide better implementations of some of the methods in the Apache Commons library that uses unqualified
 * calls to String.getBytes().  This unsafely relies upon the default encoding of the JVM.  We want to specifically get the bytes in the UTF-8
 * character set.
 */
public abstract class DigestUtils extends org.apache.commons.codec.digest.DigestUtils {

    public static MessageDigest newMD5Digest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm should always exist.", e);
        }
    }

    public static byte[] md5(String data) {
        return md5(BVStringUtils.getBytesUTF8(data));
    }

    public static String md5Hex(String data) {
        return md5Hex(BVStringUtils.getBytesUTF8(data));
    }

    public static byte[] sha(String data) {
        return sha(BVStringUtils.getBytesUTF8(data));
    }

    public static String shaHex(String data) {
        return shaHex(BVStringUtils.getBytesUTF8(data));
    }
}
