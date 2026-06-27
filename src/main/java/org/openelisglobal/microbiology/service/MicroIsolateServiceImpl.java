package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivityType;
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
    public MicroIsolate createIsolate(String caseId, String isolateLabel, String organismId,
            String preliminaryOrganismText, MicroIsolateSignificance significance, String performedBy) {
        MicroCaseServiceImpl.requireText(caseId, "caseId");
        MicroCaseServiceImpl.requireText(isolateLabel, "isolateLabel");
        if (!caseDAO.get(caseId).isPresent()) {
            throw new IllegalArgumentException("Case not found");
        }

        MicroIsolate isolate = new MicroIsolate();
        isolate.setCaseId(caseId);
        isolate.setIsolateLabel(isolateLabel);
        isolate.setOrganismId(organismId);
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
        MicroCaseServiceImpl.requireText(isolateId, "isolateId");
        MicroIsolate isolate = isolateDAO.get(isolateId)
                .orElseThrow(() -> new IllegalArgumentException("Isolate not found"));
        isolate.setOrganismId(organismId);
        isolate.setPreliminaryOrganismText(preliminaryOrganismText);
        isolate.setSignificance((significance == null ? MicroIsolateSignificance.UNKNOWN : significance).name());
        isolate.setIdentificationStatus(
                (identificationStatus == null ? MicroIsolateIdentificationStatus.PRELIMINARY : identificationStatus)
                        .name());
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
