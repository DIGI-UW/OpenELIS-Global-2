package org.openelisglobal.biorepository.controller.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
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
public class BioSampleSearchSampleTypeTest {

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
    public void searchSamples_returnsMatches_whenSampleTypeProvided() throws Exception {
        TypeOfSample plasma = new TypeOfSample();
        plasma.setId("10");
        plasma.setDescription("Plasma");

        Sample sample = new Sample();
        sample.setId("100");
        sample.setAccessionNumber("ACC-001");

        SampleItem sampleItem = new SampleItem();
        sampleItem.setId("501");
        sampleItem.setExternalId("BIO-PLASMA-1");
        sampleItem.setTypeOfSample(plasma);
        sampleItem.setSample(sample);
        sampleItem.setQuantity(3.0);
        sampleItem.setRemainingQuantity(BigDecimal.valueOf(3));

        BioSample bioSample = new BioSample();
        bioSample.setId(99);
        bioSample.setWorkflowStatus(WorkflowStatus.STORED);
        bioSample.setSampleItem(sampleItem);
        bioSample.setOriginLab("CTD");

        when(typeOfSampleService.getAllTypeOfSamples()).thenReturn(List.of(plasma));
        when(bioSampleService.searchForRetrieval(any())).thenReturn(List.of(bioSample));

        mockMvc.perform(get("/rest/biorepository/sample/search")
                .param("sampleType", "Plasma")
                .param("status", "STORED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].barcode").value("BIO-PLASMA-1"))
                .andExpect(jsonPath("$[0].originLab").value("CTD"));
    }

    @Test
    public void searchSamples_returnsEmpty_whenNoSearchParams() throws Exception {
        mockMvc.perform(get("/rest/biorepository/sample/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
