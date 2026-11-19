package com.campuscore;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The file store on its own: validation, safe names, path traversal and persistence. No Spring needed. */
class AttachmentStorageTest {

    @TempDir Path dir;

    private AttachmentStorage storage() {
        return new AttachmentStorage(dir.toString(), 1024 * 1024);
    }

    private long filesInFolder() throws Exception {
        try (Stream<Path> s = Files.list(dir)) {
            return s.count();
        }
    }

    @Test void storesAPdfUnderAGeneratedNameAndKeepsTheOriginalName() throws Exception {
        AttachmentStorage s = storage();
        AttachmentStorage.StoredFile f = s.store(TestFiles.file("Unit 1 Notes.pdf", TestFiles.pdf()));
        assertEquals("Unit 1 Notes.pdf", f.originalName());
        assertEquals("application/pdf", f.contentType());
        assertEquals(TestFiles.pdf().length, f.size());
        assertTrue(f.storedName().matches("[0-9a-f-]{36}\\.pdf"));
        assertArrayEquals(TestFiles.pdf(), Files.readAllBytes(s.pathOf(f.storedName())));
    }

    @Test void acceptsDocxPptxXlsxAndTxt() {
        AttachmentStorage s = storage();
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            s.store(TestFiles.file("a.docx", TestFiles.docx())).contentType());
        assertEquals("application/vnd.openxmlformats-officedocument.presentationml.presentation",
            s.store(TestFiles.file("b.pptx", TestFiles.pptx())).contentType());
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            s.store(TestFiles.file("c.xlsx", TestFiles.xlsx())).contentType());
        assertEquals("text/plain", s.store(TestFiles.file("d.txt", TestFiles.txt())).contentType());
    }

    @Test void extensionsAreNotCaseSensitive() {
        assertEquals("application/pdf", storage().store(TestFiles.file("SCAN.PDF", TestFiles.pdf())).contentType());
    }

    @Test void unsupportedExtensionsAreRejected() throws Exception {
        AttachmentStorage s = storage();
        for (String name : new String[]{"virus.exe", "page.html", "script.js", "archive.zip", "noextension", "image.png"}) {
            assertThrows(IllegalArgumentException.class, () -> s.store(TestFiles.file(name, TestFiles.pdf())), name);
        }
        assertEquals(0, filesInFolder());
    }

    @Test void emptyFileIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> storage().store(TestFiles.file("empty.pdf", new byte[0])));
    }

    @Test void oversizedFileIsRejected() throws Exception {
        byte[] big = new byte[1024 * 1024 + 1];
        System.arraycopy(TestFiles.pdf(), 0, big, 0, 5);
        assertThrows(IllegalArgumentException.class, () -> storage().store(TestFiles.file("big.pdf", big)));
        assertEquals(0, filesInFolder());
    }

    @Test void contentThatDoesNotMatchTheExtensionIsRejected() throws Exception {
        AttachmentStorage s = storage();
        assertThrows(IllegalArgumentException.class, () -> s.store(TestFiles.file("fake.pdf", TestFiles.txt())));
        assertThrows(IllegalArgumentException.class, () -> s.store(TestFiles.file("fake.docx", TestFiles.txt())));
        assertThrows(IllegalArgumentException.class, () -> s.store(TestFiles.file("fake.docx", TestFiles.pptx())));
        assertThrows(IllegalArgumentException.class, () -> s.store(TestFiles.file("fake.xlsx", TestFiles.docx())));
        assertThrows(IllegalArgumentException.class, () -> s.store(TestFiles.file("fake.txt", TestFiles.docx())));
        assertEquals(0, filesInFolder());      // refused files leave nothing behind
    }

    @Test void pathTraversalInTheFileNameIsNeutralised() throws Exception {
        AttachmentStorage s = storage();
        AttachmentStorage.StoredFile f = s.store(TestFiles.file("../../etc/passwd.pdf", TestFiles.pdf()));
        assertEquals("passwd.pdf", f.originalName());
        assertTrue(Files.exists(dir.resolve(f.storedName())));
        assertEquals(1, filesInFolder());
        AttachmentStorage.StoredFile g = s.store(TestFiles.file("..\\..\\evil.txt", TestFiles.txt()));
        assertEquals("evil.txt", g.originalName());
        assertEquals(2, filesInFolder());
    }

    @Test void onlyGeneratedNamesCanBeReadOrDeleted() throws Exception {
        AttachmentStorage s = storage();
        Path stray = dir.resolve("other.txt");
        Files.writeString(stray, "not an attachment");
        assertThrows(IllegalArgumentException.class, () -> s.pathOf("../secret.pdf"));
        assertThrows(IllegalArgumentException.class, () -> s.pathOf("other.txt"));
        assertThrows(IllegalArgumentException.class, () -> s.pathOf(null));
        assertFalse(s.delete("other.txt"));
        assertFalse(s.delete("../other.txt"));
        assertTrue(Files.exists(stray));
    }

    @Test void deleteRemovesOnlyTheNamedFile() {
        AttachmentStorage s = storage();
        AttachmentStorage.StoredFile one = s.store(TestFiles.file("one.pdf", TestFiles.pdf()));
        AttachmentStorage.StoredFile two = s.store(TestFiles.file("two.pdf", TestFiles.pdf()));
        assertTrue(s.delete(one.storedName()));
        assertFalse(s.exists(one.storedName()));
        assertTrue(s.exists(two.storedName()));
    }

    @Test void storedFilesSurviveARestart() throws Exception {
        AttachmentStorage before = storage();
        AttachmentStorage.StoredFile f = before.store(TestFiles.file("keep.pdf", TestFiles.pdf()));
        AttachmentStorage after = storage();          // a new object over the same folder, as after a restart
        assertTrue(after.exists(f.storedName()));
        assertArrayEquals(TestFiles.pdf(), Files.readAllBytes(after.pathOf(f.storedName())));
    }

    @Test void theLimitIsShownInReadableForm() {
        assertEquals("10 MB", new AttachmentStorage(dir.toString(), 10L * 1024 * 1024).limitText());
        assertEquals("512 KB", new AttachmentStorage(dir.toString(), 512L * 1024).limitText());
    }
}
