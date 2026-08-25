package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.openelisglobal.analyzer.dao.AnalyzerActivationRecordDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerActivationRecord;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingConfirmation;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyzerActivationRecordServiceImpl implements AnalyzerActivationRecordService {

    private static final String AUDIT_TABLE = "analyzer_activation_record";
    private static final Pattern FINGERPRINT = Pattern.compile("^sha256:[0-9a-f]{64}$");
    private static final Set<String> INTENTS = Set.of("ACTIVE", "INACTIVE");

    private final AnalyzerActivationRecordDAO recordDAO;
    private final AuditTrailService auditTrailService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnalyzerActivationRecordServiceImpl(AnalyzerActivationRecordDAO recordDAO,
            AuditTrailService auditTrailService) {
        this.recordDAO = recordDAO;
        this.auditTrailService = auditTrailService;
    }

    @Override
    @Transactional
    public AnalyzerActivationRecord retain(Analyzer analyzer, AnalyzerSiteBindingRevision siteBindingRevision,
            AnalyzerSiteBindingConfirmation confirmation, ObjectNode runtimeAcknowledgement, String intent,
            String actor) {
        String analyzerId = requireText(analyzer == null ? null : analyzer.getId(), "analyzer ID");
        String connectionId = requireText(analyzer.getBridgeConnectionId(), "Bridge connection ID");
        String actorId = requireText(actor, "actor");
        String exactIntent = requireText(intent, "activation intent");
        if (!INTENTS.contains(exactIntent)) {
            throw new IllegalArgumentException("Activation intent is invalid");
        }
        if (siteBindingRevision == null || analyzer.getSiteBindingRevision() == null
                || !sameId(siteBindingRevision.getId(), analyzer.getSiteBindingRevision().getId())) {
            throw new IllegalArgumentException("Exact analyzer site-binding revision is required");
        }
        if ("ACTIVE".equals(exactIntent) && confirmation == null) {
            throw new IllegalArgumentException("Exact analyzer verification is required");
        }
        if (confirmation != null && (confirmation.getSiteBindingRevision() == null
                || !sameId(siteBindingRevision.getId(), confirmation.getSiteBindingRevision().getId()))) {
            throw new IllegalArgumentException("Analyzer verification does not match the site binding");
        }
        AnalyzerProfileBinding profile = analyzer.getPinnedProfileBinding();
        if (profile == null) {
            throw new IllegalArgumentException("Pinned profile reference is required");
        }
        ObjectNode acknowledgement = runtimeAcknowledgement == null ? null : runtimeAcknowledgement.deepCopy();
        if (acknowledgement == null || !"1.0".equals(acknowledgement.path("schemaVersion").asText())
                || !connectionId.equals(acknowledgement.path("connectionId").asText())
                || !matchesProfile(profile, acknowledgement.path("profileRef"))
                || !matchesIntent(exactIntent, acknowledgement) || !successful(acknowledgement)) {
            throw new IllegalArgumentException("Runtime acknowledgement does not match the analyzer reference");
        }
        String runtimeFingerprint = requireFingerprint(acknowledgement.path("runtimeFingerprint").asText(null),
                "runtime fingerprint");

        AnalyzerActivationRecord record = new AnalyzerActivationRecord();
        record.setAnalyzer(analyzer);
        record.setSiteBindingRevision(siteBindingRevision);
        record.setVerificationConfirmation(confirmation);
        record.setBridgeConnectionId(connectionId);
        record.setActivationIntent(exactIntent);
        record.setRuntimeAcknowledgementJson(write(acknowledgement));
        record.setRuntimeFingerprint(runtimeFingerprint);
        record.setCreatedBy(actorId);
        record.setSysUserId(actorId);
        recordDAO.insert(record);
        auditTrailService.saveNewHistory(record, actorId, AUDIT_TABLE);
        return record;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerActivationRecord> findByAnalyzerId(String analyzerId) {
        return recordDAO.findByAnalyzerId(requireText(analyzerId, "analyzer ID"));
    }

    private static boolean matchesProfile(AnalyzerProfileBinding profile, JsonNode profileRef) {
        return profile.getProfileId().equals(profileRef.path("profileId").asText(null))
                && profile.getProfileRevision() == profileRef.path("revision").asInt(0)
                && profile.getProfileFingerprint().equals(profileRef.path("fingerprint").asText(null));
    }

    private static boolean matchesIntent(String intent, JsonNode acknowledgement) {
        String action = "ACTIVE".equals(intent) ? "ACTIVATE" : "DEACTIVATE";
        return action.equals(acknowledgement.path("action").asText())
                && intent.equals(acknowledgement.path("actualRuntimeState").asText());
    }

    private static boolean successful(JsonNode acknowledgement) {
        String outcome = acknowledgement.path("outcome").asText();
        return "APPLIED".equals(outcome) || "ALREADY_APPLIED".equals(outcome);
    }

    private String write(ObjectNode acknowledgement) {
        try {
            return objectMapper.writeValueAsString(acknowledgement);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Cannot retain runtime acknowledgement", exception);
        }
    }

    private static String requireFingerprint(String value, String label) {
        String fingerprint = requireText(value, label);
        if (!FINGERPRINT.matcher(fingerprint).matches()) {
            throw new IllegalArgumentException(label + " is invalid");
        }
        return fingerprint;
    }

    private static String requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.trim();
    }

    private static boolean sameId(String left, String right) {
        return left != null && left.equals(right);
    }
}
