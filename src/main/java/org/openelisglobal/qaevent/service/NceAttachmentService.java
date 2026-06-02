package org.openelisglobal.qaevent.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.qaevent.valueholder.NceAttachment;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service interface for NceAttachment operations.
 */
public interface NceAttachmentService extends BaseObjectService<NceAttachment, Integer> {

    /**
     * Find all attachments for a given NCE.
     *
     * @param nceId the NCE ID
     * @return list of attachments
     */
    @PreAuthorize("hasAuthority('PRIV_NCE_VIEW')")
    List<NceAttachment> findByNceId(Integer nceId);

    /**
     * Create an attachment for an NCE.
     *
     * @param nceId      the NCE ID
     * @param fileName   original file name
     * @param filePath   stored file path
     * @param fileType   MIME type
     * @param fileSize   size in bytes
     * @param uploadedBy user ID who uploaded
     * @return the created attachment
     */
    @PreAuthorize("hasAuthority('PRIV_NCE_EDIT')")
    NceAttachment createAttachment(Integer nceId, String fileName, String filePath, String fileType, Long fileSize,
            Integer uploadedBy);

    /**
     * Delete all attachments for a given NCE.
     *
     * @param nceId the NCE ID
     */
    @PreAuthorize("hasAuthority('PRIV_NCE_EDIT')")
    void deleteByNceId(Integer nceId);

    /**
     * Count attachments for a given NCE.
     *
     * @param nceId the NCE ID
     * @return attachment count
     */
    @PreAuthorize("hasAuthority('PRIV_NCE_VIEW')")
    int countByNceId(Integer nceId);

    /**
     * Create an attachment from an uploaded file. Handles file storage and creates
     * the database record.
     *
     * @param nceId      the NCE ID
     * @param file       the uploaded file
     * @param uploadedBy user ID who uploaded
     * @return the created attachment
     */
    @PreAuthorize("hasAuthority('PRIV_NCE_EDIT')")
    NceAttachment createAttachmentFromUpload(Integer nceId, MultipartFile file, Integer uploadedBy);
}
