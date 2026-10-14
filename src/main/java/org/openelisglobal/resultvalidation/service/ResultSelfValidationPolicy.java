package org.openelisglobal.resultvalidation.service;

import java.util.Comparator;
import java.util.Objects;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.audittrail.dao.HistoryDAO;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.microbiology.dao.MicroCaseAnalysisDAO;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.result.service.ResultService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ResultSelfValidationPolicy {
    @org.springframework.beans.factory.annotation.Autowired
    private org.openelisglobal.analysis.service.AnalysisService analyses;
    private final HistoryDAO history;
    private final ReferenceTablesService tables;
    private final ResultService results;
    private final MicroCaseAnalysisDAO links;
    private final org.openelisglobal.microbiology.dao.MicroCaseDAO cases;
    private final org.openelisglobal.microbiology.service.MicrobiologyCaseAccessService access;

    public ResultSelfValidationPolicy(HistoryDAO history, ReferenceTablesService tables, ResultService results,
            MicroCaseAnalysisDAO links, org.openelisglobal.microbiology.dao.MicroCaseDAO cases,
            org.openelisglobal.microbiology.service.MicrobiologyCaseAccessService access) {
        this.history = history;
        this.tables = tables;
        this.results = results;
        this.links = links;
        this.cases = cases;
        this.access = access;
    }

    public boolean blocked(Analysis analysis, String actor) {
        if (!ConfigurationProperties.getInstance().isPropertyValueEqual(Property.BLOCK_SELF_VALIDATION, "true"))
            return false;
        var link = links.getActiveByAnalysisId(analysis.getId());
        if (link != null && Objects.equals(actor, link.getEnteredBy()))
            return true;
        String tableId = tables.getReferenceTableByName("RESULT").getId();
        for (var result : results.getResultsByAnalysis(analysis)) {
            var latest = history.getHistoryByRefIdAndRefTableId(result.getId(), tableId).stream()
                    .filter(h -> "I".equals(h.getActivity()) || "U".equals(h.getActivity()))
                    .max(Comparator.comparing(History::getTimestamp, Comparator.nullsFirst(Comparator.naturalOrder())));
            if (latest.isPresent() && Objects.equals(actor, latest.get().getSysUserId()))
                return true;
        }
        return false;
    }

    public void decorateRows(java.util.List<org.openelisglobal.resultvalidation.bean.AnalysisItem> rows, String actor) {
        if (rows == null)
            return;
        java.util.Map<String, Boolean> blockedByAnalysis = new java.util.HashMap<>();
        for (var row : rows) {
            if (row.getAnalysisId() == null)
                continue;
            boolean denied = blockedByAnalysis.computeIfAbsent(row.getAnalysisId(), id -> {
                var analysis = analyses.get(id);
                return analysis != null && blocked(analysis, actor);
            });
            row.setSelfValidationBlocked(denied);
            if (denied)
                row.setClear(false);
        }
    }

    public void requireCaseWriteAccess(Analysis analysis, String actor, String role) {
        var link = links.getActiveByAnalysisId(analysis.getId());
        if (link == null)
            return;
        var c = cases.getForUpdate(link.getCaseId());
        if (c == null || !access.hasLabUnitRole(actor, c.getLabUnitId(), role))
            throw new AccessDeniedException("Case lab unit access required");
        org.openelisglobal.microbiology.service.MicroCaseMutationGuard.requireMutable(c);
    }

    public void requireAnotherValidator(Analysis analysis, String actor) {
        requireCaseWriteAccess(analysis, actor, org.openelisglobal.common.constants.Constants.ROLE_VALIDATION);
        if (blocked(analysis, actor))
            throw new AccessDeniedException("MICROBIOLOGY_SELF_VALIDATION_BLOCKED");
    }
}
