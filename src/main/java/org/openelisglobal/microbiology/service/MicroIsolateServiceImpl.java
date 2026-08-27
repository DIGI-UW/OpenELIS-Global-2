package org.openelisglobal.microbiology.service;

import java.math.BigDecimal;
import java.util.List;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivityType;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationStatus;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroIsolateServiceImpl implements MicroIsolateService {

    private final MicroCaseDAO caseDAO;
    private final MicroIsolateDAO isolateDAO;
    private final MicroCaseActivityDAO activityDAO;

    public MicroIsolateServiceImpl(MicroCaseDAO caseDAO, MicroIsolateDAO isolateDAO, MicroCaseActivityDAO activityDAO) {
        this.caseDAO = caseDAO;
        this.isolateDAO = isolateDAO;
        this.activityDAO = activityDAO;
    }

    @Override
    @Transactional
    public MicroIsolate createIsolate(String caseId, String isolateLabel, String gramStain, String colonyMorphology,
            MicroIsolateSignificance significance, String performedBy) {
        MicroCaseServiceImpl.requireText(caseId, "caseId");
        MicroCaseServiceImpl.requireText(isolateLabel, "isolateLabel");
        MicroCaseServiceImpl.requireText(gramStain, "gramStain");
        MicroCase microCase = caseDAO.get(caseId).orElseThrow(() -> new IllegalArgumentException("Case not found"));
        MicroCaseMutationGuard.requireMutable(microCase);

        MicroIsolate isolate = new MicroIsolate();
        isolate.setCaseId(caseId);
        isolate.setIsolateLabel(isolateLabel);
        isolate.setGramStain(gramStain.trim());
        isolate.setColonyMorphology(trimToNull(colonyMorphology));
        isolate.setSignificance((significance == null ? MicroIsolateSignificance.UNKNOWN : significance).name());
        isolate.setIdentificationStatus(MicroIsolateIdentificationStatus.PRELIMINARY.name());
        isolate.setCreatedAt(MicroCaseServiceImpl.now());
        isolateDAO.insert(isolate);
        recordActivity(caseId, MicroCaseActivityType.ISOLATE_CREATED, performedBy,
                "Gram stain for " + isolateLabel + ": " + isolate.getGramStain(),
                "{\"isolateId\":\"" + isolate.getId() + "\"}");
        if (MicroCaseStage.RECEIVED.name().equals(microCase.getStage())
                || MicroCaseStage.SETUP_RECORDED.name().equals(microCase.getStage())
                || MicroCaseStage.INCUBATING.name().equals(microCase.getStage())
                || MicroCaseStage.POSITIVE_SIGNAL.name().equals(microCase.getStage())
                || MicroCaseStage.GROWTH_DETECTED.name().equals(microCase.getStage())) {
            microCase.setStage(MicroCaseStage.IDENTIFICATION.name());
            caseDAO.update(microCase);
        }
        return isolate;
    }

    @Override
    @Transactional
    public MicroIsolate updateIdentification(String isolateId, String organismId, String preliminaryOrganismText,
            MicroIsolateSignificance significance, MicroIsolateIdentificationStatus identificationStatus,
            String identificationMethod, BigDecimal identificationConfidence, String performedBy) {
        MicroCaseServiceImpl.requireText(isolateId, "isolateId");
        MicroCaseServiceImpl.requireText(organismId, "organismId");
        MicroCaseServiceImpl.requireText(identificationMethod, "identificationMethod");
        requireConfidence(identificationConfidence);
        if (!MicroIsolateIdentificationStatus.CONFIRMED.equals(identificationStatus)) {
            throw new IllegalArgumentException("identificationStatus must be CONFIRMED");
        }
        MicroIsolate isolate = isolateDAO.get(isolateId)
                .orElseThrow(() -> new IllegalArgumentException("Isolate not found"));
        MicroCase microCase = caseDAO.get(isolate.getCaseId())
                .orElseThrow(() -> new IllegalArgumentException("Case not found"));
        MicroCaseMutationGuard.requireMutable(microCase);
        isolate.setOrganismId(optionalId(organismId));
        isolate.setPreliminaryOrganismText(preliminaryOrganismText);
        isolate.setIdentificationMethod(identificationMethod.trim());
        isolate.setIdentificationConfidence(identificationConfidence);
        isolate.setSignificance((significance == null ? MicroIsolateSignificance.UNKNOWN : significance).name());
        isolate.setIdentificationStatus(MicroIsolateIdentificationStatus.CONFIRMED.name());
        MicroIsolate updated = isolateDAO.update(isolate);
        recordActivity(isolate.getCaseId(), MicroCaseActivityType.ISOLATE_UPDATED, performedBy,
                "Isolate " + isolate.getIsolateLabel() + " updated", "{\"isolateId\":\"" + isolate.getId() + "\"}");
        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroIsolate> getIsolatesForCase(String caseId) {
        return isolateDAO.getByCaseId(caseId);
    }

    private String optionalId(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private void requireConfidence(BigDecimal confidence) {
        if (confidence == null || confidence.compareTo(BigDecimal.ZERO) < 0
                || confidence.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("identificationConfidence must be between 0 and 100");
        }
    }

    private void recordActivity(String caseId, MicroCaseActivityType activityType, String performedBy, String note,
            String structuredData) {
        MicroCaseActivity activity = new MicroCaseActivity();
        activity.setCaseId(caseId);
        activity.setActivityType(activityType.name());
        activity.setOccurredAt(MicroCaseServiceImpl.now());
        activity.setPerformedBy(performedBy);
        activity.setNote(note);
        activity.setStructuredData(structuredData);
        activityDAO.insert(activity);
    }
}
