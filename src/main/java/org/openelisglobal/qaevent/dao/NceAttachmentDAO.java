package org.openelisglobal.qaevent.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.qaevent.valueholder.NceAttachment;

/**
 * DAO interface for NceAttachment entities.
 */
public interface NceAttachmentDAO extends BaseDAO<NceAttachment, String> {

    /**
     * Find all attachments for a given NCE.
     *
     * @param nceId the NCE ID
     * @return list of attachments
     */
    List<NceAttachment> findByNceId(Integer nceId);

    /**
     * Delete all attachments for a given NCE.
     *
     * @param nceId the NCE ID
     */
    void deleteByNceId(Integer nceId);

    /**
     * Count attachments for a given NCE.
     *
     * @param nceId the NCE ID
     * @return attachment count
     */
    int countByNceId(Integer nceId);
}
