package org.openelisglobal.program.service;

/** Attachment fields accepted by the clinical case forms. */
public interface ClinicalAttachmentUpload {
    Integer getId();

    String getBase64Image();

    byte[] getImage();

    void setImage(byte[] image);

    String getFileType();

    void setFileType(String fileType);
}
