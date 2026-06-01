package org.openelisglobal.biorepository.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.biorepository.controller.rest.dto.BioSampleListDTO;
import org.openelisglobal.biorepository.controller.rest.dto.RetrievalItemSuggestionDTO;
import org.openelisglobal.biorepository.controller.rest.dto.RetrievalItemSuggestionsRequestDTO;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalItem;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;

@RunWith(MockitoJUnitRunner.class)
public class RetrievalFulfillmentSuggestionServiceTest {

    @InjectMocks
    private RetrievalFulfillmentSuggestionServiceImpl suggestionService;

    @Mock
    private SampleRetrievalService retrievalService;

    @Mock
    private BioSampleFulfillmentSearchService fulfillmentSearchService;

    @Mock
    private TypeOfSampleService typeOfSampleService;

    @Mock
    private HttpServletRequest httpRequest;

    @Test
    public void getSuggestions_returnsEntryForMissingItem() {
        RetrievalItemSuggestionsRequestDTO request = new RetrievalItemSuggestionsRequestDTO();
        request.setItemIds(Arrays.asList(101, 202));

        when(retrievalService.getRetrievalItem(101)).thenReturn(null);

        SampleRetrievalItem reference = new SampleRetrievalItem();
        reference.setId(202);

        when(retrievalService.getRetrievalItem(202)).thenReturn(reference);

        Map<String, RetrievalItemSuggestionDTO> result = suggestionService.getSuggestions(request, httpRequest);

        assertEquals(2, result.size());
        assertNotNull(result.get("101"));
        assertEquals("NO_CANDIDATE", result.get("101").getSuggestionStatus());
        assertNotNull(result.get("202"));
        assertEquals("NO_CRITERIA", result.get("202").getSuggestionStatus());
    }

    @Test
    public void getSuggestions_searchesWhenSampleTypeTextDoesNotResolveToIds() {
        when(typeOfSampleService.getAllTypeOfSamples()).thenReturn(java.util.Collections.emptyList());

        SampleRetrievalItem item = new SampleRetrievalItem();
        item.setId(303);
        item.setRequestedSampleType("blood");
        when(retrievalService.getRetrievalItem(303)).thenReturn(item);

        BioSampleListDTO top = new BioSampleListDTO();
        top.setId(55);
        top.setAccessionNumber("ACC-55");
        top.setExactIdentityMatch(false);

        FulfillmentSearchOutcome outcome = new FulfillmentSearchOutcome();
        outcome.setCandidates(java.util.List.of(top));
        outcome.setMetadata(new FulfillmentSearchMetadata());
        when(fulfillmentSearchService.search(any(), any())).thenReturn(outcome);

        RetrievalItemSuggestionsRequestDTO request = new RetrievalItemSuggestionsRequestDTO();
        request.setItemIds(java.util.Arrays.asList(303));

        Map<String, RetrievalItemSuggestionDTO> result = suggestionService.getSuggestions(request, httpRequest);

        verify(fulfillmentSearchService).search(any(), any());
        assertEquals("REVIEW_SUGGESTIONS", result.get("303").getSuggestionStatus());
    }

    @Test
    public void getSuggestions_isolatesPerItemFailures() {
        RetrievalItemSuggestionsRequestDTO request = new RetrievalItemSuggestionsRequestDTO();
        request.setItemIds(Arrays.asList(404, 505));

        SampleRetrievalItem failingItem = new SampleRetrievalItem();
        failingItem.setId(404);
        failingItem.setRequestedSampleType("blood");
        when(retrievalService.getRetrievalItem(404)).thenReturn(failingItem);
        doThrow(new RuntimeException("search exploded"))
                .when(fulfillmentSearchService)
                .search(any(), any());

        SampleRetrievalItem okItem = new SampleRetrievalItem();
        okItem.setId(505);
        when(retrievalService.getRetrievalItem(505)).thenReturn(okItem);

        Map<String, RetrievalItemSuggestionDTO> result = suggestionService.getSuggestions(request, httpRequest);

        assertEquals("NO_CANDIDATE", result.get("404").getSuggestionStatus());
        assertEquals("NO_CRITERIA", result.get("505").getSuggestionStatus());
    }
}
