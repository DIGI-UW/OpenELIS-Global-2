package org.openelisglobal.biorepository.controller.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.biorepository.controller.rest.dto.BioSampleListDTO;
import org.openelisglobal.biorepository.service.BioSampleFulfillmentSearchService;
import org.openelisglobal.biorepository.service.BioSampleLifecycleService;
import org.openelisglobal.biorepository.service.BioSampleService;
import org.openelisglobal.biorepository.service.FulfillmentSearchMetadata;
import org.openelisglobal.biorepository.service.FulfillmentSearchOutcome;
import org.openelisglobal.department.service.DepartmentIsolationService;
import org.openelisglobal.storage.service.SampleStorageService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@RunWith(MockitoJUnitRunner.class)
public class BioSampleSearchFulfillmentContextTest {

    @InjectMocks
    private BioSampleRestController controller;

    @Mock
    private BioSampleService bioSampleService;

    @Mock
    private BioSampleFulfillmentSearchService fulfillmentSearchService;

    @Mock
    private BioSampleLifecycleService bioSampleLifecycleService;

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
    }

    @Test
    public void searchSamples_returnsFulfillmentMetadataOnCandidates() throws Exception {
        BioSampleListDTO dto = new BioSampleListDTO();
        dto.setId(1);
        dto.setAccessionNumber("ACC-9");
        dto.setExactIdentityMatch(true);
        dto.setMatchReason("EXACT_ACCESSION");
        dto.setMatchScore(220);

        FulfillmentSearchOutcome outcome = new FulfillmentSearchOutcome();
        outcome.setCandidates(List.of(dto));
        FulfillmentSearchMetadata metadata = new FulfillmentSearchMetadata();
        metadata.setExactIdentityMatchesFound(true);
        metadata.setSearchPhase(1);
        outcome.setMetadata(metadata);

        when(fulfillmentSearchService.search(any(), any())).thenReturn(outcome);

        mockMvc.perform(get("/rest/biorepository/sample/search")
                .param("accessionNumber", "ACC-9")
                .param("status", "STORED")
                .param("context", "fulfillment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accessionNumber").value("ACC-9"))
                .andExpect(jsonPath("$[0].exactIdentityMatch").value(true))
                .andExpect(jsonPath("$[0].matchReason").value("EXACT_ACCESSION"));
    }
}
