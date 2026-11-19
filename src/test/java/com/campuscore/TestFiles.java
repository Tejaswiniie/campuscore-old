package com.campuscore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.mock.web.MockMultipartFile;

/** Small valid documents for the attachment tests. */
final class TestFiles {
    private TestFiles() {}

    static byte[] pdf() {
        return "%PDF-1.4\n1 0 obj\n<< >>\nendobj\ntrailer\n<< >>\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
    }

    static byte[] txt() {
        return "Notes for the class.\nSecond line.\n".getBytes(StandardCharsets.UTF_8);
    }

    /** An Office file is a ZIP with [Content_Types].xml and a folder such as word/. */
    static byte[] office(String folder) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(out)) {
                zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
                zip.write("<Types/>".getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
                zip.putNextEntry(new ZipEntry(folder + "document.xml"));
                zip.write("<doc/>".getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static byte[] docx() { return office("word/"); }
    static byte[] pptx() { return office("ppt/"); }
    static byte[] xlsx() { return office("xl/"); }

    static MockMultipartFile file(String name, byte[] data) {
        return new MockMultipartFile("file", name, "application/octet-stream", data);
    }
}
