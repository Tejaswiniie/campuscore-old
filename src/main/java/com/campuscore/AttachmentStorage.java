package com.campuscore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Enumeration;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Stores assignment attachments on disk, outside the application JAR and outside the static resources.
 *
 * - The uploaded file name is never used as a path. Every file gets a random name made here.
 * - The file type is checked twice: by extension and by looking at the real content.
 * - Files can only be read or deleted through a stored name that matches the exact pattern this class generates.
 */
@Component
public class AttachmentStorage {

    /** What was stored. The original name is only information for the user; the stored name is generated here. */
    public record StoredFile(String originalName, String storedName, String contentType, long size) {}

    private static final Set<String> EXTENSIONS = Set.of("pdf", "docx", "pptx", "xlsx", "txt");
    private static final Pattern STORED_NAME = Pattern.compile(
        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(pdf|docx|pptx|xlsx|txt)");

    private static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final Path root;
    private final long maxBytes;

    public AttachmentStorage(@Value("${app.upload.dir:./data/uploads}") String directory,
                             @Value("${app.upload.max-bytes:10485760}") long maxBytes) {
        this.root = Paths.get(directory).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create the upload directory " + root, e);
        }
    }

    public long maxBytes() {
        return maxBytes;
    }

    /** The size limit as text, for example "10 MB". */
    public String limitText() {
        long mb = 1024L * 1024L;
        if (maxBytes % mb == 0) return (maxBytes / mb) + " MB";
        if (maxBytes >= 1024) return (maxBytes / 1024) + " KB";
        return maxBytes + " bytes";
    }

    /** Validates and saves one uploaded file. Throws IllegalArgumentException with a clear message when it is refused. */
    public StoredFile store(MultipartFile file) {
        if (file == null) throw new IllegalArgumentException("Choose a file to attach.");
        String original = cleanName(file.getOriginalFilename());
        String ext = extensionOf(original);
        if (!EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Only PDF, DOCX, PPTX, XLSX and TXT files can be attached.");
        }
        if (file.isEmpty()) throw new IllegalArgumentException("The file is empty.");
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("The file is too large (" + limitText() + " at most).");
        }
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("The file could not be read.");
        }
        if (data.length == 0) throw new IllegalArgumentException("The file is empty.");
        if (data.length > maxBytes) {
            throw new IllegalArgumentException("The file is too large (" + limitText() + " at most).");
        }

        String storedName = UUID.randomUUID() + "." + ext;
        Path target = resolveInside(storedName);
        try {
            Files.write(target, data, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new IllegalStateException("The file could not be saved.", e);
        }
        String type;
        try {
            type = verify(ext, target, data);
        } catch (RuntimeException e) {
            deleteQuietly(target);
            throw e;
        }
        return new StoredFile(original, storedName, type, data.length);
    }

    /** The path of a stored file, or IllegalArgumentException if the reference is invalid or the file is gone. */
    public Path pathOf(String storedName) {
        if (storedName == null || !STORED_NAME.matcher(storedName).matches()) {
            throw new IllegalArgumentException("Invalid file reference.");
        }
        Path p = resolveInside(storedName);
        if (!Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("The file is no longer available.");
        }
        return p;
    }

    public boolean exists(String storedName) {
        try {
            pathOf(storedName);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Deletes exactly one stored file. Anything that is not a name generated by this class is ignored. */
    public boolean delete(String storedName) {
        if (storedName == null || !STORED_NAME.matcher(storedName).matches()) return false;
        try {
            return Files.deleteIfExists(resolveInside(storedName));
        } catch (IOException | IllegalArgumentException e) {
            return false;
        }
    }

    // ---- helpers ----

    private Path resolveInside(String storedName) {
        Path p = root.resolve(storedName).normalize();
        if (!p.startsWith(root) || !root.equals(p.getParent())) {
            throw new IllegalArgumentException("Invalid file reference.");
        }
        return p;
    }

    private static void deleteQuietly(Path p) {
        try {
            Files.deleteIfExists(p);
        } catch (IOException e) {
            // nothing more can be done
        }
    }

    /** The file name without any folder part, without control characters and at most 150 characters. */
    static String cleanName(String raw) {
        if (raw == null) throw new IllegalArgumentException("The file has no name.");
        String name = raw.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        StringBuilder sb = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (!Character.isISOControl(c)) sb.append(c);
        }
        name = sb.toString().trim();
        if (name.isEmpty() || name.equals(".") || name.equals("..")) {
            throw new IllegalArgumentException("The file has an invalid name.");
        }
        if (name.length() > 150) {
            int dot = name.lastIndexOf('.');
            String ext = (dot >= 0 && name.length() - dot <= 10) ? name.substring(dot) : "";
            name = name.substring(0, 150 - ext.length()) + ext;
        }
        return name;
    }

    private static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(java.util.Locale.ROOT);
    }

    private static IllegalArgumentException mismatch() {
        return new IllegalArgumentException("The content of the file does not match its file type.");
    }

    /** Checks the real content against the extension and returns the content type. */
    private static String verify(String ext, Path file, byte[] data) {
        switch (ext) {
            case "pdf":
                if (!startsWith(data, "%PDF-")) throw mismatch();
                return "application/pdf";
            case "txt":
                if (!looksLikeText(data)) throw mismatch();
                return "text/plain";
            case "docx":
                checkOffice(file, "word/");
                return DOCX;
            case "pptx":
                checkOffice(file, "ppt/");
                return PPTX;
            case "xlsx":
                checkOffice(file, "xl/");
                return XLSX;
            default:
                throw mismatch();
        }
    }

    private static boolean startsWith(byte[] data, String prefix) {
        if (data.length < prefix.length()) return false;
        for (int i = 0; i < prefix.length(); i++) {
            if (data[i] != (byte) prefix.charAt(i)) return false;
        }
        return true;
    }

    /** Plain text has no NUL bytes and no control characters other than tab, line breaks and form feed. */
    private static boolean looksLikeText(byte[] data) {
        for (byte b : data) {
            int v = b & 0xFF;
            if (v == 0) return false;
            if (v < 0x20 && v != '\t' && v != '\n' && v != '\r' && v != '\f') return false;
        }
        return true;
    }

    /**
     * An Office file is a ZIP that has [Content_Types].xml and a folder such as word/. Only the names of the
     * entries are read, so a "zip bomb" cannot make the server inflate anything.
     */
    private static void checkOffice(Path file, String folder) {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            boolean hasTypes = false;
            boolean hasBody = false;
            int seen = 0;
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements() && seen < 10000) {
                String name = entries.nextElement().getName();
                seen++;
                if (name.equals("[Content_Types].xml")) hasTypes = true;
                if (name.startsWith(folder)) hasBody = true;
            }
            if (!hasTypes || !hasBody) throw mismatch();
        } catch (IOException e) {
            throw mismatch();
        }
    }
}
