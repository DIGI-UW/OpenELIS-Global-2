package org.openelisglobal.biorepository.controller.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.biorepository.dao.BioSampleRetrievalSearchCriteria;
import org.openelisglobal.biorepository.service.BioSampleService;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BioSample.WorkflowStatus;
import org.openelisglobal.department.service.DepartmentIsolationService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.service.SampleStorageService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@RunWith(MockitoJUnitRunner.class)
public class BioSampleSearchDiscoveryTest {

    @InjectMocks
    private BioSampleRestController controller;

    @Mock
    private BioSampleService bioSampleService;

    @Mock
    private TypeOfSampleService typeOfSampleService;

    @Mock
    private SampleStorageService sampleStorageService;

    @Mock
    private DepartmentIsolationService departmentIsolationService;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        when(departmentIsolationService.hasUnrestrictedDepartmentAccess(any())).thenReturn(true);
        when(sampleStorageService.getSampleItemLocation(any())).thenReturn(java.util.Map.of());
    }

    @Test
    public void searchSamples_returnsMatches_whenSampleTypeOnlyProvided() throws Exception {
        BioSample bioSample = storedSample("BIO-PLASMA-1", "ACC-100", "CTD", "PROJ-A", "Plasma");

        when(typeOfSampleService.getAllTypeOfSamples()).thenReturn(List.of(plasmaType()));
        when(bioSampleService.searchForRetrieval(any())).thenReturn(List.of(bioSample));

        mockMvc.perform(get("/rest/biorepository/sample/search")
                .param("sampleType", "Plasma")
                .param("status", "STORED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].barcode").value("BIO-PLASMA-1"))
                .andExpect(jsonPath("$[0].originLab").value("CTD"));
    }

    @Test
    public void searchSamples_returnsMatches_whenOriginLabOnlyProvided() throws Exception {
        BioSample bioSample = storedSample("BIO-CTD-1", "ACC-200", "CTD Laboratory", "PROJ-B", "Serum");

        when(bioSampleService.searchForRetrieval(any())).thenReturn(List.of(bioSample));

        mockMvc.perform(get("/rest/biorepository/sample/search")
                .param("originLab", "CTD")
                .param("status", "STORED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].originLab").value("CTD Laboratory"));
    }

    @Test
    public void searchSamples_returnsMatches_whenProjectOnlyProvided() throws Exception {
        BioSample bioSample = storedSample("BIO-PROJ-1", "ACC-300", "Virology", "COVID-2026", "Swab");

        when(bioSampleService.searchForRetrieval(any())).thenReturn(List.of(bioSample));

        mockMvc.perform(get("/rest/biorepository/sample/search")
                .param("projectId", "COVID")
                .param("status", "STORED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].projectId").value("COVID-2026"));
    }

    @Test
    public void searchSamples_returnsMatches_whenBrowseRequested() throws Exception {
        BioSample bioSample = storedSample("BIO-BROWSE-1", "ACC-400", "CTD", "PROJ-C", "Plasma");

        when(bioSampleService.searchForRetrieval(any())).thenReturn(List.of(bioSample));

        mockMvc.perform(get("/rest/biorepository/sample/search").param("browse", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].barcode").value("BIO-BROWSE-1"));

        ArgumentCaptor<BioSampleRetrievalSearchCriteria> captor =
                ArgumentCaptor.forClass(BioSampleRetrievalSearchCriteria.class);
        verify(bioSampleService).searchForRetrieval(captor.capture());
        org.junit.Assert.assertEquals(WorkflowStatus.STORED, captor.getValue().getWorkflowStatus());
    }

    @Test
    public void searchSamples_returnsEmpty_whenNoSearchParams() throws Exception {
        mockMvc.perform(get("/rest/biorepository/sample/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    public void searchSamples_passesCombinedFiltersToService() throws Exception {
        when(typeOfSampleService.getAllTypeOfSamples()).thenReturn(List.of(plasmaType()));
        when(bioSampleService.searchForRetrieval(any())).thenReturn(List.of());

        mockMvc.perform(get("/rest/biorepository/sample/search")
                .param("sampleType", "Plasma")
                .param("originLab", "CTD")
                .param("projectId", "COVID")
                .param("barcode", "BIO")
                .param("accessionNumber", "ACC")
                .param("status", "STORED"))
                .andExpect(status().isOk());

        ArgumentCaptor<BioSampleRetrievalSearchCriteria> captor =
                ArgumentCaptor.forClass(BioSampleRetrievalSearchCriteria.class);
        verify(bioSampleService).searchForRetrieval(captor.capture());
        BioSampleRetrievalSearchCriteria criteria = captor.getValue();
        org.junit.Assert.assertEquals("%BIO%", criteria.getBarcodePattern());
        org.junit.Assert.assertEquals("%ACC%", criteria.getAccessionPattern());
        org.junit.Assert.assertEquals("%CTD%", criteria.getOriginLabPattern());
        org.junit.Assert.assertEquals("%COVID%", criteria.getProjectIdPattern());
        org.junit.Assert.assertNotNull(criteria.getSampleTypeIds());
    }

    private TypeOfSample plasmaType() {
        TypeOfSample plasma = new TypeOfSample();
        plasma.setId("10");
        plasma.setDescription("Plasma");
        return plasma;
    }

    private BioSample storedSample(String barcode, String accession, String originLab, String projectId,
            String typeDescription) {
        TypeOfSample type = new TypeOfSample();
        type.setId("10");
        type.setDescription(typeDescription);

        Sample sample = new Sample();
        sample.setId("100");
        sample.setAccessionNumber(accession);

        SampleItem sampleItem = new SampleItem();
        sampleItem.setId("501");
        sampleItem.setExternalId(barcode);
        sampleItem.setTypeOfSample(type);
        sampleItem.setSample(sample);
        sampleItem.setQuantity(5.0);
        sampleItem.setRemainingQuantity(BigDecimal.valueOf(5));

        BioSample bioSample = new BioSample();
        bioSample.setId(99);
        bioSample.setWorkflowStatus(WorkflowStatus.STORED);
        bioSample.setSampleItem(sampleItem);
        bioSample.setOriginLab(originLab);
        bioSample.setProjectId(projectId);
        return bioSample;
    }
}
