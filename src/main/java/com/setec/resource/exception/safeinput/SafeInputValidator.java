package com.setec.resource.exception.safeinput;//package com.setec.online_survey.exception.safeinput;
//
//import jakarta.validation.ConstraintValidator;
//import jakarta.validation.ConstraintValidatorContext;
//
//public class SafeInputValidator implements ConstraintValidator<SafeInput, String> {
//
//    @Override
//    public void initialize(SafeInput constraintAnnotation) {
//        // Initialization code, if needed
//    }
//
//    @Override
//    public boolean isValid(String value, ConstraintValidatorContext context) {
//        if (value == null || value.trim().isEmpty()) {
//            return true; // Or false, depending on whether null or empty is acceptable
//        }
//
//        // Perform custom validation logic here
//        // Example: Check for malicious input
//        String sanitizedValue = sanitize(value);
//        return !containsMaliciousContent(sanitizedValue);
//    }
//
//    private String sanitize(String input) {
//        // Implement sanitization logic (e.g., escape special characters)
//        return input.replaceAll("[<>\"'&]", "");
//    }
//
//    private boolean containsMaliciousContent(String input) {
//
//        // Implement logic to detect malicious content
//        // Example: Simple check for SQL injection patterns
//        return input.matches(".*(SELECT|INSERT|UPDATE|DELETE|--|\\bDROP\\b).*");
//    }
//}
//
