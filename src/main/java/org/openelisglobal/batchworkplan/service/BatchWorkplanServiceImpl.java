package org.openelisglobal.batchworkplan.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.batchworkplan.dao.BatchWorkplanDAO;
import org.openelisglobal.batchworkplan.dao.BatchWorkplanItemDAO;
import org.openelisglobal.batchworkplan.form.BatchWorkplanItemResponse;
import org.openelisglobal.batchworkplan.form.BatchWorkplanRequest;
import org.openelisglobal.batchworkplan.form.BatchWorkplanResponse;
import org.openelisglobal.batchworkplan.form.PendingBatchTestResponse;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplan;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplanItem;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplanStatus;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BatchWorkplanServiceImpl implements BatchWorkplanService {

    private static final int DEFAULT_PENDING_LIMIT = 100;
    private static final int MAX_PENDING_LIMIT = 500;
    private static final DateTimeFormatter BATCH_NAME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final BatchWorkplanDAO batchWorkplanDAO;
    private final BatchWorkplanItemDAO batchWorkplanItemDAO;
    private final AnalysisService analysisService;
    private final IStatusService statusService;

    public BatchWorkplanServiceImpl(BatchWorkplanDAO batchWorkplanDAO, BatchWorkplanItemDAO batchWorkplanItemDAO,
            AnalysisService analysisService, IStatusService statusService) {
        this.batchWorkplanDAO = batchWorkplanDAO;
        this.batchWorkplanItemDAO = batchWorkplanItemDAO;
        this.analysisService = analysisService;
        this.statusService = statusService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingBatchTestResponse> getPendingTests(Integer limit) {
        int boundedLimit = boundLimit(limit);
        Set<String> assignedAnalysisIds = batchWorkplanItemDAO.getAnalysisIdsInStatuses(openStatuses());
        List<Analysis> analyses = analysisService.getAllAnalysisByStatus(workplanPendingStatusIds(), MAX_PENDING_LIMIT);
        List<PendingBatchTestResponse> responses = new ArrayList<>();
        for (Analysis analysis : analyses) {
            if (assignedAnalysisIds.contains(analysis.getId())) {
                continue;
            }
            responses.add(toPendingResponse(analysis));
            if (responses.size() >= boundedLimit) {
                break;
            }
        }
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BatchWorkplanResponse> getBatches() {
        List<BatchWorkplan> batches = batchWorkplanDAO.getAllWithItems();
        Set<String> analysisIds = batches.stream().flatMap(batch -> batch.getItems().stream())
                .map(BatchWorkplanItem::getAnalysisId).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, Analysis> analysesById = analysesById(new ArrayList<>(analysisIds));
        return batches.stream().map(batch -> toBatchResponse(batch, analysesById)).collect(Collectors.toList());
    }

    @Override
    public BatchWorkplanResponse createBatch(BatchWorkplanRequest request, String sysUserId) {
        if (request == null || request.getAnalysisIds() == null || request.getAnalysisIds().isEmpty()) {
            throw new IllegalArgumentException("At least one analysis is required to create a batch workplan");
        }

        List<String> analysisIds = request.getAnalysisIds().stream().filter(StringUtils::isNotBlank).distinct()
                .collect(Collectors.toList());
        if (analysisIds.isEmpty()) {
            throw new IllegalArgumentException("At least one analysis is required to create a batch workplan");
        }

        Set<String> alreadyAssigned = batchWorkplanItemDAO.getExistingAnalysisIds(analysisIds, openStatuses());
        if (!alreadyAssigned.isEmpty()) {
            throw new IllegalArgumentException("Analyses already assigned to an open batch: " + alreadyAssigned);
        }

        List<Analysis> analyses = analysisService.getAnalysesByIdsWithDetails(analysisIds);
        if (analyses.size() != analysisIds.size()) {
            throw new IllegalArgumentException("One or more analyses were not found");
        }
        validatePending(analyses);

        BatchWorkplan batch = new BatchWorkplan();
        batch.setName(StringUtils.defaultIfBlank(request.getName(),
                "Batch " + LocalDateTime.now().format(BATCH_NAME_FORMAT)));
        batch.setStatus(BatchWorkplanStatus.DRAFT);
        batch.setTestSectionId(StringUtils.trimToNull(request.getTestSectionId()));
        batch.setNotes(StringUtils.trimToNull(request.getNotes()));
        batch.setCreatedAt(Timestamp.from(Instant.now()));
        batch.setSysUserId(sysUserId);
        batch.setCreatedByUserId(toUserId(sysUserId));
        batch.setUpdatedByUserId(toUserId(sysUserId));

        for (int index = 0; index < analysisIds.size(); index++) {
            BatchWorkplanItem item = new BatchWorkplanItem();
            item.setAnalysisId(analysisIds.get(index));
            item.setSortOrder(index + 1);
            batch.addItem(item);
        }

        Long id = batchWorkplanDAO.insert(batch);
        BatchWorkplan saved = batchWorkplanDAO.getWithItems(id).orElse(batch);
        return toBatchResponse(saved, analysesByIdFromAnalyses(analyses));
    }

    @Override
    public BatchWorkplanResponse transitionBatch(Long id, BatchWorkplanStatus nextStatus, String sysUserId) {
        if (nextStatus == null) {
            throw new IllegalArgumentException("Batch status is required");
        }
        BatchWorkplan batch = batchWorkplanDAO.getWithItems(id)
                .orElseThrow(() -> new LIMSRuntimeException("Batch workplan not found: " + id));
        BatchWorkplanStatus currentStatus = batch.getStatus();
        if (!currentStatus.canTransitionTo(nextStatus)) {
            throw new IllegalArgumentException(
                    "Cannot transition batch workplan from " + currentStatus + " to " + nextStatus);
        }

        Timestamp now = Timestamp.from(Instant.now());
        batch.setStatus(nextStatus);
        batch.setSysUserId(sysUserId);
        batch.setUpdatedByUserId(toUserId(sysUserId));
        if (nextStatus == BatchWorkplanStatus.ACTIVE) {
            batch.setActivatedAt(now);
        } else if (nextStatus == BatchWorkplanStatus.COMPLETED) {
            batch.setCompletedAt(now);
        } else if (nextStatus == BatchWorkplanStatus.ARCHIVED) {
            batch.setArchivedAt(now);
        }

        BatchWorkplan updated = batchWorkplanDAO.update(batch);
        return toBatchResponse(updated, analysesById(
                updated.getItems().stream().map(BatchWorkplanItem::getAnalysisId).collect(Collectors.toList())));
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

    private List<BatchWorkplanStatus> openStatuses() {
        return Arrays.asList(BatchWorkplanStatus.DRAFT, BatchWorkplanStatus.ACTIVE, BatchWorkplanStatus.COMPLETED);
    }

    private int boundLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_PENDING_LIMIT;
        }
        return Math.min(limit, MAX_PENDING_LIMIT);
    }

    private Map<String, Analysis> analysesById(List<String> analysisIds) {
        if (analysisIds == null || analysisIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return analysesByIdFromAnalyses(analysisService.getAnalysesByIdsWithDetails(analysisIds));
    }

    private Map<String, Analysis> analysesByIdFromAnalyses(List<Analysis> analyses) {
        Map<String, Analysis> byId = new HashMap<>();
        for (Analysis analysis : analyses) {
            byId.put(analysis.getId(), analysis);
        }
        return byId;
    }

    private BatchWorkplanResponse toBatchResponse(BatchWorkplan batch, Map<String, Analysis> analysesById) {
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
        if (batch.getItems() != null) {
            for (BatchWorkplanItem item : batch.getItems()) {
                BatchWorkplanItemResponse itemResponse = new BatchWorkplanItemResponse();
                Analysis analysis = analysesById.get(item.getAnalysisId());
                if (analysis != null) {
                    copyPendingFields(toPendingResponse(analysis), itemResponse);
                } else {
                    itemResponse.setAnalysisId(item.getAnalysisId());
                }
                itemResponse.setId(item.getId());
                itemResponse.setSortOrder(item.getSortOrder());
                items.add(itemResponse);
            }
        }
        response.setItems(items);
        return response;
    }

    private PendingBatchTestResponse toPendingResponse(Analysis analysis) {
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
            }
        }

        Test test = analysis.getTest();
        if (test != null) {
            response.setTestId(test.getId());
            response.setTestName(test.getName());
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
    }

    private String buildGroupKey(PendingBatchTestResponse response) {
        String testId = StringUtils.defaultIfBlank(response.getTestId(), "unknown-test");
        String methodId = StringUtils.defaultIfBlank(response.getMethodId(), "manual");
        return testId + ":" + methodId;
    }

    private Integer toUserId(String sysUserId) {
        if (StringUtils.isBlank(sysUserId)) {
            return null;
        }
        return Integer.valueOf(sysUserId);
    }
}
