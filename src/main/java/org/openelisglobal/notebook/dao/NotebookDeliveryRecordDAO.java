package org.openelisglobal.notebook.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.notebook.valueholder.NotebookDeliveryRecord;

/**
 * DAO interface for NotebookDeliveryRecord entity operations.
 */
public interface NotebookDeliveryRecordDAO extends BaseDAO<NotebookDeliveryRecord, Integer> {

    /**
     * Get all delivery records for a specific notebook.
     *
     * @param notebookId the notebook ID
     * @return list of delivery records ordered by delivered_at descending
     */
    List<NotebookDeliveryRecord> getByNotebookId(Integer notebookId);

    /**
     * Get delivery records for a notebook filtered by delivery type.
     *
     * @param notebookId   the notebook ID
     * @param deliveryType the delivery type to filter by
     * @return list of delivery records matching the type
     */
    List<NotebookDeliveryRecord> getByNotebookIdAndDeliveryType(Integer notebookId, String deliveryType);

    /**
     * Get the count of delivery records for a notebook.
     *
     * @param notebookId the notebook ID
     * @return count of delivery records
     */
    long getCountByNotebookId(Integer notebookId);
}
