package org.openelisglobal.analyzer.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.openelisglobal.analyzer.dao.AnalyzerDeliveryActionDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerDeliveryAction;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyzerDeliveryActionServiceImpl implements AnalyzerDeliveryActionService {

    private static final String AUDIT_TABLE = "analyzer_delivery_action";
    private static final Set<String> ACTIONS = Set.of(RETRY, DISMISS);

    private final AnalyzerDeliveryActionDAO actionDAO;
    private final AuditTrailService auditTrailService;

    public AnalyzerDeliveryActionServiceImpl(AnalyzerDeliveryActionDAO actionDAO, AuditTrailService auditTrailService) {
        this.actionDAO = actionDAO;
        this.auditTrailService = auditTrailService;
    }

    @Override
    @Transactional
    public AnalyzerDeliveryAction retain(String outboxEntryId, String action, String analyzerId, String actor) {
        String entryId = requireText(outboxEntryId, "outbox entry ID");
        String exactAction = requireText(action, "action");
        String actorId = requireText(actor, "actor");
        if (!ACTIONS.contains(exactAction)) {
            throw new IllegalArgumentException("Action is invalid");
        }

        AnalyzerDeliveryAction record = new AnalyzerDeliveryAction();
        record.setOutboxEntryId(entryId);
        record.setAction(exactAction);
        record.setAnalyzerId(analyzerId);
        record.setActor(actorId);
        record.setActedAt(Timestamp.from(Instant.now()));
        record.setSysUserId(actorId);
        actionDAO.insert(record);
        auditTrailService.saveNewHistory(record, actorId, AUDIT_TABLE);
        return record;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerDeliveryAction> findByOutboxEntryId(String outboxEntryId) {
        return actionDAO.findByOutboxEntryId(requireText(outboxEntryId, "outbox entry ID"));
    }

    private static String requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.trim();
    }
}
