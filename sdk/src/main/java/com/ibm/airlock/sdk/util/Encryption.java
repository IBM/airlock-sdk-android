package com.ibm.airlock.sdk.util;

/**
 * Created by Denis Voloshin on 10/11/2017.
 */

import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


public class Encryption {

    static byte[] magic = new byte[]{0x54, 0x39, 0x71, 0x12};
    static byte[] version = new byte[]{0x00, 0x01}; // for future use

    static final int blockSize = 16;
    static final int headerSize = magic.length + version.length + blockSize;

    public static byte[] getMagic() {
        return magic;
    }

    public static byte[] getVersion() {
        return version;
    }

    byte[] key;

    public Encryption(byte[] key) {
        this.key = key;
    }

    public Encryption(String key) {
        this.key = fromB64(key);
    }

    public static byte[] fromB64(String in) {
        return Base64.decode(in, Base64.DEFAULT);
    }

    public static String toB64(byte[] in) {
        return Base64.encodeToString(in, Base64.DEFAULT);
    }

    public static byte[] fromString(String in) {
        try {
            return in.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public static String toString(byte[] in) {
        try {
            return new String(in, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public byte[] encrypt(byte[] plain) throws GeneralSecurityException, IOException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        SecureRandom randomSecureRandom = SecureRandom.getInstance("SHA1PRNG");

        byte[] ivBytes = new byte[cipher.getBlockSize()];
        randomSecureRandom.nextBytes(ivBytes);
        if (ivBytes.length != blockSize) {
            throw new GeneralSecurityException("unexpected cypher block size");
        }

        SecretKeySpec aesKey = new SecretKeySpec(key, "AES");
        IvParameterSpec iv = new IvParameterSpec(ivBytes);

        cipher.init(Cipher.ENCRYPT_MODE, aesKey, iv);
        byte[] encrypted = cipher.doFinal(plain);

        // prefix the encrypted bytes with the magic, version, and initialization vector
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(magic);
        out.write(version);
        out.write(ivBytes);
        out.write(encrypted);
        return out.toByteArray();
    }

    public byte[] decrypt(byte[] encrypted) throws GeneralSecurityException {
        if (encrypted.length <= headerSize) {
            throw new GeneralSecurityException("input size is too short");
        }

        // check the magic
        if (!Arrays.equals(Arrays.copyOfRange(encrypted, 0, magic.length), magic)) {
            throw new GeneralSecurityException("missing magic number");
        }

        // extract initialization vector and encrypted data buffer
        byte[] ivBytes = Arrays.copyOfRange(encrypted, magic.length + version.length, headerSize);
        byte[] data = Arrays.copyOfRange(encrypted, headerSize, encrypted.length);

        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        IvParameterSpec iv = new IvParameterSpec(ivBytes);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
        return cipher.doFinal(data);
    }
}

