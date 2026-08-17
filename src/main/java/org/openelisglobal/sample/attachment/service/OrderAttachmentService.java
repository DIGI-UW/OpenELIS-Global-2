package org.openelisglobal.sample.attachment.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.sample.attachment.valueholder.OrderAttachment;
import org.springframework.web.multipart.MultipartFile;

public interface OrderAttachmentService extends BaseObjectService<OrderAttachment, Integer> {

    List<OrderAttachment> findActiveBySampleId(Long sampleId);

    int countActiveBySampleId(Long sampleId);

    OrderAttachment createAttachmentFromUpload(Long sampleId, MultipartFile file, Integer uploadedBy);

    /**
     * OGC-811: upload scoped to the analysis / result component it documents.
     * Either scope may be null — order-level attachments carry neither.
     */
    OrderAttachment createAttachmentFromUpload(Long sampleId, MultipartFile file, Integer uploadedBy, Long analysisId,
            String testResultComponentId);

    void softDelete(Integer attachmentId, Integer deletedBy);
}
