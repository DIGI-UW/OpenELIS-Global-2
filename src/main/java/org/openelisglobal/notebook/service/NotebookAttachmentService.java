package org.openelisglobal.notebook.service;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import org.openelisglobal.notebook.form.NoteBookForm.NoteBookFileForm;
import org.openelisglobal.notebook.valueholder.NoteBookFile;
import org.openelisglobal.program.service.ClinicalAttachmentService;

/**
 * Notebook uploads are accepted once, before the owning service changes the
 * notebook.
 */
public final class NotebookAttachmentService {
    private static final int MAX_BYTES = 10 * 1024 * 1024;

    private NotebookAttachmentService() {
    }

    public static void validate(List<NoteBookFileForm> uploads, List<NoteBookFile> stored) {
        if (uploads == null) {
            return;
        }
        for (NoteBookFileForm upload : uploads) {
            if (upload == null) {
                throw new InvalidNotebookAttachmentException();
            }
            // An unchanged historical file may remain attached, even if it cannot be
            // previewed.
            if (upload.getBase64File() == null && stored.stream()
                    .anyMatch(file -> file.getId() != null && file.getId().equals(upload.getId())
                            && Objects.equals(file.getFileType(), upload.getFileType())
                            && Objects.equals(file.getFileName(), upload.getFileName())
                            && Arrays.equals(file.getFileData(), upload.getFileData()))) {
                continue;
            }
            String type = upload.getFileType();
            byte[] bytes = upload.getFileData();
            String encoded = upload.getBase64File();
            try {
                if (encoded != null) {
                    int separator = encoded.indexOf(";base64,");
                    if (separator < 0 || encoded.length() > 4L * ((MAX_BYTES + 2) / 3) + 40) {
                        throw new InvalidNotebookAttachmentException();
                    }
                    type = encoded.substring(0, separator);
                    bytes = Base64.getDecoder().decode(encoded.substring(separator + 8));
                }
                if (type != null && !type.startsWith("data:")) {
                    type = "data:" + type;
                }
                if (bytes == null || bytes.length == 0 || bytes.length > MAX_BYTES) {
                    throw new InvalidNotebookAttachmentException();
                }
                if ("data:text/plain".equals(type)) {
                    StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes));
                    for (byte value : bytes) {
                        if (value == 0) {
                            throw new InvalidNotebookAttachmentException();
                        }
                    }
                } else {
                    ClinicalAttachmentService.validateContent(type, bytes);
                }
            } catch (IllegalArgumentException | CharacterCodingException ex) {
                throw new InvalidNotebookAttachmentException();
            }
            upload.setFileData(bytes);
            upload.setFileType(type);
        }
    }
}
