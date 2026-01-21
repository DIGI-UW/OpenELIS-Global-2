package org.openelisglobal.biorepository.service;

import java.math.BigDecimal;
import java.util.List;
import org.openelisglobal.biorepository.valueholder.ChainOfCustodyLog;
import org.openelisglobal.biorepository.valueholder.ChainOfCustodyLog.CustodyAction;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest;
import org.openelisglobal.biorepository.valueholder.SampleTransferRequest;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.valueholder.SystemUser;

/**
 * Service interface for ChainOfCustodyLog operations.
 *
 * Provides methods to create and query the immutable audit trail of sample
 * custody changes. All entries are append-only (immutable).
 *
 * Used for: - Recording custody transfers during retrieval workflow - Tracking
 * sample movements between locations - Generating chain-of-custody reports for
 * compliance - Linking outbound retrievals to original inbound transfers
 */
public interface ChainOfCustodyService extends BaseObjectService<ChainOfCustodyLog, Integer> {

    /**
     * Log a custody action for a sample.
     *
     * @param sampleItem         the sample item being tracked
     * @param action             the custody action type
     * @param transferInRequest  optional link to inbound transfer (for
     *                           traceability)
     * @param retrievalRequest   optional link to retrieval request
     * @param storageCoordinates optional storage location coordinates
     * @param custodian          the user performing the action
     * @param fromLocation       optional source location
     * @param toLocation         optional destination location
     * @param temperature        optional temperature reading
     * @param notes              optional notes
     * @param sysUserId          the system user ID for audit
     * @return the created custody log entry
     */
    ChainOfCustodyLog logCustodyAction(SampleItem sampleItem, CustodyAction action,
            SampleTransferRequest transferInRequest, SampleRetrievalRequest retrievalRequest, String storageCoordinates,
            SystemUser custodian, String fromLocation, String toLocation, BigDecimal temperature, String notes,
            String sysUserId);

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
     * Get full lifecycle for a sample between transfer-in and retrieval.
     *
     * @param sampleItemId        the sample item ID
     * @param transferInRequestId optional inbound transfer request ID
     * @param retrievalRequestId  optional retrieval request ID
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
