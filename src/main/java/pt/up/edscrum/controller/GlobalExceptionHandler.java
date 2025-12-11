package pt.up.edscrum.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import pt.up.edscrum.exception.TeamValidationException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TeamValidationException.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleTeamValidationException(TeamValidationException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(body);
    }
}
