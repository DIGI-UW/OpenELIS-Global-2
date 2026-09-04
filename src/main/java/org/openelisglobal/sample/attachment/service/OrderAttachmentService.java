package org.openelisglobal.sample.attachment.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.sample.attachment.valueholder.OrderAttachment;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

public interface OrderAttachmentService extends BaseObjectService<OrderAttachment, Integer> {

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    List<OrderAttachment> findActiveBySampleId(Long sampleId);

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    int countActiveBySampleId(Long sampleId);

    @PreAuthorize("hasAuthority('PRIV_ORDER_EDIT')")
    OrderAttachment createAttachmentFromUpload(Long sampleId, MultipartFile file, Integer uploadedBy);

    /**
     * OGC-811: upload scoped to the analysis / result component it documents.
     * Either scope may be null — order-level attachments carry neither.
     */
    @PreAuthorize("hasAuthority('PRIV_ORDER_EDIT')")
    OrderAttachment createAttachmentFromUpload(Long sampleId, MultipartFile file, Integer uploadedBy, Long analysisId,
            String testResultComponentId);

    @PreAuthorize("hasAuthority('PRIV_ORDER_EDIT')")
    void softDelete(Integer attachmentId, Integer deletedBy);
}
