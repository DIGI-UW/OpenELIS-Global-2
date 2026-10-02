package org.openelisglobal.history.service;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.BaseObjectService;
import org.springframework.security.access.prepost.PreAuthorize;

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
    @PreAuthorize("hasAuthority('PRIV_AUDIT_VIEW')")
    Map<String, String> getSystemAuditReferenceTableIds();

    /**
     * The history rows for one record, used both for audit browsing and - via
     * {@code ResultsValidationUtility#recordedByFromHistory} - to answer "who
     * recorded this result" for one column of the validation list.
     *
     * <p>
     * So it admits the result-reviewing authorities alongside PRIV_AUDIT_VIEW.
     * Gating it on audit alone, which only the Audit Trail role holds, made GET
     * /rest/AccessionValidation answer 403 for the Validation role: its own main
     * screen could not list anything to validate. The broader audit browsing
     * methods below stay on PRIV_AUDIT_VIEW.
     */
    @PreAuthorize("hasAnyAuthority('PRIV_AUDIT_VIEW','PRIV_RESULT_VALIDATE','PRIV_RESULT_VIEW')")
    List<History> getHistoryByRefIdAndRefTableId(String Id, String Table) throws LIMSRuntimeException;

    @PreAuthorize("hasAuthority('PRIV_AUDIT_VIEW')")
    List<History> getHistoryByRefIdAndRefTableId(History history) throws LIMSRuntimeException;

    @PreAuthorize("hasAuthority('PRIV_AUDIT_VIEW')")
    List<History> getSystemEventHistory(Timestamp startDate, Timestamp endDate, String sysUserId,
            List<String> referenceTableIds, String activity, String search, String referenceId, int page, int pageSize)
            throws LIMSRuntimeException;

    @PreAuthorize("hasAuthority('PRIV_AUDIT_VIEW')")
    long getSystemEventHistoryCount(Timestamp startDate, Timestamp endDate, String sysUserId,
            List<String> referenceTableIds, String activity, String search, String referenceId)
            throws LIMSRuntimeException;
}
