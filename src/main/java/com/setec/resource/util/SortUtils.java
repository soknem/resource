package com.setec.resource.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SortUtils {

    // Cache fields to avoid repeated reflection overhead
    private static final ConcurrentHashMap<Class<?>, Set<String>> fieldCache = new ConcurrentHashMap<>();

    /**
     * Validates if the sortBy string exists as a field in the provided class.
     * Supports fields from the class and its superclasses (like Auditable).
     */
    public static void validateSortBy(Class<?> domainClass, String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sort column cannot be null or empty");
        }

        // Get or compute the valid field names for the class
        Set<String> validFields = fieldCache.computeIfAbsent(domainClass, clazz -> {
            Set<String> fields = new HashSet<>();
            
            // Traverse the class hierarchy to include fields from Auditable/Base classes
            Class<?> current = clazz;
            while (current != null && current != Object.class) {
                Arrays.stream(current.getDeclaredFields())
                        .forEach(field -> fields.add(field.getName()));
                current = current.getSuperclass();
            }
            return fields;
        });

        if (!validFields.contains(sortBy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                String.format("Invalid sort column: '%s'. Valid columns are: %s", sortBy, validFields));
        }
    }
}