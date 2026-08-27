package org.openelisglobal.microbiology.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroCaseInoculationDAO;
import org.openelisglobal.microbiology.form.MicroCaseInoculationForm;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivityType;
import org.openelisglobal.microbiology.valueholder.MicroCaseInoculation;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroCaseInoculationServiceImpl implements MicroCaseInoculationService {

    private final MicroCaseDAO caseDAO;
    private final MicroCaseInoculationDAO inoculationDAO;
    private final MicroCaseActivityDAO activityDAO;
    private final ObjectMapper objectMapper;

    @Autowired
    public MicroCaseInoculationServiceImpl(MicroCaseDAO caseDAO, MicroCaseInoculationDAO inoculationDAO,
            MicroCaseActivityDAO activityDAO) {
        this(caseDAO, inoculationDAO, activityDAO, new ObjectMapper());
    }

    MicroCaseInoculationServiceImpl(MicroCaseDAO caseDAO, MicroCaseInoculationDAO inoculationDAO,
            MicroCaseActivityDAO activityDAO, ObjectMapper objectMapper) {
        this.caseDAO = caseDAO;
        this.inoculationDAO = inoculationDAO;
        this.activityDAO = activityDAO;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public MicroCaseInoculation record(String caseId, String sourceInoculationId, String containerIdentifier,
            String media, String incubation, String atmosphere, String performedBy) {
        MicroCaseServiceImpl.requireText(caseId, "caseId");
        MicroCaseServiceImpl.requireText(containerIdentifier, "containerIdentifier");
        MicroCaseServiceImpl.requireText(media, "media");
        MicroCaseServiceImpl.requireText(performedBy, "performedBy");
        MicroCase microCase = caseDAO.get(caseId).orElseThrow(() -> new IllegalArgumentException("Case not found"));
        MicroCaseMutationGuard.requireMutable(microCase);
        if (MicroWorkflowType.UNASSIGNED.name().equals(microCase.getWorkflowType())) {
            throw new IllegalArgumentException("MICROBIOLOGY_WORKFLOW_CLASSIFICATION_REQUIRED");
        }

        MicroCaseInoculation source = sourceInoculationId == null || sourceInoculationId.trim().isEmpty() ? null
                : inoculationDAO.get(sourceInoculationId)
                        .orElseThrow(() -> new IllegalArgumentException("MICROBIOLOGY_SUBCULTURE_SOURCE_NOT_FOUND"));
        if (source != null && !caseId.equals(source.getCaseId())) {
            throw new IllegalArgumentException("MICROBIOLOGY_SUBCULTURE_SOURCE_CASE_MISMATCH");
        }

        MicroCaseInoculation inoculation = new MicroCaseInoculation();
        inoculation.setCaseId(caseId);
        inoculation.setSourceInoculationId(source == null ? null : source.getId());
        inoculation.setMethodId(microCase.getCultureMethodId());
        inoculation.setContainerIdentifier(containerIdentifier.trim());
        inoculation.setMedia(media.trim());
        inoculation.setIncubation(trimToNull(incubation));
        inoculation.setAtmosphere(trimToNull(atmosphere));
        inoculation.setOccurredAt(MicroCaseServiceImpl.now());
        inoculation.setPerformedBy(performedBy);

        MicroCaseActivity activity = new MicroCaseActivity();
        activity.setCaseId(caseId);
        activity.setActivityType((source == null ? MicroCaseActivityType.INOCULATION_RECORDED
                : MicroCaseActivityType.SUBCULTURE_RECORDED).name());
        activity.setOccurredAt(inoculation.getOccurredAt());
        activity.setPerformedBy(performedBy);
        activity.setNote(inoculation.getContainerIdentifier() + " - " + inoculation.getMedia());
        activity.setStructuredData(structuredData(inoculation));
        activityDAO.insert(activity);
        inoculation.setActivityId(activity.getId());
        inoculationDAO.insert(inoculation);

        if (MicroCaseStage.RECEIVED.name().equals(microCase.getStage())
                || MicroCaseStage.SETUP_RECORDED.name().equals(microCase.getStage())) {
            microCase.setStage(MicroCaseStage.INCUBATING.name());
            caseDAO.update(microCase);
        }
        return inoculation;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCaseInoculationForm> getByCaseId(String caseId) {
        MicroCaseServiceImpl.requireText(caseId, "caseId");
        return inoculationDAO.getByCaseId(caseId).stream().map(this::toForm).toList();
    }

    private MicroCaseInoculationForm toForm(MicroCaseInoculation inoculation) {
        MicroCaseInoculationForm form = new MicroCaseInoculationForm();
        form.id = inoculation.getId();
        form.caseId = inoculation.getCaseId();
        form.sourceInoculationId = inoculation.getSourceInoculationId();
        form.methodId = inoculation.getMethodId();
        form.containerIdentifier = inoculation.getContainerIdentifier();
        form.media = inoculation.getMedia();
        form.incubation = inoculation.getIncubation();
        form.atmosphere = inoculation.getAtmosphere();
        form.occurredAt = inoculation.getOccurredAt();
        form.performedBy = inoculation.getPerformedBy();
        return form;
    }

    private String structuredData(MicroCaseInoculation inoculation) {
        try {
            return objectMapper.writeValueAsString(Map.of("inoculationId", inoculation.getId(), "sourceInoculationId",
                    valueOrEmpty(inoculation.getSourceInoculationId()), "containerIdentifier",
                    inoculation.getContainerIdentifier(), "media", inoculation.getMedia(), "methodId",
                    valueOrEmpty(inoculation.getMethodId())));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("MICROBIOLOGY_INOCULATION_AUDIT_SERIALIZATION_FAILED", exception);
        }
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
