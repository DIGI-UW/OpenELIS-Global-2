package org.openelisglobal.biorepository.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.openelisglobal.biorepository.controller.rest.dto.RetrievalItemSuggestionDTO;
import org.openelisglobal.biorepository.controller.rest.dto.RetrievalItemSuggestionsRequestDTO;

/**
 * Bulk fulfillment suggestions grouped by retrieval item ID.
 */
public interface RetrievalFulfillmentSuggestionService {

    Map<String, RetrievalItemSuggestionDTO> getSuggestions(
            RetrievalItemSuggestionsRequestDTO request, HttpServletRequest httpRequest);
}
