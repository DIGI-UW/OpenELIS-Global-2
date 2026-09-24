package org.openelisglobal.history.service;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.BaseObjectService;

public interface HistoryService extends BaseObjectService<History, String> {

    /**
     * Reference-table names the System Audit Trail surfaces as user-facing audit
     * events. Single source of truth so the trail and any aggregate counts (e.g.
     * the QA Overview "audit entries this week" tile) stay in agreement.
     */
    List<String> SYSTEM_AUDIT_ENTITY_TABLES = Arrays.asList("TEST", "PANEL", "METHOD", "TEST_SECTION", "TYPE_OF_SAMPLE",
            "RESULT_LIMITS", "SYSTEM_USER", "SYSTEM_ROLE", "SYSTEM_USER_ROLE", "DICTIONARY", "DICTIONARY_CATEGORY",
            "analyzer", "site_information", "QA_EVENT", "ANALYSIS_QAEVENT", "ANALYSIS_QAEVENT_ACTION", "QA_OBSERVATION",
            "PATIENT", "PERSON");

    /**
     * The {@code reference_tables} id of each name in
     * {@link #SYSTEM_AUDIT_ENTITY_TABLES}, keyed by name, with unknown names left
     * out. Resolved once and cached — the mapping is static configuration, so a
     * reference table added later is only picked up on restart.
     */
    Map<String, String> getSystemAuditReferenceTableIds();

    List<History> getHistoryByRefIdAndRefTableId(String Id, String Table) throws LIMSRuntimeException;

    List<History> getHistoryByRefIdAndRefTableId(History history) throws LIMSRuntimeException;

    List<History> getSystemEventHistory(Timestamp startDate, Timestamp endDate, String sysUserId,
            List<String> referenceTableIds, String activity, String search, String referenceId, int page, int pageSize)
            throws LIMSRuntimeException;

    long getSystemEventHistoryCount(Timestamp startDate, Timestamp endDate, String sysUserId,
            List<String> referenceTableIds, String activity, String search, String referenceId)
            throws LIMSRuntimeException;
}
