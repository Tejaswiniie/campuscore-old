package com.campuscore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Builds the HTTP response for an attachment download. The download keeps the original file name. */
final class Downloads {
    private Downloads() {}

    static ResponseEntity<Resource> of(AssignmentService.Download d) {
        ContentDisposition disposition = ContentDisposition.attachment()
            .filename(d.fileName(), StandardCharsets.UTF_8).build();
        ResponseEntity.BodyBuilder response = ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .header("X-Content-Type-Options", "nosniff")
            .cacheControl(CacheControl.noStore())
            .contentType(MediaType.parseMediaType(d.contentType()));
        try {
            response.contentLength(Files.size(d.path()));
        } catch (IOException e) {
            // the length is simply not announced
        }
        return response.body(new FileSystemResource(d.path()));
    }
}
