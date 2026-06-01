package org.openelisglobal.biorepository.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.biorepository.controller.rest.dto.BioSampleListDTO;
import org.openelisglobal.biorepository.dao.BioSampleRetrievalSearchCriteria;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BioSample.WorkflowStatus;
import org.openelisglobal.department.service.DepartmentIsolationService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.service.SampleStorageService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;

@RunWith(MockitoJUnitRunner.class)
public class BioSampleFulfillmentSearchTest {

    @InjectMocks
    private BioSampleFulfillmentSearchServiceImpl fulfillmentSearchService;

    @Mock
    private BioSampleService bioSampleService;

    @Mock
    private DepartmentIsolationService departmentIsolationService;

    @Mock
    private SampleStorageService sampleStorageService;

    @Mock
    private RetentionPolicyService retentionPolicyService;

    @Mock
    private HttpServletRequest httpRequest;

    @Before
    public void setUp() {
        when(departmentIsolationService.hasUnrestrictedDepartmentAccess(any())).thenReturn(true);
        when(sampleStorageService.getSampleItemLocation(any())).thenReturn(java.util.Map.of());
    }

    @Test
    public void search_returnsExactAccessionMatchInPhase1() {
        BioSample exact = storedSample("BIO-1", "ACC-EXACT", "CTD", "PROJ", "Plasma");
        when(bioSampleService.searchForRetrieval(any())).thenReturn(List.of(exact));

        FulfillmentSearchInput input = new FulfillmentSearchInput();
        input.setFilterStatus(WorkflowStatus.STORED);
        input.setAccessionNumber("ACC-EXACT");
        input.setLimit(10);

        FulfillmentSearchOutcome outcome = fulfillmentSearchService.search(input, httpRequest);

        assertFalse(outcome.getCandidates().isEmpty());
        assertEquals("ACC-EXACT", outcome.getCandidates().get(0).getAccessionNumber());
        assertTrue(outcome.getMetadata().isExactIdentityMatchesFound());
        assertEquals(1, outcome.getMetadata().getSearchPhase());
        assertTrue(Boolean.TRUE.equals(outcome.getCandidates().get(0).getExactIdentityMatch()));
    }

    @Test
    public void search_setsFallbackWhenOnlyPartialIdentityMatches() {
        BioSample partial = storedSample("BIO-PART", "ACC-PARTIAL-XYZ", "CTD", "PROJ", "Plasma");
        when(bioSampleService.searchForRetrieval(any())).thenReturn(List.of(partial));

        FulfillmentSearchInput input = new FulfillmentSearchInput();
        input.setFilterStatus(WorkflowStatus.STORED);
        input.setAccessionNumber("PART");
        input.setLimit(10);

        FulfillmentSearchOutcome outcome = fulfillmentSearchService.search(input, httpRequest);

        assertFalse(outcome.getCandidates().isEmpty());
        assertTrue(outcome.getMetadata().isFallbackUsed());
        assertTrue(outcome.getMetadata().isNoExactMatch());
        assertEquals(4, outcome.getMetadata().getSearchPhase());
    }

    @Test
    public void search_resolvesStoredSampleWithoutBioSampleRowViaPhase0() {
        BioSample storedOnly = storedSample("BIO-STORED", "DEV012600000000000050", "CTD", "PROJ", "DNA");
        SampleItem sampleItem = storedOnly.getSampleItem();

        when(sampleStorageService.resolveSampleItemByIdentifier("DEV012600000000000050")).thenReturn(sampleItem);
        when(bioSampleService.ensureBioSampleForStoredSampleItem(eq(sampleItem), isNull())).thenReturn(storedOnly);
        when(sampleStorageService.getSampleItemLocation("100"))
                .thenReturn(java.util.Map.of("location", "Room > Device", "hierarchicalPath", "Room > Device"));

        FulfillmentSearchInput input = new FulfillmentSearchInput();
        input.setFilterStatus(WorkflowStatus.STORED);
        input.setAccessionNumber("DEV012600000000000050");
        input.setSampleType("blood");
        input.setLimit(10);

        FulfillmentSearchOutcome outcome = fulfillmentSearchService.search(input, httpRequest);

        assertFalse(outcome.getCandidates().isEmpty());
        assertEquals("DEV012600000000000050", outcome.getCandidates().get(0).getAccessionNumber());
        assertTrue(outcome.getMetadata().isExactIdentityMatchesFound());
        assertEquals(0, outcome.getMetadata().getSearchPhase());
    }

    @Test
    public void search_exactAccessionIgnoresSampleTypeFilter() {
        BioSample exact = storedSample("BIO-2", "DEV012600000000000050", "CTD", "PROJ", "DNA");
        when(bioSampleService.searchForRetrieval(argThat((BioSampleRetrievalSearchCriteria criteria) ->
                criteria != null && criteria.getIdentityPattern() != null && criteria.getSampleTypeIds() == null)))
                .thenReturn(List.of(exact));

        FulfillmentSearchInput input = new FulfillmentSearchInput();
        input.setFilterStatus(WorkflowStatus.STORED);
        input.setAccessionNumber("DEV012600000000000050");
        input.setSampleType("blood");
        input.setLimit(10);

        FulfillmentSearchOutcome outcome = fulfillmentSearchService.search(input, httpRequest);

        assertFalse(outcome.getCandidates().isEmpty());
        assertEquals("DEV012600000000000050", outcome.getCandidates().get(0).getAccessionNumber());
        assertTrue(outcome.getMetadata().isExactIdentityMatchesFound());
        assertEquals(1, outcome.getMetadata().getSearchPhase());
    }

    @Test
    public void search_runsDescriptionPhaseWhenSampleTypeTextDoesNotResolveToIds() {
        BioSample candidate = storedSample("BIO-TYPE", "ACC-TYPE", "CTD", "PROJ", "Plasma");
        when(bioSampleService.searchForRetrieval(argThat((BioSampleRetrievalSearchCriteria criteria) -> criteria != null
                && "%blood%".equals(criteria.getSampleTypeDescriptionPattern())
                && (criteria.getSampleTypeIds() == null || criteria.getSampleTypeIds().isEmpty()))))
                .thenReturn(List.of(candidate));

        FulfillmentSearchInput input = new FulfillmentSearchInput();
        input.setFilterStatus(WorkflowStatus.STORED);
        input.setSampleType("blood");
        input.setLimit(10);

        FulfillmentSearchOutcome outcome = fulfillmentSearchService.search(input, httpRequest);

        assertFalse(outcome.getCandidates().isEmpty());
        assertEquals(5, outcome.getMetadata().getSearchPhase());
    }

    @Test
    public void search_exactSampleIdMatchesBarcodeLane() {
        BioSample exact = storedSample("BIO-2026-004", "DEV012600000000000050", "CTD", "PROJ", "DNA");
        when(bioSampleService.searchForRetrieval(argThat((BioSampleRetrievalSearchCriteria criteria) ->
                criteria != null
                        && "%BIO-2026-004%".equals(criteria.getIdentityPattern())
                        && criteria.getBarcodePattern() == null
                        && criteria.getAccessionPattern() == null)))
                .thenReturn(List.of(exact));

        FulfillmentSearchInput input = new FulfillmentSearchInput();
        input.setFilterStatus(WorkflowStatus.STORED);
        input.setAccessionNumber("BIO-2026-004");
        input.setSampleType("blood");
        input.setLimit(10);

        FulfillmentSearchOutcome outcome = fulfillmentSearchService.search(input, httpRequest);

        assertFalse(outcome.getCandidates().isEmpty());
        assertEquals("BIO-2026-004", outcome.getCandidates().get(0).getBarcode());
        assertTrue(outcome.getMetadata().isExactIdentityMatchesFound());
        assertEquals(1, outcome.getMetadata().getSearchPhase());
        assertTrue(Boolean.FALSE.equals(outcome.getCandidates().get(0).getSampleTypeMatchesRequested()));
        assertEquals("TYPE_MISMATCH", outcome.getCandidates().get(0).getMismatchReason());
    }

    private BioSample storedSample(String barcode, String accession, String originLab, String projectId,
            String typeDescription) {
        Sample sample = new Sample();
        sample.setId("1");
        sample.setAccessionNumber(accession);

        TypeOfSample type = new TypeOfSample();
        type.setId("10");
        type.setDescription(typeDescription);

        SampleItem sampleItem = new SampleItem();
        sampleItem.setId("100");
        sampleItem.setExternalId(barcode);
        sampleItem.setSample(sample);
        sampleItem.setTypeOfSample(type);
        sampleItem.setQuantity(10.0);
        sampleItem.setRemainingQuantity(BigDecimal.valueOf(8));

        BioSample bioSample = new BioSample();
        bioSample.setId(1);
        bioSample.setWorkflowStatus(WorkflowStatus.STORED);
        bioSample.setOriginLab(originLab);
        bioSample.setProjectId(projectId);
        bioSample.setSampleItem(sampleItem);
        return bioSample;
    }
}
