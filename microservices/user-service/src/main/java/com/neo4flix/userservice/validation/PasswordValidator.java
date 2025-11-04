package com.neo4flix.userservice.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Validator implementation for password complexity requirements
 */
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    // Password pattern:
    // - At least 8 characters
    // - At least one uppercase letter
    // - At least one lowercase letter
    // - At least one digit
    // - At least one special character
    private static final String PASSWORD_PATTERN =
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$";

    private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);

    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        // Null or empty passwords are handled by @NotBlank
        if (password == null || password.isEmpty()) {
            return true;
        }

        // Validate password against pattern
        boolean isValid = pattern.matcher(password).matches();

        if (!isValid) {
            // Provide detailed error message
            context.disableDefaultConstraintViolation();

            String errorMessage = buildDetailedErrorMessage(password);
            context.buildConstraintViolationWithTemplate(errorMessage)
                   .addConstraintViolation();
        }

        return isValid;
    }

    /**
     * Build a detailed error message explaining what's missing
     */
    private String buildDetailedErrorMessage(String password) {
        StringBuilder errors = new StringBuilder("Password requirements not met: ");
        boolean hasError = false;

        if (password.length() < 8) {
            errors.append("minimum 8 characters");
            hasError = true;
        }

        if (!password.matches(".*[a-z].*")) {
            if (hasError) errors.append(", ");
            errors.append("one lowercase letter");
            hasError = true;
        }

        if (!password.matches(".*[A-Z].*")) {
            if (hasError) errors.append(", ");
            errors.append("one uppercase letter");
            hasError = true;
        }

        if (!password.matches(".*\\d.*")) {
            if (hasError) errors.append(", ");
            errors.append("one digit");
            hasError = true;
        }

        if (!password.matches(".*[@$!%*?&#].*")) {
            if (hasError) errors.append(", ");
            errors.append("one special character (@$!%*?&#)");
        }

        return errors.toString();
    }
}
