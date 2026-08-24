package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.regex.Pattern;
import org.openelisglobal.analyzer.dao.AnalyzerActivationCandidateDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerActivationCandidate;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingConfirmation;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyzerActivationCandidateServiceImpl implements AnalyzerActivationCandidateService {

    private static final String AUDIT_TABLE = "analyzer_activation_candidate";
    private static final Pattern FINGERPRINT_PATTERN = Pattern.compile("sha256:[0-9a-f]{64}");

    private final AnalyzerActivationCandidateDAO candidateDAO;
    private final AuditTrailService auditTrailService;
    private final ObjectMapper objectMapper;

    public AnalyzerActivationCandidateServiceImpl(AnalyzerActivationCandidateDAO candidateDAO,
            AuditTrailService auditTrailService) {
        this.candidateDAO = candidateDAO;
        this.auditTrailService = auditTrailService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    @Transactional
    public AnalyzerActivationCandidate retain(Analyzer analyzer, AnalyzerSiteBindingRevision siteBindingRevision,
            AnalyzerSiteBindingConfirmation confirmation, AnalyzerActivationDocuments documents, String actor) {
        String analyzerId = requireText(analyzer == null ? null : analyzer.getId(), "analyzer ID");
        String actorId = requireText(actor, "actor");
        if (siteBindingRevision == null || siteBindingRevision.getId() == null || confirmation == null
                || confirmation.getId() == null || confirmation.getSiteBindingRevision() == null
                || !siteBindingRevision.getId().equals(confirmation.getSiteBindingRevision().getId())) {
            throw new IllegalArgumentException("Exact verified site-binding revision is required");
        }
        if (analyzer.getSiteBindingRevision() == null
                || !siteBindingRevision.getId().equals(analyzer.getSiteBindingRevision().getId())) {
            throw new IllegalArgumentException("Analyzer does not reference the verified site-binding revision");
        }
        if (documents == null) {
            throw new IllegalArgumentException("Activation candidate documents are required");
        }
        JsonNode candidate = documents.candidate();
        JsonNode registration = documents.registration();
        if (!analyzerId.equals(requireText(candidate.path("oeAnalyzerId").asText(null), "candidate analyzer ID"))) {
            throw new IllegalArgumentException("Activation candidate does not match the analyzer");
        }
        String candidateFingerprint = requireFingerprint(candidate.path("desiredRegistrationFingerprint").asText(null),
                "candidate desired-state fingerprint");
        String registrationFingerprint = requireFingerprint(registration.path("desiredStateFingerprint").asText(null),
                "registration desired-state fingerprint");
        if (!candidateFingerprint.equals(registrationFingerprint)) {
            throw new IllegalArgumentException("Activation candidate does not name the retained Bridge registration");
        }
        String verificationAuditId = requireText(candidate.path("verification").path("auditEventId").asText(null),
                "candidate verification audit event ID");
        if (!verificationAuditId.equals(requireText(confirmation.getAuditEventId(), "verification audit event ID"))) {
            throw new IllegalArgumentException("Activation candidate does not name the retained verification event");
        }
        String bindingFingerprint = requireFingerprint(siteBindingRevision.getBindingFingerprint(),
                "site-binding fingerprint");
        if (!bindingFingerprint
                .equals(requireFingerprint(candidate.path("verification").path("siteBindingFingerprint").asText(null),
                        "verified site-binding fingerprint"))) {
            throw new IllegalArgumentException("Activation candidate does not match the retained site binding");
        }

        AnalyzerActivationCandidate retained = new AnalyzerActivationCandidate();
        retained.setAnalyzer(analyzer);
        retained.setSiteBindingRevision(siteBindingRevision);
        retained.setVerificationConfirmation(confirmation);
        retained.setCandidateDocumentJson(write(candidate, "activation candidate"));
        retained.setBridgeRegistrationJson(write(registration, "Bridge registration"));
        retained.setDesiredStateFingerprint(candidateFingerprint);
        retained.setCreatedBy(actorId);
        retained.setSysUserId(actorId);
        candidateDAO.insert(retained);
        auditTrailService.saveNewHistory(retained, actorId, AUDIT_TABLE);
        return retained;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerActivationCandidate> findByAnalyzerId(String analyzerId) {
        return candidateDAO.findByAnalyzerId(requireText(analyzerId, "analyzer ID"));
    }

    private String write(JsonNode value, String label) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Cannot retain " + label, exception);
        }
    }

    private static String requireFingerprint(String value, String label) {
        String fingerprint = requireText(value, label);
        if (!FINGERPRINT_PATTERN.matcher(fingerprint).matches()) {
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
}
