package com.setec.resource.exception;

import com.setec.resource.base.BasedError;
import com.setec.resource.base.BasedErrorResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ValidationException {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    BasedErrorResponse handleValidationErrors(MethodArgumentNotValidException ex) {
        List<Map<String, Object>> errors = new ArrayList<>();

        ex.getBindingResult().getFieldErrors().forEach(fieldError -> {
            Map<String, Object> error = new HashMap<>();
            error.put("field", fieldError.getField());
            error.put("reason", fieldError.getDefaultMessage());
            errors.add(error);
        });

        BasedError<List<Map<String, Object>>> basedError = new BasedError<>();
        basedError.setCode(HttpStatus.BAD_REQUEST.getReasonPhrase());
        basedError.setDescription(errors);

        return new BasedErrorResponse(basedError);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public BasedErrorResponse handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        List<Map<String, Object>> errors = new ArrayList<>();
        Map<String, Object> errorDetail = new HashMap<>();
        errorDetail.put("field", extractFieldNameFromMessage(ex.getMostSpecificCause().getMessage()));
        errorDetail.put("reason", "Invalid format");
        errors.add(errorDetail);

        BasedError<List<Map<String, Object>>> basedError = new BasedError<>();
        basedError.setCode(HttpStatus.BAD_REQUEST.getReasonPhrase());
        basedError.setDescription(errors);

        return new BasedErrorResponse(basedError);
    }

    @ExceptionHandler(QueryTimeoutException.class)
    @ResponseStatus(HttpStatus.REQUEST_TIMEOUT)
    public BasedErrorResponse handleQueryTimeoutException(QueryTimeoutException ex) {
        List<Map<String, Object>> errors = new ArrayList<>();
        Map<String, Object> errorDetail = new HashMap<>();
        errorDetail.put("field", "database");
        errorDetail.put("reason", "Query timed out");
        errors.add(errorDetail);

        BasedError<List<Map<String, Object>>> basedError = new BasedError<>();
        basedError.setCode(HttpStatus.REQUEST_TIMEOUT.getReasonPhrase());
        basedError.setDescription(errors);

        return new BasedErrorResponse(basedError);
    }

    @ExceptionHandler({DataAccessException.class, DataAccessResourceFailureException.class})
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public BasedErrorResponse handleDatabaseException(DataAccessException ex) {
        List<Map<String, Object>> errors = new ArrayList<>();
        Map<String, Object> errorDetail = new HashMap<>();
        errorDetail.put("field", "database");
        errorDetail.put("cause", ex.getMostSpecificCause());
        errorDetail.put("reason", "Database connectivity issue: "+ ex.getMessage());
        errors.add(errorDetail);

        BasedError<List<Map<String, Object>>> basedError = new BasedError<>();
        basedError.setCode(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase());
        basedError.setDescription(errors);

        return new BasedErrorResponse(basedError);
    }

    private String extractFieldNameFromMessage(String message) {
//        String fieldName = "json body";
//        String fieldPattern = "\\[\"(.*?)\"\\]";
//        Pattern pattern = Pattern.compile(fieldPattern);
//        Matcher matcher = pattern.matcher(message);
//
//        if (matcher.find()) {
//            fieldName = matcher.group(1);
//        }
//
//        return fieldName;
//
        return message;
    }
}
