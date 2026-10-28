package org.openelisglobal.program.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidClinicalAttachmentException extends IllegalArgumentException {
    public InvalidClinicalAttachmentException() {
        super("Attachment must be a JPEG, PNG or PDF file no larger than 10 MB");
    }
}
