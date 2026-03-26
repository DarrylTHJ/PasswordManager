package com.example.passwordmanager;

import android.util.Base64;

public class EncryptionUtils {

    // Scrambles the text
    public static String encrypt(String input) {
        if (input == null || input.isEmpty()) return input;
        return Base64.encodeToString(input.getBytes(), Base64.DEFAULT);
    }

    // Unscrambles the text
    public static String decrypt(String input) {
        if (input == null || input.isEmpty()) return input;
        try {
            byte[] decodedBytes = Base64.decode(input, Base64.DEFAULT);
            return new String(decodedBytes);
        } catch (IllegalArgumentException e) {
            // Safety net: If it fails to decrypt (e.g., reading your older unencrypted tests), return the raw text
            return input;
        }
    }
}