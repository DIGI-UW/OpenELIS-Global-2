package org.openelisglobal.biorepository.controller.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.biorepository.controller.rest.dto.BioSampleListDTO;
import org.openelisglobal.biorepository.controller.rest.dto.RetrievalItemSuggestionDTO;
import org.openelisglobal.biorepository.controller.rest.dto.RetrievalItemSuggestionSummaryDTO;
import org.openelisglobal.biorepository.service.RetrievalFulfillmentSuggestionService;
import org.openelisglobal.biorepository.service.SampleRetrievalService;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.notebook.service.NotebookEntryService;
import org.openelisglobal.storage.dao.SampleStorageAssignmentDAO;
import org.openelisglobal.storage.service.StorageLocationService;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@RunWith(MockitoJUnitRunner.class)
public class SampleRetrievalSuggestionsTest {

    @InjectMocks
    private SampleRetrievalRestController controller;

    @Mock
    private SampleRetrievalService retrievalService;

    @Mock
    private RetrievalFulfillmentSuggestionService fulfillmentSuggestionService;

    @Mock
    private NotebookEntryService notebookEntryService;

    @Mock
    private SampleStorageAssignmentDAO sampleStorageAssignmentDAO;

    @Mock
    private StorageLocationService storageLocationService;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void getItemSuggestions_returnsEntryForEveryRequestedItemId() throws Exception {
        when(fulfillmentSuggestionService.getSuggestions(any(), any())).thenReturn(new LinkedHashMap<>());

        MockHttpSession session = new MockHttpSession();
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);
        session.setAttribute(IActionConstants.USER_SESSION_DATA, usd);

        mockMvc.perform(post("/rest/biorepository/retrieval/items/suggestions")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"itemIds\":[101,202]}"))
                .andExpect(status().isOk());
    }

    @Test
    public void getItemSuggestions_returnsGroupedSuggestions() throws Exception {
        BioSampleListDTO top = new BioSampleListDTO();
        top.setId(50);
        top.setAccessionNumber("ACC-1");
        top.setExactIdentityMatch(true);
        top.setMatchReason("EXACT_ACCESSION");

        RetrievalItemSuggestionDTO suggestion = new RetrievalItemSuggestionDTO();
        suggestion.setRetrievalItemId(101);
        suggestion.setSuggestionStatus("EXACT_MATCH");
        suggestion.setExactMatchFound(true);
        suggestion.setTopCandidate(top);
        suggestion.setCandidates(List.of(top));
        RetrievalItemSuggestionSummaryDTO summary = new RetrievalItemSuggestionSummaryDTO();
        summary.setSampleIdentity("ACC-1");
        summary.setMatchReason("EXACT_ACCESSION");
        suggestion.setSummary(summary);

        Map<String, RetrievalItemSuggestionDTO> response = new LinkedHashMap<>();
        response.put("101", suggestion);

        when(fulfillmentSuggestionService.getSuggestions(any(), any())).thenReturn(response);

        MockHttpSession session = new MockHttpSession();
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);
        session.setAttribute(IActionConstants.USER_SESSION_DATA, usd);

        mockMvc.perform(post("/rest/biorepository/retrieval/items/suggestions")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"itemIds\":[101]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['101'].suggestionStatus").value("EXACT_MATCH"))
                .andExpect(jsonPath("$['101'].topCandidate.accessionNumber").value("ACC-1"))
                .andExpect(jsonPath("$['101'].summary.sampleIdentity").value("ACC-1"))
                .andExpect(jsonPath("$['101'].summary.matchReason").value("EXACT_ACCESSION"));
    }
}
