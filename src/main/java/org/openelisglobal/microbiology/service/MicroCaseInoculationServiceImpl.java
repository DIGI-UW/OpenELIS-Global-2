package org.openelisglobal.microbiology.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroCaseInoculationDAO;
import org.openelisglobal.microbiology.dao.MicroCaseSpecimenDAO;
import org.openelisglobal.microbiology.form.MicroCaseInoculationForm;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivityType;
import org.openelisglobal.microbiology.valueholder.MicroCaseInoculation;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroInventoryUsageContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroCaseInoculationServiceImpl implements MicroCaseInoculationService {

    private final MicroCaseDAO caseDAO;
    private final MicroCaseInoculationDAO inoculationDAO;
    private final MicroCaseActivityDAO activityDAO;
    private final MicroReagentLotService reagentLotService;
    private final ObjectMapper objectMapper;
    private final MicroCaseSpecimenDAO specimenDAO;
    private final MicrobiologyCaseAccessService accessService;

    @Autowired
    public MicroCaseInoculationServiceImpl(MicroCaseDAO caseDAO, MicroCaseInoculationDAO inoculationDAO,
            MicroCaseActivityDAO activityDAO, MicroReagentLotService reagentLotService,
            MicroCaseSpecimenDAO specimenDAO, MicrobiologyCaseAccessService accessService) {
        this(caseDAO, inoculationDAO, activityDAO, reagentLotService, new ObjectMapper(), specimenDAO, accessService);
    }

    MicroCaseInoculationServiceImpl(MicroCaseDAO caseDAO, MicroCaseInoculationDAO inoculationDAO,
            MicroCaseActivityDAO activityDAO, MicroReagentLotService reagentLotService, ObjectMapper objectMapper,
            MicroCaseSpecimenDAO specimenDAO, MicrobiologyCaseAccessService accessService) {
        this.caseDAO = caseDAO;
        this.inoculationDAO = inoculationDAO;
        this.activityDAO = activityDAO;
        this.reagentLotService = reagentLotService;
        this.objectMapper = objectMapper;
        this.specimenDAO = specimenDAO;
        this.accessService = accessService;
    }

    @Override
    @Transactional
    public MicroCaseInoculation record(String caseId, String sourceSampleItemId, String sourceInoculationId,
            String containerIdentifier, String media, String incubation, String atmosphere,
            List<MicroLotSelection> lotSelections, String performedBy) {
        MicroCaseServiceImpl.requireText(caseId, "caseId");
        MicroCaseServiceImpl.requireText(sourceSampleItemId, "sourceSampleItemId");
        MicroCaseServiceImpl.requireText(containerIdentifier, "containerIdentifier");
        MicroCaseServiceImpl.requireText(media, "media");
        MicroCaseServiceImpl.requireText(performedBy, "performedBy");
        MicroCase observed = caseDAO.get(caseId).orElseThrow(() -> new IllegalArgumentException("Case not found"));
        caseDAO.lockOrder(observed.getSampleId());
        MicroCase microCase = caseDAO.getForUpdate(caseId);
        accessService.requireResults(caseId, performedBy);
        MicroCaseMutationGuard.requireMutable(microCase);
        if (specimenDAO.getByCaseAndSampleItem(caseId, sourceSampleItemId) == null) {
            throw new IllegalArgumentException("The culture specimen must belong to this case");
        }

        MicroCaseInoculation source = sourceInoculationId == null || sourceInoculationId.trim().isEmpty() ? null
                : inoculationDAO.get(sourceInoculationId)
                        .orElseThrow(() -> new IllegalArgumentException("MICROBIOLOGY_SUBCULTURE_SOURCE_NOT_FOUND"));
        if (source != null && !caseId.equals(source.getCaseId())) {
            throw new IllegalArgumentException("MICROBIOLOGY_SUBCULTURE_SOURCE_CASE_MISMATCH");
        }
        if (source != null && !sourceSampleItemId.equals(source.getSourceSampleItemId())) {
            throw new IllegalArgumentException("The subculture must retain its parent's specimen");
        }

        MicroCaseInoculation inoculation = new MicroCaseInoculation();
        inoculation.setCaseId(caseId);
        inoculation.setSourceSampleItemId(sourceSampleItemId);
        inoculation.setSourceInoculationId(source == null ? null : source.getId());
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
        reagentLotService.recordSelections(caseId, MicroInventoryUsageContext.CULTURE_SETUP, activity.getId(),
                lotSelections == null ? List.of() : lotSelections, performedBy);
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
        form.sourceSampleItemId = inoculation.getSourceSampleItemId();
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
            return objectMapper.writeValueAsString(Map.of("inoculationId", inoculation.getId(), "sourceSampleItemId",
                    inoculation.getSourceSampleItemId(), "sourceInoculationId",
                    valueOrEmpty(inoculation.getSourceInoculationId()), "containerIdentifier",
                    inoculation.getContainerIdentifier(), "media", inoculation.getMedia(), "incubation",
                    valueOrEmpty(inoculation.getIncubation()), "atmosphere", valueOrEmpty(inoculation.getAtmosphere()),
                    "methodId", valueOrEmpty(inoculation.getMethodId())));
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
