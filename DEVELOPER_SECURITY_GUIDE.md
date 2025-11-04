# Developer Security Quick Reference

## 🎯 Quick Start

### Password Requirements (ALWAYS ENFORCE)
```java
// In DTOs
import com.neo4flix.userservice.validation.ValidPassword;

@NotBlank(message = "Password is required")
@ValidPassword  // ← Add this!
private String password;
```

**Requirements:**
- Min 8 chars
- 1 uppercase, 1 lowercase
- 1 digit, 1 special char (@$!%*?&#)

---

## 🛡️ Input Sanitization (ALWAYS SANITIZE)

```java
@Autowired
private InputSanitizer sanitizer;

// Before saving to database
user.setBio(sanitizer.sanitize(userInput.getBio()));

// Check for suspicious content
if (sanitizer.containsSuspiciousContent(input)) {
    throw new SecurityException("Invalid input detected");
}

// Sanitize file names
String safeFileName = sanitizer.sanitizeFileName(uploadedFile.getName());
```

---

## 📝 Security Logging (LOG SECURITY EVENTS)

```java
@Autowired
private SecurityAuditLogger auditLogger;

// Log authentication
auditLogger.logSuccessfulAuth(username, ipAddress, userAgent);
auditLogger.logFailedAuth(username, ipAddress, userAgent, reason);

// Log security events
auditLogger.logPasswordChange(username, ipAddress, success);
auditLogger.log2FAChange(username, enabled, ipAddress);
auditLogger.logSuspiciousActivity(username, ipAddress, type, details);
```

---

## ❌ Error Handling (DON'T EXPOSE INTERNALS)

### ✅ DO THIS:
```java
throw new ResourceNotFoundException("Resource not found");
```

### ❌ DON'T DO THIS:
```java
throw new Exception("User with ID " + userId + " not found in neo4j.users table");
```

**Why:** Attackers can learn about your system structure.

---

## 🚦 Rate Limiting (ALREADY ENABLED)

No action needed! Already protecting:
- 60 req/min for standard endpoints
- 5 req/min for auth endpoints

**Custom limits?** Modify `RateLimitingInterceptor.java`

---

## 🔍 Quick Security Checklist

Before committing code:

- [ ] Passwords use `@ValidPassword`
- [ ] User input is sanitized
- [ ] Security events are logged
- [ ] Error messages don't expose internals
- [ ] No hardcoded secrets
- [ ] SQL queries use parameters (not string concatenation)
- [ ] File uploads are validated
- [ ] Authentication required for protected endpoints

---

## 💡 Common Pitfalls

### 1. String Concatenation in Queries ❌
```java
// DON'T DO THIS
String query = "MATCH (u:User {username: '" + username + "'})";
```

### 2. Exposing Stack Traces ❌
```java
// DON'T DO THIS
catch (Exception e) {
    return ResponseEntity.badRequest().body(e.getStackTrace());
}
```

### 3. Not Validating File Uploads ❌
```java
// DON'T DO THIS
fileStorageService.save(file);

// DO THIS
String safeFileName = sanitizer.sanitizeFileName(file.getName());
if (file.getSize() > MAX_SIZE) throw new Exception("File too large");
fileStorageService.save(safeFileName, file);
```

---

## 🔧 Integration Examples

### Complete User Registration Flow
```java
public ResponseEntity<User> register(@Valid @RequestBody UserRegistrationRequest request,
                                     HttpServletRequest httpRequest) {
    // 1. Validate (automatic via @Valid and @ValidPassword)

    // 2. Sanitize
    String safeBio = sanitizer.sanitize(request.getBio());

    // 3. Create user
    User user = userService.createUser(request);

    // 4. Log security event
    auditLogger.logUserRegistration(
        user.getUsername(),
        user.getEmail(),
        httpRequest.getRemoteAddr()
    );

    return ResponseEntity.ok(user);
}
```

### Complete Authentication Flow
```java
public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                          HttpServletRequest httpRequest) {
    try {
        // Attempt authentication
        AuthResponse response = authService.authenticate(request);

        // Log success
        auditLogger.logSuccessfulAuth(
            request.getUsername(),
            httpRequest.getRemoteAddr(),
            httpRequest.getHeader("User-Agent")
        );

        return ResponseEntity.ok(response);

    } catch (BadCredentialsException e) {
        // Log failure
        auditLogger.logFailedAuth(
            request.getUsername(),
            httpRequest.getRemoteAddr(),
            httpRequest.getHeader("User-Agent"),
            "Invalid credentials"
        );

        throw e; // GlobalExceptionHandler will handle this
    }
}
```

---

## 📚 Further Reading

- OWASP Top 10: https://owasp.org/www-project-top-ten/
- Spring Security Docs: https://spring.io/projects/spring-security
- Password Guidelines: NIST SP 800-63B
- Input Validation: OWASP Input Validation Cheat Sheet

---

## 🆘 Need Help?

1. Check `SECURITY_IMPROVEMENTS.md` for detailed docs
2. Review test cases in `src/test/java/`
3. Ask team security lead
4. Consult OWASP guidelines

**Remember:** Security is everyone's responsibility!
