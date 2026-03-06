package com.setec.resource.util;

import com.setec.resource.base.BaseSpecification;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FilterUtils {

    public enum GlobalOperator {
        AND, OR;

        public static GlobalOperator fromString(String operator) {
            try {
                return GlobalOperator.valueOf(operator.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid global operator: " + operator);
            }
        }
    }

    public enum Operation {
        EQUAL, LIKE, IN, GREATER_THAN, LESS_THAN, BETWEEN ,   NOT_EQUAL, NOT_LIKE, NOT_IN;;

        public static Operation fromString(String operation) {
            try {
                return Operation.valueOf(operation.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid operation: " + operation);
            }
        }
    }

    public static BaseSpecification.FilterDto.GlobalOperator toGlobalOperator(String operator) {
        return BaseSpecification.FilterDto.GlobalOperator.valueOf(GlobalOperator.fromString(operator).name());
    }

    public static BaseSpecification.SpecsDto.Operation toOperation(String operation) {
        return BaseSpecification.SpecsDto.Operation.valueOf(Operation.fromString(operation).name());
    }

    public static BaseSpecification.FilterDto buildFilterDto(WebRequest request, GlobalOperator defaultGlobalOperator) {

        List<BaseSpecification.SpecsDto> specsList = new ArrayList<>();

        Map<String, String[]> parameterMap = request.getParameterMap();
        boolean hasValidParams = false;

        // Get global operator or use default
        String opParam = request.getParameter("gop");

        GlobalOperator globalOperator;

        if (opParam != null && !opParam.isEmpty()) {
            globalOperator = GlobalOperator.fromString(opParam);
        } else {
            globalOperator = defaultGlobalOperator;
        }

        // Iterate through parameters
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {

            String paramName = entry.getKey();

            String[] values = entry.getValue();

            // Skip special parameters
            if (paramName.equals("orderBy")||paramName.equals("sortBy") ||paramName.equals("gop") || paramName.equals("pageNumber") || paramName.equals("pageSize")||paramName.equals("monthYear")) {
                continue;
            }

            for (String value : values) {
                if (value != null && !value.isEmpty()) {
                    hasValidParams = true;

                    BaseSpecification.SpecsDto specs = new BaseSpecification.SpecsDto();
                    String[] parts = paramName.split("\\.");

                    if (parts.length > 1) {

                        String joinTable = String.join(".", extractJoinTable(parts));

                        specs.setJoinTable(joinTable);

                        specs.setColumn(extractColumnName(parts[parts.length - 1]));

                    } else {
                        specs.setColumn(extractColumnName(parts[0]));
                    }

                    specs.setValue(value);

                    specs.setOperation(detectOperation(paramName));

                    specsList.add(specs);
                }
            }
        }

        if (!hasValidParams) {
            return null;
        }

        BaseSpecification.FilterDto filterDto = new BaseSpecification.FilterDto();

        filterDto.setSpecsDto(specsList);

        filterDto.setGlobalOperator(toGlobalOperator(globalOperator.name()));

        return filterDto;
    }

    private static BaseSpecification.SpecsDto.Operation detectOperation(String paramName) {

        if (paramName.endsWith("_like")) {

            return BaseSpecification.SpecsDto.Operation.LIKE;

        } else if (paramName.endsWith("_nlike")) {

            return BaseSpecification.SpecsDto.Operation.NOT_LIKE;

        } else if (paramName.endsWith("_in")) {

            return BaseSpecification.SpecsDto.Operation.IN;

        } else if (paramName.endsWith("_nin")) {

            return BaseSpecification.SpecsDto.Operation.NOT_IN;

        } else if (paramName.endsWith("_gt")) {

            return BaseSpecification.SpecsDto.Operation.GREATER_THAN;

        } else if (paramName.endsWith("_lt")) {

            return BaseSpecification.SpecsDto.Operation.LESS_THAN;

        } else if (paramName.endsWith("_between")) {

            return BaseSpecification.SpecsDto.Operation.BETWEEN;

        } else if (paramName.endsWith("_ne")) {

            return BaseSpecification.SpecsDto.Operation.NOT_EQUAL;

        } else {

            return BaseSpecification.SpecsDto.Operation.EQUAL;

        }
    }


    private static String extractColumnName(String paramName) {

        if (paramName.endsWith("_like")) {

            return paramName.substring(0, paramName.length() - 5);

        } else if (paramName.endsWith("_nlike")) {

            return paramName.substring(0, paramName.length() - 6);

        } else if (paramName.endsWith("_in")) {

            return paramName.substring(0, paramName.length() - 3);

        } else if (paramName.endsWith("_nin")) {

            return paramName.substring(0, paramName.length() - 4);

        } else if (paramName.endsWith("_gt")) {

            return paramName.substring(0, paramName.length() - 3);

        } else if (paramName.endsWith("_lt")) {

            return paramName.substring(0, paramName.length() - 3);

        } else if (paramName.endsWith("_between")) {

            return paramName.substring(0, paramName.length() - 8);

        } else if (paramName.endsWith("_ne")) {

            return paramName.substring(0, paramName.length() - 3);

        } else {

            return paramName;

        }
    }


    private static String[] extractJoinTable(String[] parts) {

        // Extract all parts except the last one which is the column with operation
        String[] joinTableParts = new String[parts.length - 1];

        System.arraycopy(parts, 0, joinTableParts, 0, parts.length - 1);

        return joinTableParts;
    }

    @SafeVarargs
    public static <T> Specification<T> combineSpecifications(Specification<T>... specifications) {
        Specification<T> combinedSpecification = specifications[0];
        for (int i = 1; i < specifications.length; i++) {
            combinedSpecification = combinedSpecification.and(specifications[i]);
        }
        return combinedSpecification;
    }

    public static BaseSpecification.FilterDto buildFilterDtoFromParams(String globalOperator, Map<String, String> params) {
        List<BaseSpecification.SpecsDto> specsList = new ArrayList<>();
        boolean hasValidParams = false;

        // Get global operator
        GlobalOperator operator = GlobalOperator.fromString(globalOperator);

        // Iterate through parameters
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String paramName = entry.getKey();
            String value = entry.getValue();

            if (value != null && !value.isEmpty()) {
                hasValidParams = true;

                BaseSpecification.SpecsDto specs = new BaseSpecification.SpecsDto();
                String[] parts = paramName.split("\\.");

                if (parts.length > 1) {
                    String joinTable = String.join(".", extractJoinTable(parts));
                    specs.setJoinTable(joinTable);
                    specs.setColumn(extractColumnName(parts[parts.length - 1]));
                } else {
                    specs.setColumn(extractColumnName(parts[0]));
                }

                specs.setValue(value);
                specs.setOperation(detectOperation(paramName));
                specsList.add(specs);
            }
        }

        if (!hasValidParams) {
            return null;
        }

        BaseSpecification.FilterDto filterDto = new BaseSpecification.FilterDto();
        filterDto.setSpecsDto(specsList);
        filterDto.setGlobalOperator(toGlobalOperator(operator.name()));
        return filterDto;
    }

    public static <T> Specification<T> buildSpecification(String fieldName, String operation, String value, BaseSpecification<T> baseSpecification) {
        // Create param for filter
        Map<String, String> param = new HashMap<>();
        param.put(fieldName, value);

        // Build filter dto from parameter
        BaseSpecification.FilterDto filterDto = buildFilterDtoFromParams(operation, param);

        // Create a dynamic query specification for filtering entities based on the criteria provided
        return baseSpecification.filter(filterDto);
    }


}
