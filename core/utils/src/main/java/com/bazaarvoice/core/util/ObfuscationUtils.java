package com.bazaarvoice.core.util;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.security.spec.KeySpec;

/**
 * Insecure encryption and decryption facilities.
 */
public abstract class ObfuscationUtils {

    // 8-byte salt for PBEWithMD5AndDES
    private static final byte[] SALT = new byte[]{
            0x41, 0xb, -0x60, 0x51, 0xb, -0x57, 0x3d, -0x7
    };
    private static final int ITERATION_COUNT = 19;

    public static Cipher newCipher(String passphrase, int mode) throws Exception {
        // Create an encryption key by combining the passphrase, salt and iteration count.
        KeySpec keySpec = new PBEKeySpec(passphrase.toCharArray(), SALT, ITERATION_COUNT);
        SecretKey secretKey = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);
        Cipher cipher = Cipher.getInstance(secretKey.getAlgorithm());
        cipher.init(mode, secretKey, new PBEParameterSpec(SALT, ITERATION_COUNT));
        return cipher;
    }

    /** Encrypts a string and returns a base64-encoded result. */
    public static String encryptString(String passphrase, String string) throws Exception {
        return encrypt64(passphrase, string.getBytes("UTF8"));
    }

    /** Decrypts a string encrypted using encryptString(). */
    public static String decryptString(String passphrase, String string) throws Exception {
        return new String(decrypt64(passphrase, string), "UTF8");
    }

    /** Encrypts a byte array and returns a base64-encoded result. */
    public static String encrypt64(String passphrase, byte[] bytes) throws Exception {
        return new String(Base64.encodeBase64(encrypt(passphrase, bytes)), "US-ASCII");
    }

    /** Decrypts a byte array encrypted using encrypt64(). */
    public static byte[] decrypt64(String passphrase, String string) throws Exception {
        return decrypt(passphrase, Base64.decodeBase64(string.getBytes("US-ASCII")));
    }

    public static byte[] encrypt(String passphrase, byte[] bytes) throws Exception {
        return newCipher(passphrase, Cipher.ENCRYPT_MODE).doFinal(bytes);
    }

    public static byte[] decrypt(String passphrase, byte[] bytes) throws Exception {
        return newCipher(passphrase, Cipher.DECRYPT_MODE).doFinal(bytes);
    }
}
