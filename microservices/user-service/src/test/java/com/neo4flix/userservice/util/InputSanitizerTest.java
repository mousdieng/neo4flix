package com.neo4flix.userservice.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InputSanitizer
 */
class InputSanitizerTest {

    private InputSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new InputSanitizer();
    }

    @Test
    void testSanitizeScriptTag() {
        String malicious = "<script>alert('XSS')</script>Hello";
        String sanitized = sanitizer.sanitize(malicious);

        assertFalse(sanitized.contains("<script>"),
                "Sanitized output should not contain script tags");
        assertFalse(sanitized.contains("alert"),
                "Sanitized output should not contain script content");
    }

    @Test
    void testSanitizeHTMLTags() {
        String htmlInput = "<div>Hello <b>World</b></div>";
        String sanitized = sanitizer.sanitize(htmlInput);

        assertFalse(sanitized.contains("<"),
                "Sanitized output should not contain < character");
        assertFalse(sanitized.contains(">"),
                "Sanitized output should not contain > character");
        assertTrue(sanitized.contains("Hello"),
                "Sanitized output should contain text content");
    }

    @Test
    void testSanitizeJavaScript() {
        String jsInput = "javascript:alert('XSS')";
        String sanitized = sanitizer.sanitize(jsInput);

        assertFalse(sanitized.contains("javascript:"),
                "Sanitized output should not contain javascript: protocol");
    }

    @Test
    void testSanitizeSpecialCharacters() {
        String specialChars = "Test & <test> \"quotes\" 'single'";
        String sanitized = sanitizer.sanitize(specialChars);

        assertTrue(sanitized.contains("&amp;"),
                "Ampersand should be encoded");
        assertTrue(sanitized.contains("&lt;"),
                "Less than should be encoded");
        assertTrue(sanitized.contains("&gt;"),
                "Greater than should be encoded");
        assertTrue(sanitized.contains("&quot;"),
                "Double quotes should be encoded");
        assertTrue(sanitized.contains("&#x27;"),
                "Single quotes should be encoded");
    }

    @Test
    void testSanitizeForDb() {
        String sqlInjection = "admin' OR '1'='1";
        String sanitized = sanitizer.sanitizeForDb(sqlInjection);

        assertFalse(sanitized.contains("--"),
                "SQL comments should be removed");
        assertTrue(sanitized.contains("''"),
                "Single quotes should be escaped");
    }

    @Test
    void testValidUsername() {
        assertTrue(sanitizer.isValidUsername("john_doe"),
                "Valid username should be accepted");
        assertTrue(sanitizer.isValidUsername("user123"),
                "Alphanumeric username should be accepted");
        assertTrue(sanitizer.isValidUsername("test-user"),
                "Hyphenated username should be accepted");

        assertFalse(sanitizer.isValidUsername("ab"),
                "Username too short should be rejected");
        assertFalse(sanitizer.isValidUsername("user@name"),
                "Username with special characters should be rejected");
        assertFalse(sanitizer.isValidUsername("user name"),
                "Username with spaces should be rejected");
    }

    @Test
    void testValidEmail() {
        assertTrue(sanitizer.isValidEmail("user@example.com"),
                "Valid email should be accepted");
        assertTrue(sanitizer.isValidEmail("test.user+tag@example.co.uk"),
                "Complex valid email should be accepted");

        assertFalse(sanitizer.isValidEmail("invalid"),
                "Invalid email without @ should be rejected");
        assertFalse(sanitizer.isValidEmail("user@"),
                "Invalid email without domain should be rejected");
        assertFalse(sanitizer.isValidEmail("@example.com"),
                "Invalid email without user should be rejected");
    }

    @Test
    void testContainsSuspiciousContent() {
        assertTrue(sanitizer.containsSuspiciousContent("<script>alert('XSS')</script>"),
                "Script tag should be detected as suspicious");
        assertTrue(sanitizer.containsSuspiciousContent("' OR '1'='1"),
                "SQL injection pattern should be detected");
        assertTrue(sanitizer.containsSuspiciousContent("javascript:void(0)"),
                "JavaScript protocol should be detected");

        assertFalse(sanitizer.containsSuspiciousContent("Normal text"),
                "Normal text should not be suspicious");
    }

    @Test
    void testSanitizeFileName() {
        assertEquals("test_file.txt",
                sanitizer.sanitizeFileName("test file.txt"),
                "Spaces in filename should be replaced");

        assertEquals("document.pdf",
                sanitizer.sanitizeFileName("../../../etc/passwd"),
                "Directory traversal should be removed");

        assertEquals("my_file_name.doc",
                sanitizer.sanitizeFileName("my@file$name.doc"),
                "Special characters should be replaced");
    }

    @Test
    void testSanitizeNullAndEmpty() {
        assertNull(sanitizer.sanitize(null),
                "Null input should return null");
        assertEquals("", sanitizer.sanitize(""),
                "Empty input should return empty");

        assertNull(sanitizer.sanitizeForDb(null),
                "Null DB input should return null");
        assertEquals("", sanitizer.sanitizeForDb(""),
                "Empty DB input should return empty");
    }

    @Test
    void testSanitizeTrimsWhitespace() {
        String withWhitespace = "  Hello World  ";
        String sanitized = sanitizer.sanitize(withWhitespace);

        assertEquals("Hello World", sanitized,
                "Whitespace should be trimmed");
    }
}
