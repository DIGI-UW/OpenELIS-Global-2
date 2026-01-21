package org.openelisglobal.biorepository.dao;

import java.util.List;
import org.openelisglobal.biorepository.valueholder.ChainOfCustodyLog;
import org.openelisglobal.biorepository.valueholder.ChainOfCustodyLog.CustodyAction;
import org.openelisglobal.common.dao.BaseDAO;

/**
 * DAO interface for ChainOfCustodyLog entity operations.
 *
 * Provides methods to query the immutable audit trail of sample custody
 * changes.
 */
public interface ChainOfCustodyLogDAO extends BaseDAO<ChainOfCustodyLog, Integer> {

    /**
     * Get full custody history for a sample item.
     *
     * @param sampleItemId the sample item ID
     * @return list of custody log entries ordered by action timestamp ascending
     */
    List<ChainOfCustodyLog> getBySampleItemId(Integer sampleItemId);

    /**
     * Get custody history for a specific transfer-in request.
     *
     * @param transferInRequestId the inbound transfer request ID
     * @return list of custody log entries ordered by action timestamp ascending
     */
    List<ChainOfCustodyLog> getByTransferInRequestId(Integer transferInRequestId);

    /**
     * Get custody history for a specific retrieval request.
     *
     * @param retrievalRequestId the retrieval request ID
     * @return list of custody log entries ordered by action timestamp ascending
     */
    List<ChainOfCustodyLog> getByRetrievalRequestId(Integer retrievalRequestId);

    /**
     * Get custody log entries by action type.
     *
     * @param action the custody action type
     * @return list of custody log entries ordered by action timestamp descending
     */
    List<ChainOfCustodyLog> getByAction(CustodyAction action);

    /**
     * Get recent custody actions with pagination.
     *
     * @param limit maximum results
     * @return list of recent custody log entries ordered by timestamp descending
     */
    List<ChainOfCustodyLog> getRecentActions(int limit);

    /**
     * Get custody log entries for a sample between two requests (full lifecycle).
     *
     * @param sampleItemId        the sample item ID
     * @param transferInRequestId the inbound transfer request ID (optional)
     * @param retrievalRequestId  the retrieval request ID (optional)
     * @return list of custody log entries ordered by action timestamp ascending
     */
    List<ChainOfCustodyLog> getFullLifecycle(Integer sampleItemId, Integer transferInRequestId,
            Integer retrievalRequestId);

    /**
     * Count custody actions by type.
     *
     * @param action the custody action type
     * @return count of actions
     */
    long countByAction(CustodyAction action);

    /**
     * Search custody logs with filters and pagination.
     *
     * @param sampleExternalId sample barcode/external ID (partial match)
     * @param action           custody action type filter (null = all)
     * @param custodianId      custodian user ID filter (null = all)
     * @param startDate        start timestamp filter (null = no limit)
     * @param endDate          end timestamp filter (null = no limit)
     * @param page             page number (0-indexed)
     * @param pageSize         results per page
     * @return list of custody log entries matching filters
     */
    List<ChainOfCustodyLog> searchCustodyLogs(String sampleExternalId, CustodyAction action, Integer custodianId,
            java.sql.Timestamp startDate, java.sql.Timestamp endDate, int page, int pageSize);

    /**
     * Count custody logs matching search filters.
     *
     * @param sampleExternalId sample barcode/external ID (partial match)
     * @param action           custody action type filter (null = all)
     * @param custodianId      custodian user ID filter (null = all)
     * @param startDate        start timestamp filter (null = no limit)
     * @param endDate          end timestamp filter (null = no limit)
     * @return total count of matching records
     */
    long countCustodyLogs(String sampleExternalId, CustodyAction action, Integer custodianId,
            java.sql.Timestamp startDate, java.sql.Timestamp endDate);
}
