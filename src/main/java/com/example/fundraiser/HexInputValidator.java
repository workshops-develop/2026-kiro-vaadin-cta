package com.example.fundraiser;

import java.util.regex.Pattern;

/**
 * Utility class for validating and sanitizing hexadecimal key input.
 * All methods are stateless and thread-safe.
 */
public class HexInputValidator {

    public static final int REQUIRED_LENGTH = 240;
    public static final Pattern HEX_PATTERN = Pattern.compile("^[0-9a-fA-F]{240}$");

    private HexInputValidator() {
        // Utility class — no instantiation
    }

    /**
     * Returns {@code true} iff {@code input} is non-null and is exactly 240
     * hexadecimal characters ({@code [0-9a-fA-F]}).
     */
    public static boolean isValid(String input) {
        return input != null && HEX_PATTERN.matcher(input).matches();
    }

    /**
     * Removes all non-hexadecimal characters from {@code input} and truncates
     * the result to at most {@value #REQUIRED_LENGTH} characters.
     *
     * @param input the raw string to sanitize; {@code null} is treated as an
     *              empty string and returns {@code ""}
     * @return a string containing only {@code [0-9a-fA-F]}, at most 240 chars
     */
    public static String stripNonHex(String input) {
        if (input == null) {
            return "";
        }
        String stripped = input.replaceAll("[^0-9a-fA-F]", "");
        return stripped.substring(0, Math.min(stripped.length(), REQUIRED_LENGTH));
    }
}
