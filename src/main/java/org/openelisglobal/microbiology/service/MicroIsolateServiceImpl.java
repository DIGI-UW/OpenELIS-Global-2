package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivityType;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationEvent;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationStatus;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroIsolateServiceImpl implements MicroIsolateService {

    private final MicroCaseDAO caseDAO;
    private final MicroIsolateDAO isolateDAO;
    private final MicroCaseActivityDAO activityDAO;
    private final MicroIdentificationHistoryService identificationHistoryService;

    public MicroIsolateServiceImpl(MicroCaseDAO caseDAO, MicroIsolateDAO isolateDAO, MicroCaseActivityDAO activityDAO,
            MicroIdentificationHistoryService identificationHistoryService) {
        this.caseDAO = caseDAO;
        this.isolateDAO = isolateDAO;
        this.activityDAO = activityDAO;
        this.identificationHistoryService = identificationHistoryService;
    }

    @Override
    @Transactional
    public MicroIsolate createIsolate(String caseId, String isolateLabel, String organismId,
            String preliminaryOrganismText, MicroIsolateSignificance significance, String performedBy) {
        MicroCaseServiceImpl.requireText(caseId, "caseId");
        MicroCaseServiceImpl.requireText(isolateLabel, "isolateLabel");
        MicroCase microCase = caseDAO.get(caseId).orElseThrow(() -> new IllegalArgumentException("Case not found"));
        MicroCaseMutationGuard.requireMutable(microCase);

        MicroIsolate isolate = new MicroIsolate();
        isolate.setCaseId(caseId);
        isolate.setIsolateLabel(isolateLabel);
        isolate.setOrganismId(optionalId(organismId));
        isolate.setPreliminaryOrganismText(preliminaryOrganismText);
        isolate.setSignificance((significance == null ? MicroIsolateSignificance.UNKNOWN : significance).name());
        isolate.setIdentificationStatus(MicroIsolateIdentificationStatus.PRELIMINARY.name());
        isolate.setCreatedAt(MicroCaseServiceImpl.now());
        isolateDAO.insert(isolate);
        recordActivity(caseId, MicroCaseActivityType.ISOLATE_CREATED, performedBy,
                "Isolate " + isolateLabel + " created", "{\"isolateId\":\"" + isolate.getId() + "\"}");
        return isolate;
    }

    @Override
    @Transactional
    public MicroIsolate updateIdentification(String isolateId, String organismId, String preliminaryOrganismText,
            MicroIsolateSignificance significance, MicroIsolateIdentificationStatus identificationStatus,
            String performedBy) {
        return updateIdentification(isolateId, organismId, preliminaryOrganismText, significance, identificationStatus,
                null, performedBy);
    }

    @Override
    @Transactional
    public MicroIsolate updateIdentification(String isolateId, String organismId, String preliminaryOrganismText,
            MicroIsolateSignificance significance, MicroIsolateIdentificationStatus identificationStatus, String reason,
            String performedBy) {
        MicroCaseServiceImpl.requireText(isolateId, "isolateId");
        MicroIsolate isolate = isolateDAO.get(isolateId)
                .orElseThrow(() -> new IllegalArgumentException("Isolate not found"));
        MicroCase microCase = caseDAO.get(isolate.getCaseId())
                .orElseThrow(() -> new IllegalArgumentException("Case not found"));
        MicroCaseMutationGuard.requireMutable(microCase);
        MicroIsolate previous = identificationSnapshot(isolate);
        isolate.setOrganismId(optionalId(organismId));
        isolate.setPreliminaryOrganismText(preliminaryOrganismText);
        isolate.setSignificance((significance == null ? MicroIsolateSignificance.UNKNOWN : significance).name());
        isolate.setIdentificationStatus(
                (identificationStatus == null ? MicroIsolateIdentificationStatus.PRELIMINARY : identificationStatus)
                        .name());
        MicroIsolateIdentificationEvent event = identificationHistoryService.recordChange(previous, isolate, reason,
                performedBy);
        MicroIsolate updated = isolateDAO.update(isolate);
        recordActivity(isolate.getCaseId(), MicroCaseActivityType.ISOLATE_UPDATED, performedBy,
                identificationActivityNote(isolate, previous, reason),
                "{\"isolateId\":\"" + isolate.getId() + "\",\"identificationEventId\":\"" + event.getId() + "\"}");
        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroIsolate> getIsolatesForCase(String caseId) {
        return isolateDAO.getByCaseId(caseId);
    }

    private MicroIsolate identificationSnapshot(MicroIsolate isolate) {
        MicroIsolate snapshot = new MicroIsolate();
        snapshot.setId(isolate.getId());
        snapshot.setCaseId(isolate.getCaseId());
        snapshot.setIsolateLabel(isolate.getIsolateLabel());
        snapshot.setOrganismId(isolate.getOrganismId());
        snapshot.setPreliminaryOrganismText(isolate.getPreliminaryOrganismText());
        snapshot.setSignificance(isolate.getSignificance());
        snapshot.setIdentificationStatus(isolate.getIdentificationStatus());
        return snapshot;
    }

    private String identificationActivityNote(MicroIsolate updated, MicroIsolate previous, String reason) {
        String note = "Isolate " + updated.getIsolateLabel() + " identification changed from "
                + identificationLabel(previous) + " to " + identificationLabel(updated);
        return reason == null || reason.trim().isEmpty() ? note : note + ": " + reason.trim();
    }

    private String identificationLabel(MicroIsolate isolate) {
        if (isolate.getPreliminaryOrganismText() != null && !isolate.getPreliminaryOrganismText().trim().isEmpty()) {
            return isolate.getPreliminaryOrganismText().trim();
        }
        if (isolate.getOrganismId() != null && !isolate.getOrganismId().trim().isEmpty()) {
            return isolate.getOrganismId().trim();
        }
        return "unidentified";
    }

    private String optionalId(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
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
