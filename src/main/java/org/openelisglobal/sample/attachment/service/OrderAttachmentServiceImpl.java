package org.openelisglobal.sample.attachment.service;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.sample.attachment.dao.OrderAttachmentDAO;
import org.openelisglobal.sample.attachment.valueholder.OrderAttachment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class OrderAttachmentServiceImpl extends AuditableBaseObjectServiceImpl<OrderAttachment, Integer>
        implements OrderAttachmentService {

    private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("application/pdf", "image/jpeg", "image/jpg",
            "image/png", "image/tiff");

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".jpg", ".jpeg", ".png", ".tif", ".tiff");

    @Autowired
    protected OrderAttachmentDAO baseObjectDAO;

    public OrderAttachmentServiceImpl() {
        super(OrderAttachment.class);
        this.auditTrailLog = true;
    }

    @Override
    protected OrderAttachmentDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderAttachment> findActiveBySampleId(Long sampleId) {
        return baseObjectDAO.findActiveBySampleId(sampleId);
    }

    @Override
    @Transactional(readOnly = true)
    public int countActiveBySampleId(Long sampleId) {
        return baseObjectDAO.countActiveBySampleId(sampleId);
    }

    @Override
    @Transactional
    public OrderAttachment createAttachmentFromUpload(Long sampleId, MultipartFile file, Integer uploadedBy) {
        validateFile(file);
        try {
            OrderAttachment attachment = new OrderAttachment();
            attachment.setSampleId(sampleId);
            attachment.setOriginalFileName(file.getOriginalFilename());
            attachment.setFileType(file.getContentType());
            attachment.setFileSizeBytes(file.getSize());
            attachment.setFileContent(file.getBytes());
            attachment.setUploadedBy(uploadedBy);
            attachment.setUploadedAt(Timestamp.from(Instant.now()));
            attachment.setIsDeleted(Boolean.FALSE);
            if (uploadedBy != null) {
                attachment.setSysUserId(String.valueOf(uploadedBy));
            }

            Integer id = baseObjectDAO.insert(attachment);
            attachment.setId(id);
            return attachment;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded file: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void softDelete(Integer attachmentId, Integer deletedBy) {
        OrderAttachment existing = baseObjectDAO.get(attachmentId).orElse(null);
        if (existing == null || Boolean.TRUE.equals(existing.getIsDeleted())) {
            return;
        }
        existing.setIsDeleted(Boolean.TRUE);
        if (deletedBy != null) {
            existing.setSysUserId(String.valueOf(deletedBy));
        }
        baseObjectDAO.update(existing);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "File size exceeds maximum allowed size of " + (MAX_FILE_SIZE / 1024 / 1024) + " MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("File type not allowed: " + contentType);
        }
        String extension = getFileExtension(file.getOriginalFilename());
        if (extension.isEmpty() || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("File extension not allowed: " + extension);
        }
    }

    private String getFileExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return "";
    }
}
