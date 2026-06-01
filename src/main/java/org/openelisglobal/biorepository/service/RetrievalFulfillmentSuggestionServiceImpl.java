package org.openelisglobal.biorepository.service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.openelisglobal.biorepository.controller.rest.dto.BioSampleListDTO;
import org.openelisglobal.biorepository.controller.rest.dto.RetrievalItemIdentityLookupDTO;
import org.openelisglobal.biorepository.controller.rest.dto.RetrievalItemSuggestionDTO;
import org.openelisglobal.biorepository.controller.rest.dto.RetrievalItemSuggestionSummaryDTO;
import org.openelisglobal.biorepository.controller.rest.dto.RetrievalItemSuggestionsRequestDTO;
import org.openelisglobal.biorepository.valueholder.BioSample.WorkflowStatus;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalItem;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RetrievalFulfillmentSuggestionServiceImpl implements RetrievalFulfillmentSuggestionService {

    private static final Logger log =
            LoggerFactory.getLogger(RetrievalFulfillmentSuggestionServiceImpl.class);

    private static final int CANDIDATE_CAP = 10;

    @Autowired private SampleRetrievalService retrievalService;

    @Autowired private BioSampleFulfillmentSearchService fulfillmentSearchService;

    @Autowired private TypeOfSampleService typeOfSampleService;

    @Override
    public Map<String, RetrievalItemSuggestionDTO> getSuggestions(
            RetrievalItemSuggestionsRequestDTO request, HttpServletRequest httpRequest) {
        Map<String, RetrievalItemSuggestionDTO> response = new LinkedHashMap<>();
        if (request == null || request.getItemIds() == null || request.getItemIds().isEmpty()) {
            return response;
        }

        Map<Integer, RetrievalItemIdentityLookupDTO> identityOverrides = new HashMap<>();
        if (request.getIdentityLookups() != null) {
            for (RetrievalItemIdentityLookupDTO lookup : request.getIdentityLookups()) {
                if (lookup != null && lookup.getRetrievalItemId() != null) {
                    identityOverrides.put(lookup.getRetrievalItemId(), lookup);
                }
            }
        }

        for (Integer itemId : request.getItemIds()) {
            if (itemId == null) {
                continue;
            }
            try {
                SampleRetrievalItem item = retrievalService.getRetrievalItem(itemId);
                RetrievalItemIdentityLookupDTO override = identityOverrides.get(itemId);
                if (item == null) {
                    response.put(String.valueOf(itemId), buildUnavailableSuggestion(itemId));
                    continue;
                }
                if (!item.isReferenceLine()) {
                    response.put(String.valueOf(itemId), buildUnavailableSuggestion(itemId));
                    continue;
                }
                response.put(String.valueOf(itemId), buildSuggestionForItem(item, override, httpRequest));
            } catch (RuntimeException ex) {
                log.warn("Fulfillment suggestion failed for retrieval item {}", itemId, ex);
                response.put(String.valueOf(itemId), buildUnavailableSuggestion(itemId));
            }
        }
        return response;
    }

    private RetrievalItemSuggestionDTO buildUnavailableSuggestion(Integer itemId) {
        RetrievalItemSuggestionDTO dto = new RetrievalItemSuggestionDTO();
        dto.setRetrievalItemId(itemId);
        dto.setSuggestionStatus("NO_CANDIDATE");
        dto.setExactMatchFound(false);
        dto.setSummary(new RetrievalItemSuggestionSummaryDTO());
        return dto;
    }

    private RetrievalItemSuggestionDTO buildSuggestionForItem(
            SampleRetrievalItem item,
            RetrievalItemIdentityLookupDTO override,
            HttpServletRequest httpRequest) {
        RetrievalItemSuggestionDTO dto = new RetrievalItemSuggestionDTO();
        dto.setRetrievalItemId(item.getId());

        String accession =
            firstNonBlank(
                override != null ? override.getAccessionNumber() : null, item.getRequestedAccessionNumber());
        String barcode =
            firstNonBlank(override != null ? override.getBarcode() : null, item.getRequestedBarcode());
        String sampleType = trimToNull(item.getRequestedSampleType());
        String originLab = trimToNull(item.getRequestedOriginLab());
        String projectId = trimToNull(item.getRequestedProjectId());

        if (!hasSearchCriteria(accession, barcode, sampleType, originLab, projectId)) {
            dto.setSuggestionStatus("NO_CRITERIA");
            dto.setSummary(new RetrievalItemSuggestionSummaryDTO());
            return dto;
        }

        Set<String> matchingTypeIds = resolveSampleTypeIds(sampleType);

        FulfillmentSearchInput input = new FulfillmentSearchInput();
        input.setFilterStatus(WorkflowStatus.STORED);
        input.setMatchingTypeIds(matchingTypeIds.isEmpty() ? null : matchingTypeIds);
        input.setIdentity(deriveFulfillmentIdentity(accession, barcode));
        input.setBarcode(barcode);
        input.setAccessionNumber(accession);
        input.setSampleType(sampleType);
        input.setOriginLab(originLab);
        input.setProjectId(projectId);
        SampleRetrievalRequest parentRequest = item.getRetrievalRequest();
        if (parentRequest != null) {
            input.setRequesterLabUnit(trimToNull(parentRequest.getRequesterLabUnit()));
        }
        if (item.getRequestedCollectionDateFrom() != null) {
            input.setCollectionDateFrom(item.getRequestedCollectionDateFrom().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
        if (item.getRequestedCollectionDateTo() != null) {
            input.setCollectionDateTo(item.getRequestedCollectionDateTo().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
        input.setLimit(CANDIDATE_CAP);

        FulfillmentSearchOutcome outcome = fulfillmentSearchService.search(input, httpRequest);
        List<BioSampleListDTO> candidates = outcome.getCandidates();
        FulfillmentSearchMetadata metadata = outcome.getMetadata();

        dto.setFallbackUsed(metadata != null && metadata.isFallbackUsed());
        dto.setNoExactMatch(metadata != null && metadata.isNoExactMatch());
        dto.setCandidates(candidates);

        if (candidates.isEmpty()) {
            dto.setSuggestionStatus("NO_CANDIDATE");
            dto.setExactMatchFound(false);
            dto.setSummary(new RetrievalItemSuggestionSummaryDTO());
            return dto;
        }

        List<BioSampleListDTO> exactMatches =
            candidates.stream()
                .filter(c -> Boolean.TRUE.equals(c.getExactIdentityMatch()))
                .collect(Collectors.toList());

        if (!exactMatches.isEmpty()) {
            BioSampleListDTO topExact = exactMatches.get(0);
            boolean sampleTypeMatches = !Boolean.FALSE.equals(topExact.getSampleTypeMatchesRequested());
            dto.setSuggestionStatus(sampleTypeMatches ? "EXACT_MATCH" : "EXACT_MATCH_TYPE_MISMATCH");
            dto.setExactMatchFound(true);
            dto.setSampleTypeMatchesRequested(sampleTypeMatches);
            dto.setMismatchReason(topExact.getMismatchReason());
            dto.setTopCandidate(topExact);
            dto.setSummary(buildSummary(topExact));
            return dto;
        }

        dto.setSuggestionStatus("REVIEW_SUGGESTIONS");
        dto.setExactMatchFound(false);
        dto.setTopCandidate(candidates.get(0));
        dto.setSummary(buildSummary(candidates.get(0)));
        return dto;
    }

    private RetrievalItemSuggestionSummaryDTO buildSummary(BioSampleListDTO topCandidate) {
        RetrievalItemSuggestionSummaryDTO summary = new RetrievalItemSuggestionSummaryDTO();
        if (topCandidate == null) {
            return summary;
        }
        summary.setSampleIdentity(firstNonBlank(
                topCandidate.getAccessionNumber(),
                topCandidate.getBarcode(),
                topCandidate.getProjectId()));
        summary.setAvailableQuantity(
                topCandidate.getRemainingQuantity() != null
                        ? topCandidate.getRemainingQuantity()
                        : topCandidate.getQuantity());
        summary.setAvailableUnitOfMeasure(topCandidate.getUnitOfMeasure());
        summary.setSamplePath(firstNonBlank(topCandidate.getSamplePath(), topCandidate.getHierarchicalPath()));
        summary.setMatchReason(topCandidate.getMatchReason());
        summary.setMatchScore(topCandidate.getMatchScore());
        summary.setSampleTypeMatchesRequested(topCandidate.getSampleTypeMatchesRequested());
        summary.setMismatchReason(topCandidate.getMismatchReason());
        return summary;
    }

    private boolean hasSearchCriteria(
            String accession, String barcode, String sampleType, String originLab, String projectId) {
        return accession != null
            || barcode != null
            || sampleType != null
            || originLab != null
            || projectId != null;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private String deriveFulfillmentIdentity(String accession, String barcode) {
        if (accession != null && barcode != null) {
            return accession.equalsIgnoreCase(barcode) ? accession : null;
        }
        return accession != null ? accession : barcode;
    }

    private Set<String> resolveSampleTypeIds(String sampleTypeQuery) {
        if (sampleTypeQuery == null || sampleTypeQuery.trim().isEmpty()) {
            return Set.of();
        }
        String trimmed = sampleTypeQuery.trim();
        String normalizedQuery = trimmed.toLowerCase(Locale.ROOT);
        Set<String> matchingIds = new HashSet<>();

        for (TypeOfSample type : typeOfSampleService.getAllTypeOfSamples()) {
            if (type.getId() == null) {
                continue;
            }
            String description =
                type.getDescription() != null ? type.getDescription().toLowerCase(Locale.ROOT) : "";
            String localizedName =
                type.getLocalizedName() != null ? type.getLocalizedName().toLowerCase(Locale.ROOT) : "";
            if (description.contains(normalizedQuery)
                || localizedName.contains(normalizedQuery)
                || trimmed.equalsIgnoreCase(type.getId())) {
                matchingIds.add(type.getId());
            }
        }
        return matchingIds;
    }
}
