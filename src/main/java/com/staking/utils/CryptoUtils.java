package com.staking.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CryptoUtils {

    /**
     * This method converts the source string to sha256 encoded hex string
     * @param src source string to be converted
     * @return hex string
     */
    public static String applySha256(String src){
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            //Applies sha256 to our input,
            byte[] hash = digest.digest(src.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte hash1 : hash) {
                String hex = Integer.toHexString(0xff & hash1);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        }catch (NoSuchAlgorithmException ignored){
            throw new RuntimeException("Never happens");
        }
    }

    public static byte[] applyRawSha256(String src){
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            //Applies sha256 to our input,
            return digest.digest(src.getBytes(StandardCharsets.UTF_8));
        }catch (NoSuchAlgorithmException ignored){
            throw new RuntimeException("Never happens");
        }
    }


}

