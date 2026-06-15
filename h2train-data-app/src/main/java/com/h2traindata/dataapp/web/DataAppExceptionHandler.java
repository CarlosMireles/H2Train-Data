package com.h2traindata.dataapp.web;

import com.h2traindata.dataapp.dataset.exception.InvalidDatasetQueryException;
import com.h2traindata.dataapp.dataset.exception.UnsupportedMetricException;
import java.util.NoSuchElementException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class DataAppExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> badRequest(IllegalArgumentException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(InvalidDatasetQueryException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalidDatasetQuery(InvalidDatasetQueryException exception) {
        return error(exception.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> missingParameter(MissingServletRequestParameterException exception) {
        return Map.of("error", exception.getParameterName() + " is required");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalidParameter(MethodArgumentTypeMismatchException exception) {
        return error(exception.getName() + " has an invalid value");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalidBody(HttpMessageNotReadableException exception) {
        return error("Request body is invalid");
    }

    @ExceptionHandler(UnsupportedMetricException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> unsupportedMetric(UnsupportedMetricException exception) {
        return error(exception.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> notFound(NoSuchElementException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> unexpected(Exception exception) {
        return error("Unexpected server error");
    }

    private Map<String, String> error(String message) {
        return Map.of("error", message == null ? "Invalid request" : message);
    }
}
