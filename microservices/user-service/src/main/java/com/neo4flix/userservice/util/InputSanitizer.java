package com.neo4flix.userservice.util;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Utility class for sanitizing user input to prevent XSS and injection attacks
 */
@Component
public class InputSanitizer {

    // Patterns for detecting malicious input
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile("('|(\\-\\-)|(;)|(\\|\\|)|(\\*))", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern JAVASCRIPT_PATTERN = Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE);

    /**
     * Sanitize input by removing potentially dangerous characters
     */
    public String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String sanitized = input;

        // Remove HTML/Script tags
        sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = HTML_TAG_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = JAVASCRIPT_PATTERN.matcher(sanitized).replaceAll("");

        // Encode special characters
        sanitized = sanitized
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;")
                .replace("/", "&#x2F;");

        return sanitized.trim();
    }

    /**
     * Sanitize for database query (basic protection)
     */
    public String sanitizeForDb(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // Remove SQL injection attempts
        String sanitized = input;
        sanitized = sanitized.replace("'", "''"); // Escape single quotes
        sanitized = sanitized.replace("--", "");  // Remove SQL comments
        sanitized = sanitized.replace(";", "");   // Remove statement terminators

        return sanitized.trim();
    }

    /**
     * Validate username format
     */
    public boolean isValidUsername(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }

        // Username should only contain alphanumeric characters, underscores, and hyphens
        return username.matches("^[a-zA-Z0-9_-]{3,50}$");
    }

    /**
     * Validate email format (basic check)
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }

        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    /**
     * Check if input contains suspicious patterns
     */
    public boolean containsSuspiciousContent(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        return SCRIPT_PATTERN.matcher(input).find() ||
               SQL_INJECTION_PATTERN.matcher(input).find() ||
               JAVASCRIPT_PATTERN.matcher(input).find();
    }

    /**
     * Sanitize file names
     */
    public String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return fileName;
        }

        // Remove directory traversal attempts
        String sanitized = fileName.replace("../", "").replace("..\\", "");

        // Remove special characters except dots and hyphens
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9.\\-_]", "_");

        return sanitized;
    }
}
