package com.campuscore;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/** Backend authorization failures inside controllers and services show the "no access" page with status 403. */
@ControllerAdvice
public class GlobalErrors {

    @ExceptionHandler(AccessDeniedException.class)
    public String denied(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        return "denied";
    }
}
