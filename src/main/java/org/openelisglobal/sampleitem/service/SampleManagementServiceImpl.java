/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 */
package org.openelisglobal.sampleitem.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.dao.SampleItemDAO;
import org.openelisglobal.sampleitem.dto.AddTestsResponse;
import org.openelisglobal.sampleitem.dto.AliquotSummaryDTO;
import org.openelisglobal.sampleitem.dto.CancelTestResponse;
import org.openelisglobal.sampleitem.dto.CreateAliquotResponse;
import org.openelisglobal.sampleitem.dto.SampleItemDTO;
import org.openelisglobal.sampleitem.dto.SearchSamplesResponse;
import org.openelisglobal.sampleitem.dto.TestSummaryDTO;
import org.openelisglobal.sampleitem.form.AddTestsForm;
import org.openelisglobal.sampleitem.form.CancelTestForm;
import org.openelisglobal.sampleitem.form.CreateAliquotForm;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of SampleManagementService.
 *
 * <p>
 * Provides business logic for sample management operations including search,
 * aliquoting, and test management. All public methods execute within
 * transactions per Constitution III.6 (@Transactional in services only).
 *
 * <p>
 * Data Transfer Objects are compiled WITHIN transaction boundaries to prevent
 * LazyInitializationException per Constitution III.7.
 *
 * <p>
 * Related: Feature 001-sample-management
 */
@Service
public class SampleManagementServiceImpl implements SampleManagementService {

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemDAO sampleItemDAO;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private TestService testService;

    @Override
    @Transactional(readOnly = true)
    public SearchSamplesResponse searchByAccessionNumber(String accessionNumber, boolean includeTests) {
        // Step 1: Find sample by accession number
        Sample sample = sampleService.getSampleByAccessionNumber(accessionNumber);

        // Step 2: If no sample found, return empty results
        if (sample == null) {
            return new SearchSamplesResponse(accessionNumber, new ArrayList<>(), 0);
        }

        // Step 3: Get all sample items for this sample with hierarchy eagerly loaded
        List<SampleItem> sampleItems = sampleItemDAO.getSampleItemsBySampleId(sample.getId());
        System.out.println("SampleManagementServiceImpl.searchByAccessionNumber: Retrieved " + sampleItems.size()
                + " sample items for accession number " + accessionNumber);

        // Step 4: If hierarchy is needed, use getSampleItemsWithHierarchy for eager
        // loading
        if (!sampleItems.isEmpty()) {
            List<String> sampleItemIds = sampleItems.stream().map(SampleItem::getId).collect(Collectors.toList());
            sampleItems = sampleItemDAO.getSampleItemsWithHierarchy(sampleItemIds);
        }
        System.out.println("SampleManagementServiceImpl.searchByAccessionNumber: Retrieved2 " + sampleItems.size()
                + " sample items for accession number " + accessionNumber);
        // Step 5: Convert entities to DTOs WITHIN transaction boundary
        List<SampleItemDTO> dtos = sampleItems.stream().map(item -> convertToDTO(item, includeTests))
                .collect(Collectors.toList());

        // Step 6: Return response with results
        return new SearchSamplesResponse(accessionNumber, dtos, dtos.size());
    }

    @Override
    @Transactional
    public CreateAliquotResponse createAliquot(CreateAliquotForm form, String sysUserId) {
        // Step 1: Validate and load parent sample item
        SampleItem parent = sampleItemService.getData(form.getParentSampleItemId());
        if (parent == null) {
            throw new IllegalArgumentException("Parent sample item not found: " + form.getParentSampleItemId());
        }

        BigDecimal totalQuantityToTransfer = form.getQuantityToTransfer();
        int numberOfAliquots = form.getNumberOfAliquots();

        // Default to 1 if not specified or invalid
        if (numberOfAliquots < 1) {
            numberOfAliquots = 1;
        }

        // Step 2: Validate quantity to transfer is positive
        if (totalQuantityToTransfer.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity to transfer must be greater than 0");
        }

        // Step 3: Calculate quantity per aliquot (divide equally)
        BigDecimal quantityPerAliquot = totalQuantityToTransfer.divide(BigDecimal.valueOf(numberOfAliquots), 6,
                java.math.RoundingMode.HALF_UP);

        // Validate minimum quantity per aliquot
        BigDecimal minQuantity = new BigDecimal("0.001");
        if (quantityPerAliquot.compareTo(minQuantity) < 0) {
            throw new IllegalArgumentException(String.format(
                    "Cannot create %d aliquots: quantity per aliquot (%s) would be less than minimum (0.001)",
                    numberOfAliquots, quantityPerAliquot));
        }

        // Step 4: Validate parent can aliquot the total requested quantity
        if (!parent.canAliquot(totalQuantityToTransfer)) {
            BigDecimal parentRemaining = parent.getEffectiveRemainingQuantity();
            if (parentRemaining == null || parentRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException(
                        "Cannot aliquot: parent sample item has no remaining quantity (all volume dispensed)");
            } else {
                throw new IllegalArgumentException(
                        String.format("Cannot aliquot: requested total volume (%s) exceeds remaining volume (%s)",
                                totalQuantityToTransfer, parentRemaining));
            }
        }

        // Step 5: Calculate starting sequence number
        int nextSequence = parent.getChildAliquots() != null ? parent.getChildAliquots().size() + 1 : 1;

        // Step 6: Create all aliquots
        List<SampleItem> createdAliquots = new ArrayList<>();
        for (int i = 0; i < numberOfAliquots; i++) {
            // Generate aliquot external ID with .{n} suffix
            String aliquotExternalId = parent.getExternalId() + "." + (nextSequence + i);

            // Create new aliquot sample item
            SampleItem aliquot = new SampleItem();
            aliquot.setSample(parent.getSample());
            aliquot.setTypeOfSample(parent.getTypeOfSample());
            aliquot.setUnitOfMeasure(parent.getUnitOfMeasure());
            aliquot.setExternalId(aliquotExternalId);
            aliquot.setQuantity(quantityPerAliquot.doubleValue()); // Store as quantity
            aliquot.setRemainingQuantity(quantityPerAliquot); // Initially, remaining = transferred
            aliquot.setParentSampleItem(parent);
            aliquot.setCollectionDate(parent.getCollectionDate());
            aliquot.setStatusId(parent.getStatusId());
            aliquot.setSortOrder(parent.getSortOrder());
            aliquot.setSysUserId(sysUserId); // Set for audit trail

            createdAliquots.add(aliquot);
        }

        // Step 7: Decrement parent's remaining quantity by total amount
        parent.decrementRemainingQuantity(totalQuantityToTransfer);
        parent.setSysUserId(sysUserId); // Set for audit trail

        // Step 8: Save all entities (parent update + all aliquots insert)
        sampleItemService.update(parent);
        for (SampleItem aliquot : createdAliquots) {
            sampleItemService.save(aliquot);
        }

        // Step 9: Convert to DTOs WITHIN transaction boundary
        List<SampleItemDTO> aliquotDTOs = createdAliquots.stream().map(aliquot -> convertToDTO(aliquot, false))
                .collect(Collectors.toList());
        BigDecimal updatedParentRemaining = parent.getEffectiveRemainingQuantity();

        // Step 10: Return response
        String message;
        if (numberOfAliquots == 1) {
            message = String.format(
                    "Aliquot %s created successfully from parent %s. Parent remaining quantity updated to %s.",
                    aliquotDTOs.get(0).getExternalId(), parent.getExternalId(), updatedParentRemaining);
        } else {
            String aliquotIds = createdAliquots.stream().map(SampleItem::getExternalId)
                    .collect(Collectors.joining(", "));
            message = String.format(
                    "%d aliquots created successfully from parent %s (%s each): %s. Parent remaining quantity updated to %s.",
                    numberOfAliquots, parent.getExternalId(), quantityPerAliquot, aliquotIds, updatedParentRemaining);
        }

        return new CreateAliquotResponse(aliquotDTOs, updatedParentRemaining, quantityPerAliquot, message);
    }

    /**
     * Convert SampleItem entity to DTO with all relationships loaded.
     *
     * <p>
     * This method MUST be called within transaction boundaries to access
     * lazy-loaded associations (parent, children, tests).
     *
     * @param sampleItem   the entity to convert
     * @param includeTests whether to load ordered tests
     * @return populated DTO
     */
    private SampleItemDTO convertToDTO(SampleItem sampleItem, boolean includeTests) {
        SampleItemDTO dto = new SampleItemDTO();

        // Basic fields
        dto.setId(sampleItem.getId());
        dto.setExternalId(sampleItem.getExternalId());
        dto.setSampleAccessionNumber(
                sampleItem.getSample() != null ? sampleItem.getSample().getAccessionNumber() : null);

        // Sample type
        if (sampleItem.getTypeOfSample() != null) {
            dto.setSampleType(sampleItem.getTypeOfSample().getDescription());
            dto.setSampleTypeId(sampleItem.getTypeOfSample().getId());
        }

        // Quantity fields (use quantity column as original, remainingQuantity for
        // aliquoting)
        dto.setQuantity(sampleItem.getQuantity());
        dto.setRemainingQuantity(sampleItem.getRemainingQuantity());

        // Unit of measure
        if (sampleItem.getUnitOfMeasure() != null) {
            dto.setUnitOfMeasure(sampleItem.getUnitOfMeasure().getUnitOfMeasureName());
            dto.setUnitOfMeasureId(sampleItem.getUnitOfMeasure().getId());
        }

        // Status
        if (sampleItem.getStatusId() != null) {
            dto.setStatusId(sampleItem.getStatusId());
            // Status description would come from status service if needed
        }

        // Collection date
        dto.setCollectionDate(sampleItem.getCollectionDate());

        // Parent-child relationships (eagerly loaded via getSampleItemsWithHierarchy)
        if (sampleItem.getParentSampleItem() != null) {
            dto.setParentId(sampleItem.getParentSampleItem().getId());
            dto.setParentExternalId(sampleItem.getParentSampleItem().getExternalId());
        }

        // Child aliquots
        if (sampleItem.getChildAliquots() != null && !sampleItem.getChildAliquots().isEmpty()) {
            List<AliquotSummaryDTO> childDtos = sampleItem.getChildAliquots().stream()
                    .map(this::convertToAliquotSummary).collect(Collectors.toList());
            dto.setChildAliquots(childDtos);
        }

        // Computed fields
        dto.setHasRemainingQuantity(sampleItem.hasRemainingQuantity());
        dto.setAliquot(sampleItem.isAliquot());
        dto.setNestingLevel(sampleItem.getNestingLevel());

        // Ordered tests (if requested) - exclude cancelled tests
        if (includeTests) {
            List<Analysis> analyses = analysisService.getAnalysesBySampleItem(sampleItem);
            if (analyses != null && !analyses.isEmpty()) {
                // Filter out cancelled tests
                IStatusService statusService = StatusService.getInstance();
                String cancelledStatusId = statusService.getStatusID(StatusService.AnalysisStatus.Canceled);

                List<TestSummaryDTO> testDtos = analyses.stream()
                        .filter(a -> !cancelledStatusId.equals(a.getStatusId())).map(this::convertToTestSummary)
                        .collect(Collectors.toList());
                dto.setOrderedTests(testDtos);
            }
        }

        // Metadata
        dto.setLastupdated(sampleItem.getLastupdated());

        return dto;
    }

    /**
     * Convert SampleItem to lightweight AliquotSummaryDTO.
     *
     * @param sampleItem the aliquot entity
     * @return summary DTO
     */
    private AliquotSummaryDTO convertToAliquotSummary(SampleItem sampleItem) {
        AliquotSummaryDTO dto = new AliquotSummaryDTO();
        dto.setId(sampleItem.getId());
        dto.setExternalId(sampleItem.getExternalId());
        dto.setQuantity(sampleItem.getQuantity());
        dto.setRemainingQuantity(sampleItem.getRemainingQuantity());
        dto.setCreatedDate(sampleItem.getLastupdated());
        return dto;
    }

    /**
     * Convert Analysis entity to TestSummaryDTO.
     *
     * @param analysis the analysis entity
     * @return test summary DTO
     */
    private TestSummaryDTO convertToTestSummary(Analysis analysis) {
        TestSummaryDTO dto = new TestSummaryDTO();
        dto.setAnalysisId(analysis.getId());

        if (analysis.getTest() != null) {
            dto.setTestId(analysis.getTest().getId());
            // Use getName() or getDescription() for test name
            String testName = analysis.getTest().getName() != null ? analysis.getTest().getName()
                    : analysis.getTest().getDescription();
            dto.setTestName(testName);
        }

        if (analysis.getStatusId() != null) {
            dto.setStatus(analysis.getStatusId());
        }

        // StartedDate is java.sql.Date, but TestSummaryDTO expects Timestamp
        // Convert Date to Timestamp
        if (analysis.getStartedDate() != null) {
            dto.setOrderedDate(new java.sql.Timestamp(analysis.getStartedDate().getTime()));
        }

        return dto;
    }

    @Override
    @Transactional
    public AddTestsResponse addTestsToSamples(AddTestsForm form, String sysUserId) {
        List<AddTestsResponse.TestAdditionResult> results = new ArrayList<>();
        int successCount = 0;

        // Step 1: Validate and cache tests
        List<Test> tests = new ArrayList<>();
        for (String testId : form.getTestIds()) {
            Test test = testService.getTestById(testId);
            if (test == null) {
                throw new IllegalArgumentException("Test not found: " + testId);
            }
            tests.add(test);
        }

        // Step 2: Validate and process each sample item
        for (String sampleItemId : form.getSampleItemIds()) {
            SampleItem sampleItem = sampleItemService.getData(sampleItemId);
            if (sampleItem == null) {
                throw new IllegalArgumentException("Sample item not found: " + sampleItemId);
            }

            // Step 2a: Validate that sample has remaining quantity (not all dispensed)
            if (!sampleItem.hasRemainingQuantity()) {
                throw new IllegalStateException(
                        String.format("Cannot add tests to sample item %s: all volume has been dispensed",
                                sampleItem.getExternalId() != null ? sampleItem.getExternalId() : sampleItemId));
            }

            // Process each test for this sample item
            AddTestsResponse.TestAdditionResult result = new AddTestsResponse.TestAdditionResult();
            result.setSampleItemId(sampleItemId);
            result.setSampleItemExternalId(sampleItem.getExternalId());
            result.setSuccess(true);

            List<String> addedTestIds = new ArrayList<>();
            List<String> skippedTestIds = new ArrayList<>();

            for (Test test : tests) {
                // Check for duplicate (test already ordered for this sample item)
                Analysis existingAnalysis = analysisService.getAnalysisBySampleItemAndTest(sampleItemId, test.getId());

                if (existingAnalysis != null) {
                    // Skip duplicate
                    skippedTestIds.add(test.getId());
                } else {
                    // Create new Analysis
                    Analysis analysis = analysisService.buildAnalysis(test, sampleItem);
                    analysis.setSysUserId(sysUserId);

                    // Save the analysis
                    analysisService.insert(analysis);
                    addedTestIds.add(test.getId());
                    successCount++;
                }
            }

            result.setAddedTestIds(addedTestIds);
            result.setSkippedTestIds(skippedTestIds);

            // Set message based on results
            if (addedTestIds.isEmpty() && !skippedTestIds.isEmpty()) {
                result.setMessage("All tests already ordered for this sample");
            } else if (!skippedTestIds.isEmpty()) {
                result.setMessage(String.format("%d test(s) added, %d test(s) skipped (already ordered)",
                        addedTestIds.size(), skippedTestIds.size()));
            } else {
                result.setMessage(String.format("%d test(s) added successfully", addedTestIds.size()));
            }

            results.add(result);
        }

        return new AddTestsResponse(successCount, results);
    }

    @Override
    @Transactional
    public CancelTestResponse cancelTest(CancelTestForm form, String sysUserId) {
        // Step 1: Load the analysis
        Analysis analysis = analysisService.getAnalysisById(form.getAnalysisId());
        if (analysis == null) {
            throw new IllegalArgumentException("Analysis not found: " + form.getAnalysisId());
        }

        // Step 2: Validate analysis belongs to the specified sample item
        if (analysis.getSampleItem() == null || !analysis.getSampleItem().getId().equals(form.getSampleItemId())) {
            throw new IllegalArgumentException("Analysis does not belong to specified sample item");
        }

        // Step 3: Check if analysis can be cancelled (not already completed/finalized)
        IStatusService statusService = StatusService.getInstance();
        String currentStatusId = analysis.getStatusId();

        // Analysis can only be cancelled if status is NotStarted or TechnicalAcceptance
        boolean canCancel = statusService.matches(currentStatusId, StatusService.AnalysisStatus.NotStarted)
                || statusService.matches(currentStatusId, StatusService.AnalysisStatus.TechnicalAcceptance);

        if (!canCancel) {
            String statusName = statusService.getStatusNameFromId(currentStatusId);
            throw new IllegalStateException(String.format("Cannot cancel test: analysis is already %s",
                    statusName != null ? statusName : "in a non-cancellable state"));
        }

        // Step 4: Get test name for response before updating
        String testName = analysis.getTest() != null
                ? (analysis.getTest().getName() != null ? analysis.getTest().getName()
                        : analysis.getTest().getDescription())
                : "Unknown Test";

        // Step 5: Set status to Canceled
        String canceledStatusId = statusService.getStatusID(StatusService.AnalysisStatus.Canceled);
        analysis.setStatusId(canceledStatusId);
        analysis.setSysUserId(sysUserId);

        // Step 6: Update the analysis
        analysisService.update(analysis);

        // Step 7: Return success response
        return new CancelTestResponse(analysis.getId(), testName, true,
                String.format("Test '%s' has been cancelled successfully", testName));
    }
}
