package org.openelisglobal.notebook.service;

import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.notebook.form.NoteBookForm.NoteBookFileForm;
import org.openelisglobal.notebook.valueholder.NoteBookFile;

public class NotebookAttachmentServiceTest {
    private NoteBookFileForm file(String type, byte[] bytes) {
        NoteBookFileForm file = new NoteBookFileForm();
        file.setFileType(type);
        file.setFileData(bytes);
        return file;
    }

    @Test
    public void acceptsSupportedFilesAndCanonicalizesTypes() {
        for (NoteBookFileForm upload : List.of(file("image/png", new byte[] { (byte) 137, 80, 78, 71, 13, 10, 26, 10 }),
                file("image/jpeg", new byte[] { (byte) 255, (byte) 216, (byte) 255 }),
                file("application/pdf", "%PDF-1.4".getBytes(StandardCharsets.US_ASCII)),
                file("text/plain", "<script>inert text</script>".getBytes(StandardCharsets.UTF_8)))) {
            NotebookAttachmentService.validate(List.of(upload), List.of());
            assertTrue(upload.getFileType().startsWith("data:"));
        }
    }

    @Test
    public void encodedContentDeterminesTypeRegardlessOfJsonFieldOrder() throws Exception {
        String data = "data:text/plain;base64,"
                + Base64.getEncoder().encodeToString("text".getBytes(StandardCharsets.UTF_8));
        for (String json : List.of("{\"base64File\":\"" + data + "\",\"fileType\":\"text/html\"}",
                "{\"fileType\":\"text/html\",\"base64File\":\"" + data + "\"}")) {
            NoteBookFileForm upload = new ObjectMapper().readValue(json, NoteBookFileForm.class);
            NotebookAttachmentService.validate(List.of(upload), List.of());
            assertEquals("data:text/plain", upload.getFileType());
            assertArrayEquals("text".getBytes(StandardCharsets.UTF_8), upload.getFileData());
        }
    }

    @Test
    public void rejectsUnsafeMalformedAndOversizeContent() {
        for (NoteBookFileForm upload : List.of(file("text/html", "<p>markup</p>".getBytes()),
                file("image/png", "not an image".getBytes()), file("image/svg+xml", "<svg/>".getBytes()),
                file("text/plain", new byte[] { (byte) 0xc3, (byte) 0x28 }), file("text/plain", new byte[] { 0 }),
                file("text/plain", new byte[0]), file("text/plain", new byte[10 * 1024 * 1024 + 1]))) {
            assertThrows(InvalidNotebookAttachmentException.class,
                    () -> NotebookAttachmentService.validate(List.of(upload), List.of()));
        }
        for (String encoded : List.of("data:text/plain", "data:text/plain;base64,%%%", "data:text/html;base64,eA==")) {
            NoteBookFileForm upload = new NoteBookFileForm();
            upload.setBase64File(encoded);
            assertThrows(InvalidNotebookAttachmentException.class,
                    () -> NotebookAttachmentService.validate(List.of(upload), List.of()));
        }
    }

    @Test
    public void onlyUnchangedFilesOfThisNotebookCanRetainHistoricalContent() {
        NoteBookFile original = new NoteBookFile();
        original.setId(17);
        original.setFileName("old.html");
        original.setFileType("text/html");
        original.setFileData("<p>old</p>".getBytes());
        NoteBookFileForm retained = file(original.getFileType(), original.getFileData());
        retained.setId(17);
        retained.setFileName("old.html");
        NotebookAttachmentService.validate(List.of(retained), List.of(original));
        assertThrows(InvalidNotebookAttachmentException.class,
                () -> NotebookAttachmentService.validate(List.of(retained), List.of()));
        retained.setFileData("<p>changed</p>".getBytes());
        assertThrows(InvalidNotebookAttachmentException.class,
                () -> NotebookAttachmentService.validate(List.of(retained), List.of(original)));
    }
}
