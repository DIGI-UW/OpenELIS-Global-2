package org.openelisglobal.microbiology.service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroCaseSpecimenDAO;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivityType;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroInventoryUsageContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroCaseStateServiceImpl implements MicroCaseStateService {

    private static final Map<MicroCaseStage, Set<MicroCaseStage>> ALLOWED_TRANSITIONS = new EnumMap<>(
            MicroCaseStage.class);

    static {
        ALLOWED_TRANSITIONS.put(MicroCaseStage.RECEIVED,
                EnumSet.of(MicroCaseStage.SETUP_RECORDED, MicroCaseStage.REJECTED));
        ALLOWED_TRANSITIONS.put(MicroCaseStage.SETUP_RECORDED,
                EnumSet.of(MicroCaseStage.INCUBATING, MicroCaseStage.REJECTED));
        ALLOWED_TRANSITIONS.put(MicroCaseStage.INCUBATING,
                EnumSet.of(MicroCaseStage.POSITIVE_SIGNAL, MicroCaseStage.NO_GROWTH_READY, MicroCaseStage.REJECTED));
        ALLOWED_TRANSITIONS.put(MicroCaseStage.POSITIVE_SIGNAL, EnumSet.of(MicroCaseStage.GROWTH_DETECTED));
        ALLOWED_TRANSITIONS.put(MicroCaseStage.GROWTH_DETECTED, EnumSet.of(MicroCaseStage.IDENTIFICATION));
        ALLOWED_TRANSITIONS.put(MicroCaseStage.IDENTIFICATION, EnumSet.of(MicroCaseStage.AST_READY));
        ALLOWED_TRANSITIONS.put(MicroCaseStage.AST_READY, EnumSet.of(MicroCaseStage.AST_IN_PROGRESS));
        ALLOWED_TRANSITIONS.put(MicroCaseStage.AST_IN_PROGRESS, EnumSet.of(MicroCaseStage.REVIEW_READY));
        ALLOWED_TRANSITIONS.put(MicroCaseStage.REVIEW_READY, EnumSet.noneOf(MicroCaseStage.class));
        ALLOWED_TRANSITIONS.put(MicroCaseStage.PRELIM_RELEASED, EnumSet.noneOf(MicroCaseStage.class));
        ALLOWED_TRANSITIONS.put(MicroCaseStage.FINAL_RELEASED, EnumSet.noneOf(MicroCaseStage.class));
    }

    private final MicroCaseDAO caseDAO;
    private final MicroCaseActivityDAO activityDAO;
    private final MicroReagentLotService reagentLotService;
    private final MicrobiologyCaseAccessService accessService;
    private final MicroCaseSpecimenDAO specimenDAO;

    public MicroCaseStateServiceImpl(MicroCaseDAO caseDAO, MicroCaseActivityDAO activityDAO,
            MicroReagentLotService reagentLotService, MicrobiologyCaseAccessService accessService,
            MicroCaseSpecimenDAO specimenDAO) {
        this.caseDAO = caseDAO;
        this.activityDAO = activityDAO;
        this.reagentLotService = reagentLotService;
        this.accessService = accessService;
        this.specimenDAO = specimenDAO;
    }

    @Override
    @Transactional
    public MicroCase advanceStage(String caseId, MicroCaseStage nextStage, String performedBy, String note) {
        return advanceStage(caseId, nextStage, performedBy, note, List.of());
    }

    @Override
    @Transactional
    public MicroCase advanceStage(String caseId, MicroCaseStage nextStage, String performedBy, String note,
            List<MicroLotSelection> lotSelections) {
        return advanceStage(caseId, nextStage, performedBy, note, lotSelections, null);
    }

    @Override
    @Transactional
    public MicroCase advanceStage(String caseId, MicroCaseStage nextStage, String performedBy, String note,
            List<MicroLotSelection> lotSelections, String sourceSampleItemId) {
        MicroCaseServiceImpl.requireText(caseId, "caseId");
        if (nextStage == null) {
            throw new IllegalArgumentException("nextStage is required");
        }
        accessService.requireResults(caseId, performedBy);
        MicroCase microCase = caseDAO.get(caseId).orElseThrow(() -> new IllegalArgumentException("Case not found"));
        MicroCaseMutationGuard.requireMutable(microCase);
        MicroCaseStage currentStage = MicroCaseStage.valueOf(microCase.getStage());
        if (!ALLOWED_TRANSITIONS.getOrDefault(currentStage, EnumSet.noneOf(MicroCaseStage.class)).contains(nextStage)) {
            throw new IllegalArgumentException("Invalid microbiology case stage transition");
        }
        if (lotSelections != null && !lotSelections.isEmpty() && !MicroCaseStage.SETUP_RECORDED.equals(nextStage)) {
            throw new IllegalArgumentException("MICROBIOLOGY_LOTS_ONLY_ALLOWED_DURING_SETUP");
        }
        boolean observation = EnumSet
                .of(MicroCaseStage.POSITIVE_SIGNAL, MicroCaseStage.GROWTH_DETECTED, MicroCaseStage.NO_GROWTH_READY)
                .contains(nextStage);
        if (observation) {
            if (sourceSampleItemId == null || sourceSampleItemId.isBlank()) {
                throw new IllegalArgumentException("MICROBIOLOGY_CULTURE_SOURCE_SAMPLE_REQUIRED");
            }
            if (specimenDAO.getByCaseAndSampleItem(caseId, sourceSampleItemId) == null) {
                throw new IllegalArgumentException("MICROBIOLOGY_CULTURE_SOURCE_NOT_CASE_MEMBER");
            }
        } else if (sourceSampleItemId != null) {
            throw new IllegalArgumentException("MICROBIOLOGY_CULTURE_SOURCE_ONLY_ALLOWED_FOR_OBSERVATION");
        }
        microCase.setStage(nextStage.name());
        MicroCase updated = caseDAO.update(microCase);
        MicroCaseActivity activity = recordActivity(caseId, MicroCaseActivityType.STAGE_CHANGED, performedBy, note,
                "{\"from\":\"" + currentStage.name() + "\",\"to\":\"" + nextStage.name() + "\"}",
                observation ? sourceSampleItemId : null);
        reagentLotService.recordSelections(caseId, MicroInventoryUsageContext.CULTURE_SETUP, activity.getId(),
                lotSelections, performedBy);
        return updated;
    }

    private MicroCaseActivity recordActivity(String caseId, MicroCaseActivityType activityType, String performedBy,
            String note, String structuredData, String sourceSampleItemId) {
        MicroCaseActivity activity = new MicroCaseActivity();
        activity.setCaseId(caseId);
        activity.setActivityType(activityType.name());
        activity.setOccurredAt(MicroCaseServiceImpl.now());
        activity.setPerformedBy(performedBy);
        activity.setNote(note);
        activity.setStructuredData(structuredData);
        activity.setResultSourceSampleItemId(sourceSampleItemId);
        activityDAO.insert(activity);
        return activity;
    }
}
