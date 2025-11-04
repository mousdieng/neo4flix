package com.neo4flix.userservice.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PasswordValidator
 */
class PasswordValidatorTest {

    private PasswordValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder builder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validator = new PasswordValidator();

        // Mock the context for detailed error messages
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
        when(builder.addConstraintViolation()).thenReturn(context);
    }

    @Test
    void testValidPassword() {
        // Valid password with all requirements
        String validPassword = "SecurePass123!";
        assertTrue(validator.isValid(validPassword, context),
                "Password should be valid when all requirements are met");
    }

    @Test
    void testPasswordTooShort() {
        // Password less than 8 characters
        String shortPassword = "Ab1!";
        assertFalse(validator.isValid(shortPassword, context),
                "Password should be invalid when less than 8 characters");
    }

    @Test
    void testPasswordWithoutUppercase() {
        // Password without uppercase letter
        String noUppercase = "lowercase123!";
        assertFalse(validator.isValid(noUppercase, context),
                "Password should be invalid without uppercase letter");
    }

    @Test
    void testPasswordWithoutLowercase() {
        // Password without lowercase letter
        String noLowercase = "UPPERCASE123!";
        assertFalse(validator.isValid(noLowercase, context),
                "Password should be invalid without lowercase letter");
    }

    @Test
    void testPasswordWithoutDigit() {
        // Password without digit
        String noDigit = "PasswordWithoutDigit!";
        assertFalse(validator.isValid(noDigit, context),
                "Password should be invalid without digit");
    }

    @Test
    void testPasswordWithoutSpecialCharacter() {
        // Password without special character
        String noSpecial = "Password123";
        assertFalse(validator.isValid(noSpecial, context),
                "Password should be invalid without special character");
    }

    @Test
    void testNullPassword() {
        // Null password should be valid (handled by @NotBlank)
        assertTrue(validator.isValid(null, context),
                "Null password should be handled by @NotBlank annotation");
    }

    @Test
    void testEmptyPassword() {
        // Empty password should be valid (handled by @NotBlank)
        assertTrue(validator.isValid("", context),
                "Empty password should be handled by @NotBlank annotation");
    }

    @Test
    void testPasswordWithAllSpecialCharacters() {
        // Test with each allowed special character
        String[] validPasswords = {
                "Password1@",
                "Password1$",
                "Password1!",
                "Password1%",
                "Password1*",
                "Password1?",
                "Password1&",
                "Password1#"
        };

        for (String password : validPasswords) {
            assertTrue(validator.isValid(password, context),
                    "Password with special character '" + password.charAt(password.length() - 1) + "' should be valid");
        }
    }

    @Test
    void testComplexValidPassword() {
        // Complex password with multiple special characters
        String complexPassword = "MyV3ry$ecur3P@ssw0rd!";
        assertTrue(validator.isValid(complexPassword, context),
                "Complex password should be valid");
    }

    @Test
    void testPasswordExactlyEightCharacters() {
        // Password with exactly 8 characters
        String eightChars = "Pass123!";
        assertTrue(validator.isValid(eightChars, context),
                "Password with exactly 8 characters should be valid");
    }

    @Test
    void testPasswordWithSpaces() {
        // Password with spaces (not allowed by pattern)
        String withSpaces = "Pass word123!";
        assertFalse(validator.isValid(withSpaces, context),
                "Password with spaces should be invalid");
    }
}
