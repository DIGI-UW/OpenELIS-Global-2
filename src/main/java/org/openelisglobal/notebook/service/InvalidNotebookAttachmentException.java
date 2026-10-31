package org.openelisglobal.notebook.service;

public class InvalidNotebookAttachmentException extends IllegalArgumentException {
    public InvalidNotebookAttachmentException() {
        super("Notebook attachment must be a JPEG, PNG, PDF or UTF-8 text file no larger than 10 MB");
    }
}
