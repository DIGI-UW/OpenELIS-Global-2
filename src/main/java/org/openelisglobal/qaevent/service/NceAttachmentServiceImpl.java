package org.openelisglobal.qaevent.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.qaevent.dao.NceAttachmentDAO;
import org.openelisglobal.qaevent.valueholder.NceAttachment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service implementation for NceAttachment operations.
 */
@Service
public class NceAttachmentServiceImpl extends AuditableBaseObjectServiceImpl<NceAttachment, Integer>
        implements NceAttachmentService {

    /** Maximum file size: 10 MB */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /** Allowed MIME types for attachments */
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/gif",
            "application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "text/plain", "text/csv");

    /** Allowed file extensions */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".pdf", ".doc",
            ".docx", ".xls", ".xlsx", ".txt", ".csv");

    @Value("${org.openelisglobal.nce.attachment.path:/var/lib/openelis-global/nce-attachments}")
    private String attachmentStoragePath;

    @Autowired
    protected NceAttachmentDAO baseObjectDAO;

    public NceAttachmentServiceImpl() {
        super(NceAttachment.class);
        this.auditTrailLog = true;
    }

    @Override
    protected NceAttachmentDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NceAttachment> findByNceId(Integer nceId) {
        return baseObjectDAO.findByNceId(nceId);
    }

    @Override
    @Transactional
    public NceAttachment createAttachment(Integer nceId, String fileName, String filePath, String fileType,
            Long fileSize, Integer uploadedBy) {
        NceAttachment attachment = new NceAttachment();
        attachment.setNceId(nceId);
        attachment.setFileName(fileName);
        attachment.setFilePath(filePath);
        attachment.setFileType(fileType);
        attachment.setFileSize(fileSize);
        attachment.setUploadedBy(uploadedBy);
        attachment.setUploadedDate(Timestamp.from(Instant.now()));

        Integer id = baseObjectDAO.insert(attachment);
        attachment.setId(id);
        return attachment;
    }

    @Override
    @Transactional
    public void deleteByNceId(Integer nceId) {
        baseObjectDAO.deleteByNceId(nceId);
    }

    @Override
    @Transactional(readOnly = true)
    public int countByNceId(Integer nceId) {
        return baseObjectDAO.countByNceId(nceId);
    }

    @Override
    @Transactional
    public NceAttachment createAttachmentFromUpload(Integer nceId, MultipartFile file, Integer uploadedBy) {
        // Validate file
        validateFile(file);

        try {
            // Ensure storage directory exists
            Path storageDir = Paths.get(attachmentStoragePath, String.valueOf(nceId));
            Files.createDirectories(storageDir);

            // Generate unique filename to avoid collisions
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String storedFilename = UUID.randomUUID().toString() + extension;
            Path filePath = storageDir.resolve(storedFilename);

            // Stream file to disk (avoids loading entire file into memory)
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            // Create database record
            return createAttachment(nceId, originalFilename, filePath.toString(), file.getContentType(), file.getSize(),
                    uploadedBy);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store attachment: " + e.getMessage(), e);
        }
    }

    /**
     * Validate uploaded file for size and type restrictions.
     *
     * @param file the uploaded file
     * @throws IllegalArgumentException if validation fails
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "File size exceeds maximum allowed size of " + (MAX_FILE_SIZE / 1024 / 1024) + " MB");
        }

        // Check content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("File type not allowed: " + contentType);
        }

        // Check file extension
        String extension = getFileExtension(file.getOriginalFilename());
        if (extension.isEmpty() || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("File extension not allowed: " + extension);
        }
    }

    /**
     * Extract file extension from filename.
     */
    private String getFileExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return "";
    }
}
