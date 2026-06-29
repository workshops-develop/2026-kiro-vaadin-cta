package com.example.fundraiser;

import java.security.SecureRandom;

/**
 * Utility class for generating cryptographically random hexadecimal strings.
 * All methods are stateless and thread-safe.
 */
public class HexStringGenerator {

    private static final int LENGTH = 240;
    private static final String HEX_CHARS = "0123456789abcdef";
    private static final SecureRandom RANDOM = new SecureRandom();

    private HexStringGenerator() {
        // Utility class — no instantiation
    }

    /**
     * Generates a cryptographically random lowercase hexadecimal string of
     * exactly {@value #LENGTH} characters.
     *
     * @return a 240-character string containing only {@code [0-9a-f]}
     */
    public static String generate() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(HEX_CHARS.charAt(RANDOM.nextInt(HEX_CHARS.length())));
        }
        return sb.toString();
    }
}
