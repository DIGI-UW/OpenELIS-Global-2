package org.openelisglobal.sample.attachment.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.sample.attachment.valueholder.OrderAttachment;

public interface OrderAttachmentDAO extends BaseDAO<OrderAttachment, Integer> {

    List<OrderAttachment> findActiveBySampleId(Long sampleId);

    int countActiveBySampleId(Long sampleId);
}
