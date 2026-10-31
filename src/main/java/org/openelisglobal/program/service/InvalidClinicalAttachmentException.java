package org.openelisglobal.program.service;

public class InvalidClinicalAttachmentException extends IllegalArgumentException {
    public InvalidClinicalAttachmentException() {
        super("Attachment must be a JPEG, PNG or PDF file no larger than 10 MB");
    }
}
