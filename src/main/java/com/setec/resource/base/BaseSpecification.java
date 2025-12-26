package com.setec.resource.base;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class BaseSpecification<T> {

    public Specification<T> filter(FilterDto filterDto) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            //if null or empty return all data
            if (filterDto==null|| filterDto.getSpecsDto() == null || filterDto.getSpecsDto().isEmpty()) {
                return criteriaBuilder.conjunction(); // Return all data
            }

            for (SpecsDto specs : filterDto.getSpecsDto()) {
                try {
                    if (specs.getJoinTable() != null) {
                        // Handle join table
                        Join<Object, Object> join = getJoin(root, specs.getJoinTable());
                        if (!isValidColumn(join.getJavaType(), specs.getColumn())) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid column: " + specs.getColumn() + " in join table: " + specs.getJoinTable());
                        }
                        predicates.add(createPredicate(join, criteriaBuilder, specs));
                    } else {
                        // Handle non-join table
                        if (!isValidColumn(root.getJavaType(), specs.getColumn())) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid column: " + specs.getColumn());
                        }
                        predicates.add(createPredicate(root, criteriaBuilder, specs));
                    }
                } catch (ResponseStatusException e) {
                    throw e; // Re-throw specific exception
                } catch (Exception e) {
                    // Catch general exceptions and log them for debugging
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request",
                            e);
                }
            }

            // Combine predicates based on global operator (AND/OR)
            return filterDto.getGlobalOperator() == FilterDto.GlobalOperator.AND
                    ? criteriaBuilder.and(predicates.toArray(new Predicate[0]))
                    : criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        };
    }

    // Validate if the column exists in the entity class
    private boolean isValidColumn(Class<?> entityClass, String column) {
        for (Field field : getAllFields(entityClass)) {
            if (field.getName().equals(column)) {
                return true;
            }
        }
        return false;
    }

    // Get all fields from the entity class, including inherited fields
    private List<Field> getAllFields(Class<?> entityClass) {
        List<Field> fields = new ArrayList<>();
        while (entityClass != null && entityClass != Object.class) {
            fields.addAll(Arrays.asList(entityClass.getDeclaredFields()));
            entityClass = entityClass.getSuperclass();
        }
        return fields;
    }

    // Create a predicate based on the operation type and column type
    private Predicate createPredicate(From<?, ?> from, CriteriaBuilder criteriaBuilder, SpecsDto specs) {
        try {
            if (!isValidOperation(specs.getOperation())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid operation: " + specs.getOperation());
            }

            Field field = from.getJavaType().getDeclaredField(specs.getColumn());
            Class<?> fieldType = field.getType();

            switch (specs.getOperation()) {
                case EQUAL:
                    return createEqualPredicate(from, criteriaBuilder, specs, fieldType);
                case LIKE:
                    if (fieldType == String.class) {
                        return criteriaBuilder.like(criteriaBuilder.lower(from.get(specs.getColumn())), "%" + specs.getValue().toLowerCase() + "%");
                    } else {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "LIKE operation is not supported for column type: " + fieldType.getSimpleName());
                    }
                case IN:
                    return createInPredicate(from, criteriaBuilder, specs, fieldType);
                case GREATER_THAN:
                    return createGreaterThanPredicate(from, criteriaBuilder, specs, fieldType);
                case LESS_THAN:
                    return createLessThanPredicate(from, criteriaBuilder, specs, fieldType);
                case BETWEEN:
                    return createBetweenPredicate(from, criteriaBuilder, specs, fieldType);
                default:
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported operation: " + specs.getOperation());
            }
        } catch (NoSuchFieldException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid column: " + specs.getColumn());
        }
    }

    private boolean isValidOperation(SpecsDto.Operation operation) {
        return Arrays.stream(SpecsDto.Operation.values()).anyMatch(op -> op == operation);
    }

    private Predicate createEqualPredicate(From<?, ?> from, CriteriaBuilder criteriaBuilder, SpecsDto specs, Class<?> fieldType) {
        if (fieldType == String.class) {
            return criteriaBuilder.equal(from.get(specs.getColumn()), specs.getValue());
        } else if (fieldType == Integer.class || fieldType == int.class) {
            return criteriaBuilder.equal(from.get(specs.getColumn()), Integer.parseInt(specs.getValue()));
        } else if (fieldType == Double.class || fieldType == double.class) {
            return criteriaBuilder.equal(from.get(specs.getColumn()), Double.parseDouble(specs.getValue()));
        } else if (fieldType == Boolean.class || fieldType == boolean.class) {
            return criteriaBuilder.equal(from.get(specs.getColumn()), Boolean.parseBoolean(specs.getValue()));
        } else if (fieldType == LocalDate.class) {
            return criteriaBuilder.equal(from.get(specs.getColumn()), LocalDate.parse(specs.getValue()));
        } else if (fieldType == LocalTime.class) {
            return criteriaBuilder.equal(from.get(specs.getColumn()), LocalTime.parse(specs.getValue()));
        } else if (fieldType == LocalDateTime.class) {
            return criteriaBuilder.equal(from.get(specs.getColumn()), LocalDateTime.parse(specs.getValue()));
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported operation for column type: " + fieldType.getSimpleName());
        }
    }

    private Predicate createInPredicate(From<?, ?> from, CriteriaBuilder criteriaBuilder, SpecsDto specs, Class<?> fieldType) {
        if (fieldType == String.class) {
            return from.get(specs.getColumn()).in(Arrays.asList(specs.getValue().split(",")));
        } else if (fieldType == Integer.class || fieldType == int.class) {
            return from.get(specs.getColumn()).in(parseIntegers(specs.getValue()));
        } else if (fieldType == Double.class || fieldType == double.class) {
            return from.get(specs.getColumn()).in(parseDoubles(specs.getValue()));
        } else if (fieldType == LocalDate.class) {
            return from.get(specs.getColumn()).in(parseLocalDates(specs.getValue()));
        } else if (fieldType == LocalTime.class) {
            return from.get(specs.getColumn()).in(parseLocalTimes(specs.getValue()));
        } else if (fieldType == LocalDateTime.class) {
            return from.get(specs.getColumn()).in(parseLocalDateTimes(specs.getValue()));
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IN operation is not supported for column type: " + fieldType.getSimpleName());
        }
    }

    private Predicate createGreaterThanPredicate(From<?, ?> from, CriteriaBuilder criteriaBuilder, SpecsDto specs, Class<?> fieldType) {
        if (fieldType == Integer.class || fieldType == int.class) {
            return criteriaBuilder.greaterThan(from.get(specs.getColumn()), Integer.parseInt(specs.getValue()));
        } else if (fieldType == Double.class || fieldType == double.class) {
            return criteriaBuilder.greaterThan(from.get(specs.getColumn()), Double.parseDouble(specs.getValue()));
        } else if (fieldType == LocalDate.class) {
            return criteriaBuilder.greaterThan(from.get(specs.getColumn()), LocalDate.parse(specs.getValue()));
        } else if (fieldType == LocalTime.class) {
            return criteriaBuilder.greaterThan(from.get(specs.getColumn()), LocalTime.parse(specs.getValue()));
        } else if (fieldType == LocalDateTime.class) {
            return criteriaBuilder.greaterThan(from.get(specs.getColumn()), LocalDateTime.parse(specs.getValue()));
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GREATER_THAN operation is not supported for column type: " + fieldType.getSimpleName());
        }
    }

    private Predicate createLessThanPredicate(From<?, ?> from, CriteriaBuilder criteriaBuilder, SpecsDto specs, Class<?> fieldType) {
        if (fieldType == Integer.class || fieldType == int.class) {
            return criteriaBuilder.lessThan(from.get(specs.getColumn()), Integer.parseInt(specs.getValue()));
        } else if (fieldType == Double.class || fieldType == double.class) {
            return criteriaBuilder.lessThan(from.get(specs.getColumn()), Double.parseDouble(specs.getValue()));
        } else if (fieldType == LocalDate.class) {
            return criteriaBuilder.lessThan(from.get(specs.getColumn()), LocalDate.parse(specs.getValue()));
        } else if (fieldType == LocalTime.class) {
            return criteriaBuilder.lessThan(from.get(specs.getColumn()), LocalTime.parse(specs.getValue()));
        } else if (fieldType == LocalDateTime.class) {
            return criteriaBuilder.lessThan(from.get(specs.getColumn()), LocalDateTime.parse(specs.getValue()));
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "LESS_THAN operation is not supported for column type: " + fieldType.getSimpleName());
        }
    }

    private Predicate createBetweenPredicate(From<?, ?> from, CriteriaBuilder criteriaBuilder, SpecsDto specs, Class<?> fieldType) {
        String[] split = specs.getValue().split(",");
        if (split.length != 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "BETWEEN operation requires two values");
        }

        if (fieldType == Integer.class || fieldType == int.class) {
            return criteriaBuilder.between(from.get(specs.getColumn()), Integer.parseInt(split[0]), Integer.parseInt(split[1]));
        } else if (fieldType == Double.class || fieldType == double.class) {
            return criteriaBuilder.between(from.get(specs.getColumn()), Double.parseDouble(split[0]), Double.parseDouble(split[1]));
        } else if (fieldType == LocalDate.class) {
            return criteriaBuilder.between(from.get(specs.getColumn()), LocalDate.parse(split[0]), LocalDate.parse(split[1]));
        } else if (fieldType == LocalTime.class) {
            return criteriaBuilder.between(from.get(specs.getColumn()), LocalTime.parse(split[0]), LocalTime.parse(split[1]));
        } else if (fieldType == LocalDateTime.class) {
            return criteriaBuilder.between(from.get(specs.getColumn()), LocalDateTime.parse(split[0]), LocalDateTime.parse(split[1]));
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "BETWEEN operation is not supported for column type: " + fieldType.getSimpleName());
        }
    }

    private Join<Object, Object> getJoin(From<?, ?> root, String joinTable) {
        try {
            String[] joinTableSplit = joinTable.split("\\.");
            Join<Object, Object> join = root.join(joinTableSplit[0]);
            for (int i = 1; i < joinTableSplit.length; i++) {
                join = join.join(joinTableSplit[i]);
            }
            return join;
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid join table: " + joinTable, e);
        }
    }

    private List<Integer> parseIntegers(String value) {
        List<Integer> result = new ArrayList<>();
        for (String val : value.split(",")) {
            result.add(Integer.parseInt(val));
        }
        return result;
    }

    private List<Double> parseDoubles(String value) {
        List<Double> result = new ArrayList<>();
        for (String val : value.split(",")) {
            result.add(Double.parseDouble(val));
        }
        return result;
    }

    private List<LocalDate> parseLocalDates(String value) {
        List<LocalDate> result = new ArrayList<>();
        for (String val : value.split(",")) {
            result.add(LocalDate.parse(val));
        }
        return result;
    }

    private List<LocalTime> parseLocalTimes(String value) {
        List<LocalTime> result = new ArrayList<>();
        for (String val : value.split(",")) {
            result.add(LocalTime.parse(val));
        }
        return result;
    }

    private List<LocalDateTime> parseLocalDateTimes(String value) {
        List<LocalDateTime> result = new ArrayList<>();
        for (String val : value.split(",")) {
            result.add(LocalDateTime.parse(val));
        }
        return result;
    }

    @Getter
    @Setter
    public static class SpecsDto {
        private String column;
        private String value;
        private String joinTable;
        private Operation operation;

        public enum Operation {
            EQUAL, LIKE, IN, GREATER_THAN, LESS_THAN, BETWEEN
        }
    }

    @Getter
    @Setter
    public static class FilterDto {
        private List<SpecsDto> specsDto;
        private GlobalOperator globalOperator;

        public enum GlobalOperator {
            AND, OR
        }
    }
}
