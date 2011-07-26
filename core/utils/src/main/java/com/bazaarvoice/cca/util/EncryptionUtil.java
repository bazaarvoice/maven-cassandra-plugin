package com.bazaarvoice.cca.util;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.io.UnsupportedEncodingException;

public class EncryptionUtil {

    //Use AES for all encryption.  AES has the following properties which are good for us:
    //  1. Symmetric
    //  2. Very Fast
    //  3. Block-Based encryption
    //  4. Strong (No known effective cryptanalysis as of 2008)
    //  5. Built-in JCA support (AES is unpatented and in the public domain in all countries)
    private static final String CIPHER_ALGORITHM = "AES";

    //Default key length is 128
    //Key lengths from 32 to 448 are allowed
    private static final int KEY_LENGTH = 128;


    public static String encryptToHex(String input, SecretKey key) {
        return toHex(encrypt(input, key));
    }
    
    public static String encryptToHex(String input, String hexKey) {
        return toHex(encrypt(input, hexKey));
    }

    public static byte[] encrypt(byte[] input, String hexKey){
        return encrypt(input, hexToKey(hexKey));
    }

    private static byte[] encrypt(String input, String hexKey){
        return encrypt(input, hexToKey(hexKey));
    }

    private static byte[] encrypt(String input, SecretKey key) {
        try {
            return encrypt(input.getBytes("UTF8"), key);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] encrypt(byte[] input, SecretKey key) {
        if (input == null) {
            return null;
        }
        Cipher cipher = getEncryptingCipher(key);
        try {
            return cipher.doFinal(input);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String decryptToString(String hexInput, String hexKey) {
        return decryptToString(hexInput, hexToKey(hexKey));
    }

    public static String decryptToString(String hexInput, SecretKey key) {
        return new String(decrypt(fromHex(hexInput), key));
    }

    public static byte[] decrypt(byte[] input, String hexKey) {
        return decrypt(input, hexToKey(hexKey));
    }
    
    private static byte[] decrypt(byte[] input, SecretKey key) {
        Cipher cipher = getDecryptingCipher(key);
        try {
            return cipher.doFinal(input);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    private static Cipher getEncryptingCipher(SecretKey key) {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    private static Cipher getDecryptingCipher(SecretKey key) {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            return cipher;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public static SecretKey generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(CIPHER_ALGORITHM);
            keyGenerator.init(KEY_LENGTH);
            return keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static SecretKey hexToKey(String hex) {
        try {
            return new SecretKeySpec(Hex.decodeHex(hex.toCharArray()), CIPHER_ALGORITHM);
        } catch (DecoderException e) {
            throw new RuntimeException(e);
        }
    }

    private static String toHex(byte[] bytes) {
        return new String(Hex.encodeHex(bytes));
    }

    private static byte[] fromHex(String hex) {
        try {
            return Hex.decodeHex(hex.toCharArray());
        } catch (DecoderException e) {
            throw new RuntimeException(e);
        }
    }

}
