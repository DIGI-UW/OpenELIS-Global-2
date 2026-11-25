package org.openelisglobal.batchworkplan.service;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.service.AnalyzerTestCapability;
import org.openelisglobal.batchworkplan.dao.BatchWorkplanDAO;
import org.openelisglobal.batchworkplan.dao.BatchWorkplanItemDAO;
import org.openelisglobal.batchworkplan.dao.BatchWorkplanQcReleaseDAO;
import org.openelisglobal.batchworkplan.form.BatchQcStatusResponse;
import org.openelisglobal.batchworkplan.form.BatchReagentRequest;
import org.openelisglobal.batchworkplan.form.BatchReleaseResponse;
import org.openelisglobal.batchworkplan.form.BatchSetupOptionsResponse;
import org.openelisglobal.batchworkplan.form.BatchWorkplanItemResponse;
import org.openelisglobal.batchworkplan.form.BatchWorkplanRequest;
import org.openelisglobal.batchworkplan.form.BatchWorkplanResponse;
import org.openelisglobal.batchworkplan.form.ControlLotQcResponse;
import org.openelisglobal.batchworkplan.form.InstrumentOptionResponse;
import org.openelisglobal.batchworkplan.form.PendingBatchTestResponse;
import org.openelisglobal.batchworkplan.form.ReagentLotOptionResponse;
import org.openelisglobal.batchworkplan.form.ReagentOptionResponse;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplan;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplanItem;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplanQcRelease;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplanStatus;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.service.InventoryLotService;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.qaevent.service.NCEventService;
import org.openelisglobal.qaevent.service.NceCategoryService;
import org.openelisglobal.qaevent.service.NceSpecimenService;
import org.openelisglobal.qaevent.service.NceTypeService;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.qaevent.valueholder.NceCategory;
import org.openelisglobal.qaevent.valueholder.NceSpecimen;
import org.openelisglobal.qc.service.QCControlLotService;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testreagentlink.service.TestReagentLinkService;
import org.openelisglobal.testreagentlink.valueholder.TestReagentLink;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BatchWorkplanServiceImpl implements BatchWorkplanService {

    public static final String TRIGGER_SOURCE_WORKPLAN_BATCH = "WORKPLAN_BATCH";

    private static final int DEFAULT_PENDING_LIMIT = 100;
    private static final int MAX_PENDING_LIMIT = 500;
    private static final int MAX_NAME_LENGTH = 100;
    private static final int COMPLETED_RETENTION_DAYS = 30;
    // An override is a deliberate release on QC that is not current, not a QC
    // failure in itself: the failing run, if any, already raised its own CRITICAL
    // NCE through the Westgard path.
    private static final String NCE_SEVERITY = "MAJOR";
    private static final String NCE_CATEGORY_ANALYSIS = "Analysis";
    private static final String NCE_TYPE_PROCEDURE_DEVIATION = "Procedure deviation";
    private static final DateTimeFormatter BATCH_NAME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final BatchWorkplanDAO batchWorkplanDAO;
    private final BatchWorkplanItemDAO batchWorkplanItemDAO;
    private final AnalysisService analysisService;
    private final IStatusService statusService;
    private final UserService userService;
    private final BatchWorkplanQcService qcService;
    private final BatchWorkplanQcReleaseDAO releaseDAO;
    private final TestReagentLinkService testReagentLinkService;
    private final InventoryItemService inventoryItemService;
    private final InventoryLotService inventoryLotService;
    private final AnalyzerService analyzerService;
    private final NCEventService ncEventService;
    private final SystemUserService systemUserService;
    private final QCControlLotService controlLotService;
    private final NceCategoryService nceCategoryService;
    private final NceTypeService nceTypeService;
    private final NceSpecimenService nceSpecimenService;

    public BatchWorkplanServiceImpl(BatchWorkplanDAO batchWorkplanDAO, BatchWorkplanItemDAO batchWorkplanItemDAO,
            AnalysisService analysisService, IStatusService statusService, UserService userService,
            BatchWorkplanQcService qcService, BatchWorkplanQcReleaseDAO releaseDAO,
            TestReagentLinkService testReagentLinkService, InventoryItemService inventoryItemService,
            InventoryLotService inventoryLotService, AnalyzerService analyzerService, NCEventService ncEventService,
            SystemUserService systemUserService, QCControlLotService controlLotService,
            NceCategoryService nceCategoryService, NceTypeService nceTypeService,
            NceSpecimenService nceSpecimenService) {
        this.batchWorkplanDAO = batchWorkplanDAO;
        this.batchWorkplanItemDAO = batchWorkplanItemDAO;
        this.analysisService = analysisService;
        this.statusService = statusService;
        this.userService = userService;
        this.qcService = qcService;
        this.releaseDAO = releaseDAO;
        this.testReagentLinkService = testReagentLinkService;
        this.inventoryItemService = inventoryItemService;
        this.inventoryLotService = inventoryLotService;
        this.analyzerService = analyzerService;
        this.ncEventService = ncEventService;
        this.systemUserService = systemUserService;
        this.controlLotService = controlLotService;
        this.nceCategoryService = nceCategoryService;
        this.nceTypeService = nceTypeService;
        this.nceSpecimenService = nceSpecimenService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingBatchTestResponse> getPendingTests(Integer limit, String sysUserId) {
        Set<String> assignedAnalysisIds = batchWorkplanItemDAO.getAnalysisIdsInStatuses(unarchivedStatuses());
        List<String> visibleTestIds = userService.getUserTestIdsForLabUnitRoles(sysUserId, Constants.ROLE_RESULTS);
        // Both exclusions are in the query, so the row cap is the last thing applied
        // and a page full of already-batched or out-of-unit work cannot squeeze the
        // eligible rows out of the result.
        List<Analysis> analyses = analysisService.getPendingAnalysesForWorkplan(workplanPendingStatusIds(),
                visibleTestIds, assignedAnalysisIds, boundLimit(limit));
        Map<String, String> patientNames = patientNames(analyses);
        return analyses.stream().map(analysis -> toPendingResponse(analysis, patientNames))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BatchWorkplanResponse> getBatches(String sysUserId) {
        List<BatchWorkplan> batches = batchWorkplanDAO.getForUserInStatuses(toUserId(sysUserId), unarchivedStatuses());
        Set<String> analysisIds = batches.stream().flatMap(batch -> batch.getItems().stream())
                .map(BatchWorkplanItem::getAnalysisId).collect(Collectors.toCollection(LinkedHashSet::new));
        List<Analysis> analyses = analysisIds.isEmpty() ? List.of()
                : analysisService.getAnalysesByIdsWithDetails(new ArrayList<>(analysisIds));
        Map<String, Analysis> analysesById = analysesByIdFromAnalyses(analyses);
        Map<String, String> patientNames = patientNames(analyses);
        return batches.stream().map(batch -> toBatchResponse(batch, analysesById, patientNames))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BatchWorkplanResponse getBatch(Long id, String sysUserId) {
        return toBatchResponse(requireOwnBatch(id, sysUserId));
    }

    @Override
    public BatchWorkplanResponse createBatch(BatchWorkplanRequest request, String sysUserId) {
        if (request == null || request.getAnalysisIds() == null || request.getAnalysisIds().isEmpty()) {
            throw new IllegalArgumentException("At least one analysis is required to create a batch workplan");
        }

        Integer ownerId = requireUserId(sysUserId);

        List<String> analysisIds = cleanIds(request.getAnalysisIds());
        if (analysisIds.isEmpty()) {
            throw new IllegalArgumentException("At least one analysis is required to create a batch workplan");
        }
        validateName(request.getName());

        Set<String> alreadyAssigned = batchWorkplanItemDAO.getExistingAnalysisIds(analysisIds, unarchivedStatuses());
        if (!alreadyAssigned.isEmpty()) {
            throw new IllegalArgumentException("Analyses already assigned to an open batch: " + alreadyAssigned);
        }

        List<Analysis> analyses = analysisService.getAnalysesByIdsWithDetails(analysisIds);
        if (analyses.size() != analysisIds.size()) {
            throw new IllegalArgumentException("One or more analyses were not found");
        }
        // Re-check the caller's lab units on the write path: the pending list is
        // already scoped, but the request body is client-supplied and a crafted one
        // would otherwise batch another unit's work.
        if (inUserLabUnits(analyses, sysUserId).size() != analyses.size()) {
            throw new IllegalArgumentException("One or more analyses are outside your lab units");
        }
        validatePending(analyses);

        BatchWorkplan batch = new BatchWorkplan();
        batch.setName(StringUtils.defaultIfBlank(StringUtils.trimToNull(request.getName()),
                "Batch " + LocalDateTime.now().format(BATCH_NAME_FORMAT)));
        batch.setStatus(BatchWorkplanStatus.DRAFT);
        batch.setTestSectionId(StringUtils.trimToNull(request.getTestSectionId()));
        batch.setNotes(StringUtils.trimToNull(request.getNotes()));
        batch.setCreatedAt(Timestamp.from(Instant.now()));
        batch.setSysUserId(sysUserId);
        batch.setCreatedByUserId(ownerId);
        batch.setUpdatedByUserId(ownerId);

        for (int index = 0; index < analysisIds.size(); index++) {
            BatchWorkplanItem item = new BatchWorkplanItem();
            item.setAnalysisId(analysisIds.get(index));
            item.setSortOrder(index + 1);
            batch.addItem(item);
        }

        Long id = batchWorkplanDAO.insert(batch);
        BatchWorkplan saved = batchWorkplanDAO.getWithItems(id).orElse(batch);
        return toBatchResponse(saved, analysesByIdFromAnalyses(analyses), patientNames(analyses));
    }

    @Override
    public BatchWorkplanResponse updateBatch(Long id, BatchWorkplanRequest request, String sysUserId) {
        BatchWorkplan batch = requireOwnBatch(id, sysUserId);
        requireDraft(batch);
        if (request == null) {
            throw new IllegalArgumentException("A batch update needs a name or tests");
        }
        validateName(request.getName());
        if (StringUtils.isNotBlank(request.getName())) {
            batch.setName(request.getName().trim());
        }
        if (request.getNotes() != null) {
            batch.setNotes(StringUtils.trimToNull(request.getNotes()));
        }

        if (request.getAnalysisIds() != null) {
            List<String> analysisIds = cleanIds(request.getAnalysisIds());
            if (analysisIds.isEmpty()) {
                throw new IllegalArgumentException("A batch must contain at least one test");
            }
            Set<String> current = batch.getItems().stream().map(BatchWorkplanItem::getAnalysisId)
                    .collect(Collectors.toSet());
            List<String> added = analysisIds.stream().filter(analysisId -> !current.contains(analysisId))
                    .collect(Collectors.toList());
            if (!added.isEmpty()) {
                Set<String> elsewhere = batchWorkplanItemDAO.getExistingAnalysisIdsOutsideBatch(added,
                        unarchivedStatuses(), batch.getId());
                if (!elsewhere.isEmpty()) {
                    throw new IllegalArgumentException("Analyses already assigned to an open batch: " + elsewhere);
                }
                List<Analysis> addedAnalyses = analysisService.getAnalysesByIdsWithDetails(added);
                if (addedAnalyses.size() != added.size()) {
                    throw new IllegalArgumentException("One or more analyses were not found");
                }
                if (inUserLabUnits(addedAnalyses, sysUserId).size() != addedAnalyses.size()) {
                    throw new IllegalArgumentException("One or more analyses are outside your lab units");
                }
                validatePending(addedAnalyses);
            }
            Map<String, BatchWorkplanItem> kept = new HashMap<>();
            for (BatchWorkplanItem item : batch.getItems()) {
                kept.put(item.getAnalysisId(), item);
            }
            batch.getItems().removeIf(item -> !analysisIds.contains(item.getAnalysisId()));
            for (int index = 0; index < analysisIds.size(); index++) {
                String analysisId = analysisIds.get(index);
                BatchWorkplanItem item = kept.get(analysisId);
                if (item == null) {
                    item = new BatchWorkplanItem();
                    item.setAnalysisId(analysisId);
                    batch.addItem(item);
                }
                item.setSortOrder(index + 1);
            }
        }

        stampUpdate(batch, sysUserId);
        return toBatchResponse(batchWorkplanDAO.update(batch));
    }

    @Override
    @Transactional(readOnly = true)
    public BatchSetupOptionsResponse getSetupOptions(Long id, String sysUserId) {
        BatchWorkplan batch = requireOwnBatch(id, sysUserId);
        List<BatchTestRef> tests = testsOf(batch);

        Map<Long, List<String>> testNamesByReagent = new LinkedHashMap<>();
        for (BatchTestRef test : tests) {
            for (TestReagentLink link : testReagentLinkService.getByTestId(test.testId())) {
                if (link.getReagentId() != null) {
                    testNamesByReagent.computeIfAbsent(link.getReagentId(), k -> new ArrayList<>())
                            .add(test.testName());
                }
            }
        }
        List<ReagentOptionResponse> reagents = new ArrayList<>();
        for (Map.Entry<Long, List<String>> entry : testNamesByReagent.entrySet()) {
            InventoryItem item = inventoryItemService.getMatch("id", entry.getKey()).orElse(null);
            if (item == null || !item.isActive()) {
                continue;
            }
            reagents.add(new ReagentOptionResponse(item.getId(), item.getName(), item.getUnits(), entry.getValue(),
                    lotOptions(item)));
        }
        return new BatchSetupOptionsResponse(reagents, instrumentOptions(tests));
    }

    @Override
    public BatchWorkplanResponse assignReagent(Long id, BatchReagentRequest request, String sysUserId) {
        BatchWorkplan batch = requireOwnBatch(id, sysUserId);
        requireDraft(batch);
        if (request == null || request.reagentId() == null || request.reagentLotId() == null) {
            throw new IllegalArgumentException("Select a reagent and a reagent lot");
        }
        List<BatchTestRef> tests = testsOf(batch);
        boolean linked = tests.stream().anyMatch(
                test -> testReagentLinkService.getByTestIdAndReagentId(test.testId(), request.reagentId()) != null);
        if (!linked) {
            throw new IllegalArgumentException("The reagent is not linked to any test in this batch");
        }
        boolean usable = inventoryLotService.getAvailableLotsByItemFEFO(request.reagentId()).stream()
                .anyMatch(lot -> request.reagentLotId().equals(lot.getId()));
        if (!usable) {
            throw new IllegalArgumentException(
                    "This lot is expired, used up, not QC-passed or not a lot of the selected reagent");
        }
        String instrumentId = StringUtils.trimToNull(request.instrumentId());
        if (instrumentId != null
                && instrumentOptions(tests).stream().noneMatch(option -> option.id().equals(instrumentId))) {
            throw new IllegalArgumentException("The analyzer is not set up to run every test in this batch");
        }

        batch.setReagentId(request.reagentId());
        batch.setReagentLotId(request.reagentLotId());
        batch.setInstrumentId(instrumentId);
        stampUpdate(batch, sysUserId);
        return toBatchResponse(batchWorkplanDAO.update(batch));
    }

    @Override
    @Transactional(readOnly = true)
    public BatchQcStatusResponse getQcStatus(Long id, String sysUserId) {
        BatchWorkplan batch = requireOwnBatch(id, sysUserId);
        return qcService.evaluate(testsOf(batch), batch.getInstrumentId());
    }

    @Override
    public BatchWorkplanResponse generate(Long id, boolean override, boolean mayOverride, String sysUserId) {
        Integer actorId = requireUserId(sysUserId);
        BatchWorkplan batch = requireOwnBatch(id, sysUserId);
        requireDraft(batch);
        if (batch.getReagentId() == null || batch.getReagentLotId() == null) {
            throw new IllegalArgumentException("Assign a reagent and lot before generating a workplan");
        }
        boolean lotStillUsable = inventoryLotService.getAvailableLotsByItemFEFO(batch.getReagentId()).stream()
                .anyMatch(lot -> batch.getReagentLotId().equals(lot.getId()));
        if (!lotStillUsable) {
            throw new IllegalArgumentException(
                    "The batch's reagent lot is no longer usable (expired, used up or not QC-passed); select another");
        }

        List<BatchTestRef> tests = testsOf(batch);
        BatchQcStatusResponse qc = qcService.evaluate(tests, batch.getInstrumentId());
        if (qc.blocking()) {
            throw new BatchReleaseRefusedException(BatchReleaseRefusedException.Reason.BLOCKED, qc);
        }
        boolean overriding = !qc.status().isPass();
        if (overriding && !override) {
            throw new BatchReleaseRefusedException(BatchReleaseRefusedException.Reason.OVERRIDE_REQUIRED, qc);
        }
        if (overriding && !mayOverride) {
            throw new BatchReleaseRefusedException(BatchReleaseRefusedException.Reason.OVERRIDE_NOT_PERMITTED, qc);
        }

        Timestamp now = Timestamp.from(Instant.now());
        NcEvent nce = overriding ? raiseOverrideNce(batch, tests, qc, now, sysUserId) : null;
        for (ControlLotQcResponse lot : qc.lots()) {
            BatchWorkplanQcRelease release = new BatchWorkplanQcRelease();
            release.setBatchWorkplanId(batch.getId());
            release.setControlLotId(lot.controlLotId());
            release.setTestId(lot.testId());
            release.setQcStatus(lot.status());
            release.setQcResultId(lot.lastRun() == null ? null : lot.lastRun().id());
            release.setQcRuleViolationId(lot.openViolationIds().isEmpty() ? null : lot.openViolationIds().get(0));
            release.setOverridden(overriding);
            release.setNceId(nce == null ? null : nce.getId());
            release.setReleasedAt(now);
            release.setReleasedByUserId(actorId);
            release.setSysUserId(sysUserId);
            releaseDAO.insert(release);
        }

        batch.setStatus(BatchWorkplanStatus.ACTIVE);
        batch.setActivatedAt(now);
        batch.setGeneratedAt(now);
        batch.setGeneratedByUserId(actorId);
        stampUpdate(batch, sysUserId);
        return toBatchResponse(batchWorkplanDAO.update(batch));
    }

    @Override
    public int archiveAll(String sysUserId) {
        Integer actorId = requireUserId(sysUserId);
        Timestamp now = Timestamp.from(Instant.now());
        int archived = 0;
        for (BatchWorkplan batch : batchWorkplanDAO.getForUserInStatuses(actorId, unarchivedStatuses())) {
            batch.setStatus(BatchWorkplanStatus.ARCHIVED);
            batch.setArchivedAt(now);
            stampUpdate(batch, sysUserId);
            batchWorkplanDAO.update(batch);
            archived++;
        }
        return archived;
    }

    @Override
    public int refreshLifecycle(String sysUserId) {
        Integer ownerId = toUserId(sysUserId);
        if (ownerId == null) {
            return 0;
        }
        Set<String> doneStatuses = Set.of(statusService.getStatusID(AnalysisStatus.Finalized),
                statusService.getStatusID(AnalysisStatus.Canceled));
        Instant now = Instant.now();
        Instant retentionCutoff = now.minus(COMPLETED_RETENTION_DAYS, ChronoUnit.DAYS);
        int changed = 0;
        for (BatchWorkplan batch : batchWorkplanDAO.getForUserInStatuses(ownerId,
                Arrays.asList(BatchWorkplanStatus.ACTIVE, BatchWorkplanStatus.COMPLETED))) {
            if (batch.getStatus() == BatchWorkplanStatus.ACTIVE && !batch.getItems().isEmpty()) {
                List<String> ids = batch.getItems().stream().map(BatchWorkplanItem::getAnalysisId)
                        .collect(Collectors.toList());
                List<Analysis> analyses = analysisService.getAnalysesByIdsWithDetails(ids);
                boolean allDone = analyses.size() == ids.size()
                        && analyses.stream().allMatch(analysis -> doneStatuses.contains(analysis.getStatusId()));
                if (allDone) {
                    batch.setStatus(BatchWorkplanStatus.COMPLETED);
                    batch.setCompletedAt(Timestamp.from(now));
                    stampUpdate(batch, sysUserId);
                    batchWorkplanDAO.update(batch);
                    changed++;
                }
            } else if (batch.getStatus() == BatchWorkplanStatus.COMPLETED && batch.getCompletedAt() != null
                    && batch.getCompletedAt().toInstant().isBefore(retentionCutoff)) {
                batch.setStatus(BatchWorkplanStatus.ARCHIVED);
                batch.setArchivedAt(Timestamp.from(now));
                stampUpdate(batch, sysUserId);
                batchWorkplanDAO.update(batch);
                changed++;
            }
        }
        return changed;
    }

    @Override
    public BatchWorkplanResponse transitionBatch(Long id, BatchWorkplanStatus nextStatus, String sysUserId) {
        if (nextStatus == null) {
            throw new IllegalArgumentException("Batch status is required");
        }
        // Activation is the release, and the release is where QC is checked: letting
        // the plain status change reach ACTIVE would print a workplan no one had
        // checked QC for.
        if (nextStatus == BatchWorkplanStatus.ACTIVE) {
            throw new IllegalArgumentException("A batch is activated by generating its workplan");
        }
        Integer actorId = requireUserId(sysUserId);
        BatchWorkplan batch = requireOwnBatch(id, sysUserId);
        BatchWorkplanStatus currentStatus = batch.getStatus();
        if (!currentStatus.canTransitionTo(nextStatus)) {
            throw new IllegalArgumentException(
                    "Cannot transition batch workplan from " + currentStatus + " to " + nextStatus);
        }

        Timestamp now = Timestamp.from(Instant.now());
        batch.setStatus(nextStatus);
        batch.setSysUserId(sysUserId);
        batch.setUpdatedByUserId(actorId);
        if (nextStatus == BatchWorkplanStatus.DRAFT) {
            // Back to setup for an edit (FR-BL-003). The release rows keep the record
            // of the earlier release; the batch itself is no longer released.
            batch.setGeneratedAt(null);
            batch.setGeneratedByUserId(null);
        } else if (nextStatus == BatchWorkplanStatus.COMPLETED) {
            batch.setCompletedAt(now);
        } else if (nextStatus == BatchWorkplanStatus.ARCHIVED) {
            batch.setArchivedAt(now);
        }

        return toBatchResponse(batchWorkplanDAO.update(batch));
    }

    /**
     * The NCE an override raises (FR-NCE-002). It records everything a QC officer
     * needs to follow the release up without opening the batch: the tests and lab
     * numbers, the analyzer, the reagent lot, and each control lot with its status
     * and the run or violation behind it.
     */
    private NcEvent raiseOverrideNce(BatchWorkplan batch, List<BatchTestRef> tests, BatchQcStatusResponse qc,
            Timestamp now, String sysUserId) {
        String instrument = qc.instrumentName() != null ? qc.instrumentName() : "bench (no analyzer)";
        String testNames = tests.stream().map(BatchTestRef::testName).distinct().collect(Collectors.joining(", "));

        StringBuilder description = new StringBuilder();
        description.append("Auto-created when workplan batch ").append(batch.getId()).append(" (\"")
                .append(batch.getName()).append("\") was released with QC status ").append(qc.status())
                .append(". Instrument: ").append(instrument).append(". Reagent lot: ").append(reagentLabel(batch))
                .append(".");
        for (ControlLotQcResponse lot : qc.lots()) {
            description.append(" Control lot ").append(lot.lotNumber()).append(" (").append(lot.controlLevel())
                    .append(", ").append(lot.testName()).append(", required ").append(lot.frequencyType()).append("): ")
                    .append(lot.status()).append("; last QC ")
                    .append(lot.lastRun() == null ? "never"
                            : lot.lastRun().runDateTime() + " " + lot.lastRun().resultStatus() + " (result "
                                    + lot.lastRun().id() + ")");
            if (!lot.ruleCodes().isEmpty()) {
                description.append("; rules ").append(String.join(", ", lot.ruleCodes()));
            }
            if (!lot.openViolationIds().isEmpty()) {
                description.append("; violation ").append(String.join(", ", lot.openViolationIds()));
            }
            description.append(".");
        }
        description.append(" Tests in batch: ").append(labNumbersAndTests(batch)).append(".");

        NcEvent nce = new NcEvent();
        nce.setSysUserId(sysUserId);
        nce.setSeverity(NCE_SEVERITY);
        nce.setTriggerSourceType(TRIGGER_SOURCE_WORKPLAN_BATCH);
        nce.setTriggerSourceId(batch.getId() + ":" + now.getTime());
        nce.setTitle(
                StringUtils.abbreviate("QC deviation - workplan override: " + testNames + " on " + instrument, 255));
        nce.setDescription(description.toString());
        nce.setImmediateAction("Batch released for testing without current QC; review the batch's results");
        nce.setNameOfReporter(userDisplayName(sysUserId));
        nce.setDateOfEvent(new java.sql.Date(now.getTime()));
        firstTestSectionId(tests).ifPresent(nce::setReportingUnitId);
        resolveCategoryAndType(nce);
        return ncEventService.createAutoGenerated(nce,
                "Auto-created from the QC override on workplan batch " + batch.getId() + " (QC " + qc.status() + ")",
                saved -> linkBatchSpecimens(saved, batch, sysUserId));
    }

    /**
     * Analysis / Procedure deviation: the release went ahead without current QC. A
     * failing run behind it already raised its own "QC failure" NCE through the
     * Westgard path. Both are seeded reference data resolved by name; a missing
     * seed leaves the NCE uncategorised rather than blocking the release.
     */
    private void resolveCategoryAndType(NcEvent nce) {
        try {
            NceCategory category = nceCategoryService.getAllNceCategories().stream()
                    .filter(candidate -> NCE_CATEGORY_ANALYSIS.equals(candidate.getName())).findFirst().orElse(null);
            if (category == null) {
                return;
            }
            nce.setNceCategoryId(category.getId());
            nceTypeService.getNceTypesByCategoryId(category.getId()).stream()
                    .filter(type -> NCE_TYPE_PROCEDURE_DEVIATION.equals(type.getName())).findFirst()
                    .ifPresent(type -> nce.setNceTypeId(type.getId()));
        } catch (RuntimeException e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "resolveCategoryAndType",
                    "Could not resolve the NCE category and type: " + e.getMessage());
        }
    }

    /**
     * The batch's analyses are the specimens the override released, so the NCE
     * names them the way a QC-failure NCE names its affected samples.
     */
    private void linkBatchSpecimens(NcEvent nce, BatchWorkplan batch, String sysUserId) {
        List<String> ids = batch.getItems().stream().map(BatchWorkplanItem::getAnalysisId).collect(Collectors.toList());
        for (Analysis analysis : ids.isEmpty() ? List.<Analysis>of()
                : analysisService.getAnalysesByIdsWithDetails(ids)) {
            NceSpecimen specimen = new NceSpecimen();
            specimen.setNceId(nce.getId());
            specimen.setAnalysisId(Integer.valueOf(analysis.getId()));
            if (analysis.getSampleItem() != null) {
                specimen.setSampleItemId(Integer.valueOf(analysis.getSampleItem().getId()));
            }
            specimen.setSysUserId(sysUserId);
            nceSpecimenService.save(specimen);
        }
    }

    private Optional<Integer> firstTestSectionId(List<BatchTestRef> tests) {
        return tests.stream().map(BatchTestRef::testSectionId).filter(StringUtils::isNumeric).findFirst()
                .map(Integer::valueOf);
    }

    private String labNumbersAndTests(BatchWorkplan batch) {
        List<String> ids = batch.getItems().stream().map(BatchWorkplanItem::getAnalysisId).collect(Collectors.toList());
        return analysisService.getAnalysesByIdsWithDetails(ids).stream().map(analysis -> {
            String labNumber = analysis.getSampleItem() != null && analysis.getSampleItem().getSample() != null
                    ? analysis.getSampleItem().getSample().getAccessionNumber()
                    : "?";
            String testName = analysis.getTest() == null ? "?" : analysis.getTest().getName();
            return labNumber + " " + testName;
        }).collect(Collectors.joining(", "));
    }

    private String reagentLabel(BatchWorkplan batch) {
        InventoryItem item = batch.getReagentId() == null ? null
                : inventoryItemService.getMatch("id", batch.getReagentId()).orElse(null);
        InventoryLot lot = batch.getReagentLotId() == null ? null
                : inventoryLotService.getMatch("id", batch.getReagentLotId()).orElse(null);
        return (item == null ? "?" : item.getName()) + " lot " + (lot == null ? "?" : lot.getLotNumber());
    }

    private List<ReagentLotOptionResponse> lotOptions(InventoryItem item) {
        List<InventoryLot> lots = inventoryLotService.getAvailableLotsByItemFEFO(item.getId());
        List<ReagentLotOptionResponse> options = new ArrayList<>();
        for (int index = 0; index < lots.size(); index++) {
            InventoryLot lot = lots.get(index);
            options.add(new ReagentLotOptionResponse(lot.getId(), lot.getLotNumber(), lot.getEffectiveExpirationDate(),
                    lot.getCurrentQuantity(), item.getUnits(), index == 0));
        }
        return options;
    }

    /** The analyzers bound to every test in the batch. */
    private List<InstrumentOptionResponse> instrumentOptions(List<BatchTestRef> tests) {
        Map<String, String> common = null;
        for (BatchTestRef test : tests) {
            Map<String, String> forTest = new LinkedHashMap<>();
            for (AnalyzerTestCapability capability : analyzerService.getCapabilitiesForTest(test.testId())) {
                forTest.putIfAbsent(capability.analyzerId(), capability.analyzerName());
            }
            if (common == null) {
                common = forTest;
            } else {
                common.keySet().retainAll(forTest.keySet());
            }
        }
        if (common == null) {
            return List.of();
        }
        return common.entrySet().stream().map(entry -> new InstrumentOptionResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private BatchWorkplan requireOwnBatch(Long id, String sysUserId) {
        Integer callerId = requireUserId(sysUserId);
        BatchWorkplan batch = id == null ? null : batchWorkplanDAO.getWithItems(id).orElse(null);
        if (batch == null || !Objects.equals(batch.getCreatedByUserId(), callerId)) {
            throw new BatchWorkplanNotFoundException(id);
        }
        return batch;
    }

    private void requireDraft(BatchWorkplan batch) {
        if (batch.getStatus() != BatchWorkplanStatus.DRAFT) {
            throw new IllegalArgumentException(
                    "Only a draft batch can be changed; use Edit to return an active batch to draft");
        }
    }

    private void validateName(String name) {
        if (name != null && name.trim().length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Batch name cannot exceed " + MAX_NAME_LENGTH + " characters");
        }
    }

    private void stampUpdate(BatchWorkplan batch, String sysUserId) {
        batch.setSysUserId(sysUserId);
        batch.setUpdatedByUserId(requireUserId(sysUserId));
    }

    private List<String> cleanIds(List<String> ids) {
        return ids.stream().filter(StringUtils::isNotBlank).map(String::trim).distinct().collect(Collectors.toList());
    }

    private List<BatchTestRef> testsOf(BatchWorkplan batch) {
        List<String> ids = batch.getItems().stream().map(BatchWorkplanItem::getAnalysisId).collect(Collectors.toList());
        Map<String, BatchTestRef> byTest = new LinkedHashMap<>();
        for (Analysis analysis : ids.isEmpty() ? List.<Analysis>of()
                : analysisService.getAnalysesByIdsWithDetails(ids)) {
            Test test = analysis.getTest();
            if (test == null) {
                continue;
            }
            TestSection section = analysis.getTestSection() != null ? analysis.getTestSection() : test.getTestSection();
            byTest.putIfAbsent(test.getId(),
                    new BatchTestRef(test.getId(), test.getName(), section == null ? null : section.getId()));
        }
        return new ArrayList<>(byTest.values());
    }

    /**
     * The subset of these analyses whose test falls in a lab unit the user holds
     * the Results role for. Same helper the Results worklist and the four legacy
     * workplan screens use, so all five scope identically.
     */
    private List<Analysis> inUserLabUnits(List<Analysis> analyses, String sysUserId) {
        if (analyses.isEmpty()) {
            return analyses;
        }
        return userService.filterAnalysesByLabUnitRoles(sysUserId, analyses, Constants.ROLE_RESULTS);
    }

    private void validatePending(List<Analysis> analyses) {
        Set<String> pendingStatuses = new LinkedHashSet<>(workplanPendingStatusIds());
        for (Analysis analysis : analyses) {
            if (!pendingStatuses.contains(analysis.getStatusId())) {
                throw new IllegalArgumentException("Analysis " + analysis.getId() + " is not pending workplan entry");
            }
        }
    }

    private List<String> workplanPendingStatusIds() {
        return Arrays.asList(statusService.getStatusID(AnalysisStatus.NotStarted),
                statusService.getStatusID(AnalysisStatus.BiologistRejected),
                statusService.getStatusID(AnalysisStatus.TechnicalRejected),
                statusService.getStatusID(AnalysisStatus.NonConforming_depricated));
    }

    /**
     * Every status except ARCHIVED, which is the one set three different questions
     * happen to share: which batches are still work in hand, which analyses a batch
     * still holds, and which analyses a new batch may not claim.
     *
     * <p>
     * COMPLETED is deliberately in the set. A completed batch keeps its analyses:
     * they carry results by then, so they are no longer pending and would not be
     * offered for batching anyway. The one case this leaves awkward is an analysis
     * reopened by a rejection or a retest, which stays held until its batch is
     * archived. Releasing it automatically needs the reopen path, which this
     * milestone does not build.
     */
    private List<BatchWorkplanStatus> unarchivedStatuses() {
        return Arrays.asList(BatchWorkplanStatus.DRAFT, BatchWorkplanStatus.ACTIVE, BatchWorkplanStatus.COMPLETED);
    }

    private int boundLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_PENDING_LIMIT;
        }
        return Math.min(limit, MAX_PENDING_LIMIT);
    }

    private Map<String, Analysis> analysesByIdFromAnalyses(List<Analysis> analyses) {
        Map<String, Analysis> byId = new HashMap<>();
        for (Analysis analysis : analyses) {
            byId.put(analysis.getId(), analysis);
        }
        return byId;
    }

    private Map<String, String> patientNames(List<Analysis> analyses) {
        Set<String> sampleIds = analyses.stream()
                .map(analysis -> analysis.getSampleItem() == null || analysis.getSampleItem().getSample() == null ? null
                        : analysis.getSampleItem().getSample().getId())
                .filter(Objects::nonNull).collect(Collectors.toSet());
        return sampleIds.isEmpty() ? Collections.emptyMap() : batchWorkplanDAO.getPatientNamesBySampleIds(sampleIds);
    }

    private BatchWorkplanResponse toBatchResponse(BatchWorkplan batch) {
        List<String> ids = batch.getItems().stream().map(BatchWorkplanItem::getAnalysisId).collect(Collectors.toList());
        List<Analysis> analyses = ids.isEmpty() ? List.of() : analysisService.getAnalysesByIdsWithDetails(ids);
        return toBatchResponse(batch, analysesByIdFromAnalyses(analyses), patientNames(analyses));
    }

    private BatchWorkplanResponse toBatchResponse(BatchWorkplan batch, Map<String, Analysis> analysesById,
            Map<String, String> patientNames) {
        BatchWorkplanResponse response = new BatchWorkplanResponse();
        response.setId(batch.getId());
        response.setName(batch.getName());
        response.setStatus(batch.getStatus());
        response.setTestSectionId(batch.getTestSectionId());
        response.setNotes(batch.getNotes());
        response.setCreatedAt(batch.getCreatedAt());
        response.setActivatedAt(batch.getActivatedAt());
        response.setCompletedAt(batch.getCompletedAt());
        response.setArchivedAt(batch.getArchivedAt());
        response.setItemCount(batch.getItems() == null ? 0 : batch.getItems().size());
        List<BatchWorkplanItemResponse> items = new ArrayList<>();
        Map<String, BatchTestRef> tests = new LinkedHashMap<>();
        if (batch.getItems() != null) {
            for (BatchWorkplanItem item : batch.getItems()) {
                BatchWorkplanItemResponse itemResponse = new BatchWorkplanItemResponse();
                Analysis analysis = analysesById.get(item.getAnalysisId());
                if (analysis != null) {
                    copyPendingFields(toPendingResponse(analysis, patientNames), itemResponse);
                    Test test = analysis.getTest();
                    if (test != null) {
                        TestSection section = analysis.getTestSection() != null ? analysis.getTestSection()
                                : test.getTestSection();
                        tests.putIfAbsent(test.getId(), new BatchTestRef(test.getId(), test.getName(),
                                section == null ? null : section.getId()));
                    }
                } else {
                    itemResponse.setAnalysisId(item.getAnalysisId());
                }
                itemResponse.setId(item.getId());
                itemResponse.setSortOrder(item.getSortOrder());
                items.add(itemResponse);
            }
        }
        response.setItems(items);

        response.setReagentId(batch.getReagentId());
        response.setReagentLotId(batch.getReagentLotId());
        if (batch.getReagentId() != null) {
            InventoryItem reagent = inventoryItemService.getMatch("id", batch.getReagentId()).orElse(null);
            response.setReagentName(reagent == null ? null : reagent.getName());
        }
        if (batch.getReagentLotId() != null) {
            InventoryLot lot = inventoryLotService.getMatch("id", batch.getReagentLotId()).orElse(null);
            if (lot != null) {
                response.setReagentLotNumber(lot.getLotNumber());
                response.setReagentLotExpiration(lot.getEffectiveExpirationDate());
            }
        }
        response.setInstrumentId(batch.getInstrumentId());
        response.setGeneratedAt(batch.getGeneratedAt());
        response.setGeneratedBy(batch.getGeneratedByUserId() == null ? null
                : userDisplayName(String.valueOf(batch.getGeneratedByUserId())));

        // The QC standing is shown once Phase 2 is done and while the batch is still
        // work to be run (FR-BL-008); a completed batch's QC is history, kept in its
        // release rows.
        boolean live = batch.getStatus() == BatchWorkplanStatus.DRAFT
                || batch.getStatus() == BatchWorkplanStatus.ACTIVE;
        if (live && batch.getReagentLotId() != null && !tests.isEmpty()) {
            BatchQcStatusResponse qc = qcService.evaluate(new ArrayList<>(tests.values()), batch.getInstrumentId());
            response.setQc(qc);
            response.setInstrumentName(qc.instrumentName());
        }
        response.setReleases(releases(batch));
        return response;
    }

    private List<BatchReleaseResponse> releases(BatchWorkplan batch) {
        if (batch.getId() == null) {
            return List.of();
        }
        List<BatchReleaseResponse> releases = new ArrayList<>();
        Map<Integer, String> nceNumbers = new HashMap<>();
        Map<String, String> testNames = new HashMap<>();
        Map<String, String> lotNumbers = new HashMap<>();
        for (BatchWorkplanQcRelease release : releaseDAO.getForBatch(batch.getId())) {
            String nceNumber = release.getNceId() == null ? null
                    : nceNumbers.computeIfAbsent(release.getNceId(), nceId -> {
                        NcEvent nce = ncEventService.getMatch("id", nceId).orElse(null);
                        return nce == null ? null : nce.getNceNumber();
                    });
            String lotNumber = release.getControlLotId() == null ? null
                    : lotNumbers.computeIfAbsent(release.getControlLotId(), lotId -> {
                        QCControlLot lot = controlLotService.getMatch("id", lotId).orElse(null);
                        return lot == null ? lotId : lot.getLotNumber();
                    });
            String testName = release.getTestId() == null ? null
                    : testNames.computeIfAbsent(release.getTestId(), testId -> {
                        Test test = analysisTest(batch, testId);
                        return test == null ? testId : test.getName();
                    });
            releases.add(new BatchReleaseResponse(release.getControlLotId(), lotNumber, testName, release.getQcStatus(),
                    release.isOverridden(), release.getNceId(), nceNumber, release.getReleasedAt(),
                    userDisplayName(String.valueOf(release.getReleasedByUserId()))));
        }
        return releases;
    }

    private Test analysisTest(BatchWorkplan batch, String testId) {
        List<String> ids = batch.getItems().stream().map(BatchWorkplanItem::getAnalysisId).collect(Collectors.toList());
        return ids.isEmpty() ? null
                : analysisService.getAnalysesByIdsWithDetails(ids).stream().map(Analysis::getTest)
                        .filter(test -> test != null && testId.equals(test.getId())).findFirst().orElse(null);
    }

    private String userDisplayName(String sysUserId) {
        if (StringUtils.isBlank(sysUserId)) {
            return null;
        }
        try {
            SystemUser user = systemUserService.getUserById(sysUserId);
            if (user != null) {
                String name = (StringUtils.trimToEmpty(user.getFirstName()) + " "
                        + StringUtils.trimToEmpty(user.getLastName())).trim();
                if (!name.isEmpty()) {
                    return name;
                }
            }
        } catch (RuntimeException e) {
            // fall through to the id
        }
        return sysUserId;
    }

    private PendingBatchTestResponse toPendingResponse(Analysis analysis, Map<String, String> patientNames) {
        PendingBatchTestResponse response = new PendingBatchTestResponse();
        response.setAnalysisId(analysis.getId());
        response.setStatusId(analysis.getStatusId());
        response.setStatusName(statusService.getStatusNameFromId(analysis.getStatusId()));
        response.setNonconforming(
                statusService.matches(analysis.getStatusId(), AnalysisStatus.NonConforming_depricated));

        SampleItem sampleItem = analysis.getSampleItem();
        if (sampleItem != null) {
            response.setSampleItemId(sampleItem.getId());
            TypeOfSample typeOfSample = sampleItem.getTypeOfSample();
            if (typeOfSample != null) {
                response.setSampleType(typeOfSample.getLocalizedName());
            }
            Sample sample = sampleItem.getSample();
            if (sample != null) {
                response.setSampleId(sample.getId());
                response.setAccessionNumber(sample.getAccessionNumber());
                response.setReceivedDate(sample.getReceivedDateForDisplay());
                response.setPatientName(patientNames.get(sample.getId()));
                response.setPriority(sample.getPriority() == null ? null : sample.getPriority().name());
                if (sample.getEnteredDate() != null) {
                    response.setOrderDate(new SimpleDateFormat("yyyy-MM-dd").format(sample.getEnteredDate()));
                    response.setOrderDateForDisplay(sample.getEnteredDateForDisplay());
                }
            }
        }

        Test test = analysis.getTest();
        if (test != null) {
            response.setTestId(test.getId());
            response.setTestName(test.getName());
        }

        Panel panel = analysis.getPanel();
        if (panel != null) {
            response.setPanelId(panel.getId());
            response.setPanelName(panel.getPanelName());
        }

        TestSection testSection = analysis.getTestSection();
        if (testSection != null) {
            response.setTestSectionId(testSection.getId());
            response.setTestSectionName(testSection.getTestSectionName());
        }

        Method method = analysis.getMethod();
        if (method != null) {
            response.setMethodId(method.getId());
            response.setMethodName(method.getLocalizedValue());
        }
        response.setGroupKey(buildGroupKey(response));
        return response;
    }

    private void copyPendingFields(PendingBatchTestResponse source, PendingBatchTestResponse target) {
        target.setAnalysisId(source.getAnalysisId());
        target.setAccessionNumber(source.getAccessionNumber());
        target.setSampleId(source.getSampleId());
        target.setSampleItemId(source.getSampleItemId());
        target.setReceivedDate(source.getReceivedDate());
        target.setTestId(source.getTestId());
        target.setTestName(source.getTestName());
        target.setTestSectionId(source.getTestSectionId());
        target.setTestSectionName(source.getTestSectionName());
        target.setMethodId(source.getMethodId());
        target.setMethodName(source.getMethodName());
        target.setSampleType(source.getSampleType());
        target.setStatusId(source.getStatusId());
        target.setStatusName(source.getStatusName());
        target.setGroupKey(source.getGroupKey());
        target.setNonconforming(source.isNonconforming());
        target.setPatientName(source.getPatientName());
        target.setPriority(source.getPriority());
        target.setPanelId(source.getPanelId());
        target.setPanelName(source.getPanelName());
        target.setOrderDate(source.getOrderDate());
        target.setOrderDateForDisplay(source.getOrderDateForDisplay());
    }

    private String buildGroupKey(PendingBatchTestResponse response) {
        String testId = StringUtils.defaultIfBlank(response.getTestId(), "unknown-test");
        String methodId = StringUtils.defaultIfBlank(response.getMethodId(), "manual");
        return testId + ":" + methodId;
    }

    /**
     * The caller's numeric id, or null when it is absent or unparseable. Null is
     * the safe answer on a read: the batch query treats it as "owns nothing" rather
     * than failing the request. Writes call {@link #requireUserId} instead, so a
     * caller we cannot identify gets a 400 rather than an unowned batch.
     */
    private Integer toUserId(String sysUserId) {
        if (StringUtils.isBlank(sysUserId)) {
            return null;
        }
        try {
            return Integer.valueOf(sysUserId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer requireUserId(String sysUserId) {
        Integer userId = toUserId(sysUserId);
        if (userId == null) {
            throw new IllegalArgumentException("Could not identify the requesting user");
        }
        return userId;
    }
}
