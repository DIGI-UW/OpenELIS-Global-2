package org.openelisglobal.qaevent.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
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

    @Value("${org.openelisglobal.nce.attachment.path:/var/lib/openelis-global/nce-attachments}")
    private String attachmentStoragePath;

    @Autowired
    protected NceAttachmentDAO baseObjectDAO;

    public NceAttachmentServiceImpl() {
        super(NceAttachment.class);
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
        try {
            // Ensure storage directory exists
            Path storageDir = Paths.get(attachmentStoragePath, String.valueOf(nceId));
            Files.createDirectories(storageDir);

            // Generate unique filename to avoid collisions
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String storedFilename = UUID.randomUUID().toString() + extension;
            Path filePath = storageDir.resolve(storedFilename);

            // Write file to disk
            Files.write(filePath, file.getBytes());

            // Create database record
            return createAttachment(nceId, originalFilename, filePath.toString(), file.getContentType(), file.getSize(),
                    uploadedBy);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store attachment: " + e.getMessage(), e);
        }
    }
}
