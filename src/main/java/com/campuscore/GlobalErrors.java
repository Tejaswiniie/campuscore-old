package com.campuscore;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** Backend authorization failures inside controllers and services show the "no access" page with status 403. */
@ControllerAdvice
public class GlobalErrors {

    @ExceptionHandler(AccessDeniedException.class)
    public String denied(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        return "denied";
    }

    /** The upload was bigger than the limit set for the server itself; show the form again with a notice. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String tooLarge() {
        return "redirect:/faculty/assignments?tooLarge";
    }
}
