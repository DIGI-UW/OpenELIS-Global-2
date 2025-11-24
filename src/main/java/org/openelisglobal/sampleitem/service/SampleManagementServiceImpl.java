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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.dao.SampleItemDAO;
import org.openelisglobal.sampleitem.dto.AliquotSummaryDTO;
import org.openelisglobal.sampleitem.dto.SampleItemDTO;
import org.openelisglobal.sampleitem.dto.SearchSamplesResponse;
import org.openelisglobal.sampleitem.dto.TestSummaryDTO;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
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
    private AnalysisService analysisService;

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

        // Step 4: If hierarchy is needed, use getSampleItemsWithHierarchy for eager
        // loading
        if (!sampleItems.isEmpty()) {
            List<String> sampleItemIds = sampleItems.stream().map(SampleItem::getId).collect(Collectors.toList());
            sampleItems = sampleItemDAO.getSampleItemsWithHierarchy(sampleItemIds);
        }

        // Step 5: Convert entities to DTOs WITHIN transaction boundary
        List<SampleItemDTO> dtos = sampleItems.stream().map(item -> convertToDTO(item, includeTests))
                .collect(Collectors.toList());

        // Step 6: Return response with results
        return new SearchSamplesResponse(accessionNumber, dtos, dtos.size());
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

        // Quantity fields
        dto.setOriginalQuantity(sampleItem.getOriginalQuantity());
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

        // Ordered tests (if requested)
        if (includeTests) {
            List<Analysis> analyses = analysisService.getAnalysesBySampleItem(sampleItem);
            if (analyses != null && !analyses.isEmpty()) {
                List<TestSummaryDTO> testDtos = analyses.stream().map(this::convertToTestSummary)
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
        dto.setOriginalQuantity(sampleItem.getOriginalQuantity());
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
}
