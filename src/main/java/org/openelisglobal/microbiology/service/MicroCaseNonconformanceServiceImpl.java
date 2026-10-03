package org.openelisglobal.microbiology.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.microbiology.dao.MicroAstRunDAO;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.form.MicroCaseNonconformanceRequestForm;
import org.openelisglobal.microbiology.valueholder.MicroAstAttemptType;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroAstTechnique;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivityType;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.qaevent.form.NonConformingEventForm;
import org.openelisglobal.qaevent.service.NceReportService;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.sample.service.SampleItemRejectionService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroCaseNonconformanceServiceImpl implements MicroCaseNonconformanceService {

    private static final DateTimeFormatter NCE_DATE = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final Set<MicroCaseStage> POSITIVE_STAGES = EnumSet.of(MicroCaseStage.POSITIVE_SIGNAL,
            MicroCaseStage.GROWTH_DETECTED, MicroCaseStage.IDENTIFICATION, MicroCaseStage.AST_READY,
            MicroCaseStage.AST_IN_PROGRESS, MicroCaseStage.REVIEW_READY, MicroCaseStage.PRELIM_RELEASED);

    private final MicroCaseDAO caseDAO;
    private final MicroCaseActivityDAO activityDAO;
    private final SampleItemService sampleItemService;
    private final NceReportService nceReportService;
    private final SampleItemRejectionService rejectionService;
    private final MicroAstRunDAO astRunDAO;
    private final MicroIsolateDAO isolateDAO;
    private final MicroAstService astService;
    private final ObjectMapper objectMapper;

    @Autowired
    public MicroCaseNonconformanceServiceImpl(MicroCaseDAO caseDAO, MicroCaseActivityDAO activityDAO,
            SampleItemService sampleItemService, NceReportService nceReportService,
            SampleItemRejectionService rejectionService, MicroAstRunDAO astRunDAO, MicroIsolateDAO isolateDAO,
            MicroAstService astService) {
        this(caseDAO, activityDAO, sampleItemService, nceReportService, rejectionService, astRunDAO, isolateDAO,
                astService, new ObjectMapper());
    }

    MicroCaseNonconformanceServiceImpl(MicroCaseDAO caseDAO, MicroCaseActivityDAO activityDAO,
            SampleItemService sampleItemService, NceReportService nceReportService,
            SampleItemRejectionService rejectionService, MicroAstRunDAO astRunDAO, MicroIsolateDAO isolateDAO,
            MicroAstService astService, ObjectMapper objectMapper) {
        this.caseDAO = caseDAO;
        this.activityDAO = activityDAO;
        this.sampleItemService = sampleItemService;
        this.nceReportService = nceReportService;
        this.rejectionService = rejectionService;
        this.astRunDAO = astRunDAO;
        this.isolateDAO = isolateDAO;
        this.astService = astService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public MicroCaseNonconformanceResult report(String caseId, MicroCaseNonconformanceRequestForm request,
            String authenticatedUserId) {
        requireText(caseId, "caseId");
        require(request, "request");
        requireText(authenticatedUserId, "authenticatedUserId");
        MicroCase currentCase = caseDAO.get(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Microbiology case not found"));
        MicroCaseNonconformanceDisposition disposition = disposition(request.disposition);
        MicroCaseNonconformanceEventType eventType = eventType(request.eventType);
        if (eventType == MicroCaseNonconformanceEventType.SPECIMEN_LOST
                && disposition != MicroCaseNonconformanceDisposition.REJECT_TEST) {
            throw new IllegalArgumentException("Specimen lost requires reject-test disposition");
        }
        if (disposition == MicroCaseNonconformanceDisposition.RETEST) {
            requireRetestSource(currentCase, request);
        }

        SampleItem item = sampleItemService.get(currentCase.getSampleItemId());
        if (item == null || item.getSample() == null) {
            throw new IllegalArgumentException("Case sample item is unavailable");
        }
        List<MicroCase> affectedCases = disposition == MicroCaseNonconformanceDisposition.REJECT_TEST
                ? caseDAO.getBySampleItem(currentCase.getSampleItemId())
                : List.of(currentCase);
        String createdAstRunId = null;
        if (disposition == MicroCaseNonconformanceDisposition.REJECT_TEST) {
            affectedCases.forEach(this::requireRejectable);
        }

        NonConformingEventForm nceForm = toNceForm(request, item);
        NcEvent nce = nceReportService.report(nceForm, authenticatedUserId);
        if (disposition == MicroCaseNonconformanceDisposition.REJECT_TEST) {
            String reason = eventType == MicroCaseNonconformanceEventType.SPECIMEN_LOST ? "Specimen lost"
                    : request.description.trim();
            rejectionService.reject(currentCase.getSampleItemId(), reason, authenticatedUserId);
            for (MicroCase affectedCase : affectedCases) {
                transitionRejected(affectedCase, eventType, authenticatedUserId, nce);
            }
        } else {
            recordActivity(currentCase.getId(), MicroCaseActivityType.NONCONFORMANCE_REPORTED, authenticatedUserId, nce,
                    request.description);
            if (disposition == MicroCaseNonconformanceDisposition.RETEST) {
                List<String> orderedAntibioticIds = request.orderedAntibioticIds == null ? List.of()
                        : request.orderedAntibioticIds;
                MicroAstRun retest = astService.startRepeatRun(request.sourceAstRunId, MicroAstAttemptType.RETEST,
                        request.description, MicroAstTechnique.valueOf(request.astTechnique), List.of(),
                        orderedAntibioticIds, authenticatedUserId);
                createdAstRunId = retest.getId();
            }
        }
        List<String> affectedCaseIds = new ArrayList<>();
        affectedCases.forEach(affectedCase -> affectedCaseIds.add(affectedCase.getId()));
        return new MicroCaseNonconformanceResult(String.valueOf(nce.getId()), nce.getNceNumber(), disposition.name(),
                eventType.name(), List.copyOf(affectedCaseIds), createdAstRunId);
    }

    private void requireRetestSource(MicroCase currentCase, MicroCaseNonconformanceRequestForm request) {
        requireText(request.sourceAstRunId, "sourceAstRunId");
        requireText(request.astTechnique, "astTechnique");
        MicroAstRun source = astRunDAO.get(request.sourceAstRunId)
                .orElseThrow(() -> new IllegalArgumentException("AST source run not found"));
        MicroIsolate isolate = isolateDAO.get(source.getIsolateId())
                .orElseThrow(() -> new IllegalArgumentException("Isolate not found"));
        if (!currentCase.getId().equals(isolate.getCaseId())) {
            throw new IllegalArgumentException("AST_RETEST_SOURCE_CASE_MISMATCH");
        }
    }

    private NonConformingEventForm toNceForm(MicroCaseNonconformanceRequestForm request, SampleItem item) {
        requireText(request.categoryId, "categoryId");
        require(request.reportingUnitId, "reportingUnitId");
        requireText(request.severity, "severity");
        requireText(request.description, "description");
        NonConformingEventForm form = new NonConformingEventForm();
        form.setLabOrderNumber(item.getSample().getAccessionNumber());
        form.setSpecimenId(item.getId());
        form.setDateOfEvent(LocalDate.now().format(NCE_DATE));
        form.setReportingUnit(request.reportingUnitId);
        form.setNceCategoryId(request.categoryId);
        form.setNceTypeId(request.typeId);
        form.setSeverity(request.severity);
        form.setTitle(request.title);
        form.setDescription(request.description);
        form.setImmediateAction(request.immediateAction);
        return form;
    }

    private void transitionRejected(MicroCase microCase, MicroCaseNonconformanceEventType eventType,
            String authenticatedUserId, NcEvent nce) {
        MicroCaseStage currentStage = MicroCaseStage.valueOf(microCase.getStage());
        MicroCaseStage nextStage;
        MicroCaseActivityType activityType;
        if (eventType == MicroCaseNonconformanceEventType.SPECIMEN_LOST) {
            nextStage = POSITIVE_STAGES.contains(currentStage) ? MicroCaseStage.LOST_SPECIMEN_POSITIVE
                    : MicroCaseStage.LOST_SPECIMEN;
            activityType = MicroCaseActivityType.SPECIMEN_LOST;
        } else {
            nextStage = MicroCaseStage.REJECTED;
            activityType = MicroCaseActivityType.NONCONFORMANCE_REPORTED;
        }
        microCase.setStage(nextStage.name());
        microCase.setClosedAt(new Timestamp(System.currentTimeMillis()));
        microCase.setClosedBy(authenticatedUserId);
        caseDAO.update(microCase);
        recordActivity(microCase.getId(), activityType, authenticatedUserId, nce,
                eventType == MicroCaseNonconformanceEventType.SPECIMEN_LOST ? "Specimen lost" : "Test rejected");
    }

    private void recordActivity(String caseId, MicroCaseActivityType activityType, String authenticatedUserId,
            NcEvent nce, String note) {
        MicroCaseActivity activity = new MicroCaseActivity();
        activity.setCaseId(caseId);
        activity.setActivityType(activityType.name());
        activity.setOccurredAt(new Timestamp(System.currentTimeMillis()));
        activity.setPerformedBy(authenticatedUserId);
        activity.setNote(note);
        activity.setStructuredData(structuredData(nce));
        activityDAO.insert(activity);
    }

    private String structuredData(NcEvent nce) {
        try {
            return objectMapper
                    .writeValueAsString(Map.of("nceId", String.valueOf(nce.getId()), "nceNumber", nce.getNceNumber()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("NCE timeline metadata could not be serialized", e);
        }
    }

    private void requireRejectable(MicroCase microCase) {
        MicroCaseStage stage = MicroCaseStage.valueOf(microCase.getStage());
        if (stage == MicroCaseStage.FINAL_RELEASED || stage == MicroCaseStage.AMENDED) {
            throw new IllegalStateException("Final microbiology work cannot be rejected");
        }
    }

    private MicroCaseNonconformanceDisposition disposition(String value) {
        requireText(value, "disposition");
        return MicroCaseNonconformanceDisposition.valueOf(value);
    }

    private MicroCaseNonconformanceEventType eventType(String value) {
        requireText(value, "eventType");
        return MicroCaseNonconformanceEventType.valueOf(value);
    }

    private void requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    private void require(Object value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
