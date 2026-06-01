package org.openelisglobal.biorepository.service;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.openelisglobal.biorepository.controller.rest.dto.BioSampleListDTO;
import org.openelisglobal.biorepository.dao.BioSampleRetrievalSearchCriteria;
import org.openelisglobal.biorepository.util.Brf02SamplePathFormatter;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BioSample.WorkflowStatus;
import org.openelisglobal.biorepository.valueholder.RetentionPolicy;
import org.openelisglobal.department.service.DepartmentIsolationService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.service.SampleStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Phased exact-first fulfillment search for stored biorepository samples.
 */
@Service
public class BioSampleFulfillmentSearchServiceImpl implements BioSampleFulfillmentSearchService {

  private static final int PHASE_FETCH_MULTIPLIER = 4;
  private static final int PHASE_FETCH_MIN = 200;
  private static final int STORAGE_BROWSE_CAP = 10;

  @Autowired private BioSampleService bioSampleService;

  @Autowired private DepartmentIsolationService departmentIsolationService;

  @Autowired private SampleStorageService sampleStorageService;

  @Autowired private RetentionPolicyService retentionPolicyService;

  @Override
  public FulfillmentSearchOutcome search(FulfillmentSearchInput input, HttpServletRequest request) {
    FulfillmentSearchOutcome outcome = new FulfillmentSearchOutcome();
    FulfillmentSearchMetadata metadata = outcome.getMetadata();

    String barcode = trimToNull(input.getBarcode());
    String accessionNumber = trimToNull(input.getAccessionNumber());
    String identity = deriveExactIdentityInput(input.getIdentity(), accessionNumber, barcode);
    String originLab = trimToNull(input.getOriginLab());
    String projectId = trimToNull(input.getProjectId());
    boolean hasAccession = accessionNumber != null;
    boolean hasBarcode = barcode != null;
    boolean hasExactIdentityInput = identity != null || hasAccession || hasBarcode;
    metadata.setHasExactIdentityInput(hasExactIdentityInput);

    int limit = input.getLimit() > 0 ? input.getLimit() : 50;
    int fetchLimit = Math.max(limit * PHASE_FETCH_MULTIPLIER, PHASE_FETCH_MIN);
    WorkflowStatus filterStatus = input.getFilterStatus();
    Set<String> matchingTypeIds = input.getMatchingTypeIds();

    // Phase 0: storage inventory bridge (SampleItem + assignment without BioSample row)
    if (hasExactIdentityInput) {
      List<BioSampleListDTO> phase0 = searchStoredInventoryByIdentity(
          identity, accessionNumber, barcode, filterStatus, request);
      if (!phase0.isEmpty()) {
        metadata.setExactIdentityMatchesFound(true);
        metadata.setSearchPhase(0);
        outcome.setCandidates(
            applyScoringAndSort(
                phase0, identity, barcode, accessionNumber, input.getSampleType(), originLab, projectId, metadata));
        return trimToLimit(outcome, limit);
      }
    }

    // Phase 1: exact smart identity (accession OR sample ID/barcode)
    if (identity != null) {
      List<BioSampleListDTO> phase1 =
          filterExactIdentity(
              runCriteriaSearch(
                  filterStatus,
                  null,
                  null,
                  identity,
                  null,
                  null,
                  null,
                  null,
                  input.getCollectionDateFrom(),
                  input.getCollectionDateTo(),
                  fetchLimit,
                  request),
              identity);
      if (!phase1.isEmpty()) {
        metadata.setExactIdentityMatchesFound(true);
        metadata.setSearchPhase(1);
        outcome.setCandidates(
        applyScoringAndSort(
            phase1, identity, barcode, accessionNumber, input.getSampleType(), originLab, projectId, metadata));
        return trimToLimit(outcome, limit);
      }
    }

    // Phase 2: exact accession when explicitly distinct
    if (identity == null && hasAccession) {
      List<BioSampleListDTO> phase2 =
          filterExactAccession(
              runCriteriaSearch(
                  filterStatus,
                  null,
                  null,
                  null,
                  null,
                  accessionNumber,
                  null,
                  null,
                  input.getCollectionDateFrom(),
                  input.getCollectionDateTo(),
                  fetchLimit,
                  request),
              accessionNumber);
      if (!phase2.isEmpty()) {
        metadata.setExactIdentityMatchesFound(true);
        metadata.setSearchPhase(2);
        outcome.setCandidates(
            applyScoringAndSort(
                phase2, identity, barcode, accessionNumber, input.getSampleType(), originLab, projectId, metadata));
        return trimToLimit(outcome, limit);
      }
    }

    // Phase 3: exact barcode when explicitly distinct
    if (identity == null && hasBarcode) {
      List<BioSampleListDTO> phase3 =
          filterExactBarcode(
              runCriteriaSearch(
                  filterStatus,
                  null,
                  null,
                  null,
                  barcode,
                  null,
                  null,
                  null,
                  input.getCollectionDateFrom(),
                  input.getCollectionDateTo(),
                  fetchLimit,
                  request),
              barcode);
      if (!phase3.isEmpty()) {
        metadata.setExactIdentityMatchesFound(true);
        metadata.setSearchPhase(3);
        outcome.setCandidates(
            applyScoringAndSort(
                phase3, identity, barcode, accessionNumber, input.getSampleType(), originLab, projectId, metadata));
        return trimToLimit(outcome, limit);
      }
    }

    // Phase 4: partial identity (accession/barcode LIKE or smart identity)
    if (hasExactIdentityInput) {
      List<BioSampleListDTO> phase4 =
          dedupeById(
              runCriteriaSearch(
                  filterStatus,
                  null,
                  null,
                  identity,
                  barcode,
                  accessionNumber,
                  null,
                  null,
                  input.getCollectionDateFrom(),
                  input.getCollectionDateTo(),
                  fetchLimit,
                  request));
      if (!phase4.isEmpty()) {
        metadata.setFallbackUsed(true);
        metadata.setNoExactMatch(true);
        metadata.setSearchPhase(4);
        outcome.setCandidates(
            applyScoringAndSort(
                phase4, identity, barcode, accessionNumber, input.getSampleType(), originLab, projectId, metadata));
        return trimToLimit(outcome, limit);
      }
    }

    // Phase 5: sample type + origin (IDs and/or free-text type description)
    String sampleTypeDescriptionPattern = resolveSampleTypeDescriptionPattern(input.getSampleType(), matchingTypeIds);
    if (hasTypeOrOriginCriteria(originLab, matchingTypeIds, sampleTypeDescriptionPattern)) {
      List<BioSampleListDTO> phase5 =
          dedupeById(
              runCriteriaSearch(
                  filterStatus,
                  matchingTypeIds,
                  sampleTypeDescriptionPattern,
                  null,
                  null,
                  null,
                  originLab,
                  null,
                  input.getCollectionDateFrom(),
                  input.getCollectionDateTo(),
                  limit,
                  request));
      if (!phase5.isEmpty()) {
        metadata.setNoExactMatch(hasExactIdentityInput);
        metadata.setSearchPhase(5);
        outcome.setCandidates(
            applyScoringAndSort(
                phase5, identity, barcode, accessionNumber, input.getSampleType(), originLab, projectId, metadata));
        return trimToLimit(outcome, limit);
      }
    }

    // Phase 5b: browse physically stored SampleItems when type-only criteria (e.g. "blood")
    if (!hasExactIdentityInput && trimToNull(input.getSampleType()) != null) {
      List<BioSampleListDTO> storageBrowse =
          browseStoredAssignments(input, filterStatus, limit, request);
      if (!storageBrowse.isEmpty()) {
        metadata.setNoExactMatch(true);
        metadata.setSearchPhase(7);
        outcome.setCandidates(
            applyScoringAndSort(
                storageBrowse,
                identity,
                barcode,
                accessionNumber,
                input.getSampleType(),
                originLab,
                projectId,
                metadata));
        return trimToLimit(outcome, limit);
      }
    }

    // Phase 6: project only
    if (projectId != null) {
      List<BioSampleListDTO> phase6 =
          dedupeById(
              runCriteriaSearch(
                  filterStatus,
                  matchingTypeIds,
                  null,
                  null,
                  null,
                  null,
                  null,
                  projectId,
                  input.getCollectionDateFrom(),
                  input.getCollectionDateTo(),
                  limit,
                  request));
      if (!phase6.isEmpty()) {
        metadata.setNoExactMatch(hasExactIdentityInput);
        metadata.setSearchPhase(6);
        outcome.setCandidates(
            applyScoringAndSort(
                phase6, identity, barcode, accessionNumber, input.getSampleType(), originLab, projectId, metadata));
        return trimToLimit(outcome, limit);
      }
    }

    metadata.setNoExactMatch(hasExactIdentityInput);
    metadata.setSearchPhase(0);
    outcome.setCandidates(new ArrayList<>());
    return outcome;
  }

  private FulfillmentSearchOutcome trimToLimit(FulfillmentSearchOutcome outcome, int limit) {
    List<BioSampleListDTO> candidates = outcome.getCandidates();
    if (candidates.size() > limit) {
      outcome.setCandidates(new ArrayList<>(candidates.subList(0, limit)));
    }
    return outcome;
  }

  private boolean hasTypeOrOriginCriteria(
      String originLab, Set<String> matchingTypeIds, String sampleTypeDescriptionPattern) {
    return originLab != null
        || (matchingTypeIds != null && !matchingTypeIds.isEmpty())
        || sampleTypeDescriptionPattern != null;
  }

  private String resolveSampleTypeDescriptionPattern(String sampleType, Set<String> matchingTypeIds) {
    String trimmed = trimToNull(sampleType);
    if (trimmed == null) {
      return null;
    }
    if (matchingTypeIds != null && !matchingTypeIds.isEmpty()) {
      return null;
    }
    return wrapLikePattern(trimmed);
  }

  private List<BioSampleListDTO> runCriteriaSearch(
      WorkflowStatus filterStatus,
      Set<String> matchingTypeIds,
      String sampleTypeDescriptionPattern,
      String identity,
      String barcode,
      String accessionNumber,
      String originLab,
      String projectId,
      String collectionDateFrom,
      String collectionDateTo,
      int limit,
      HttpServletRequest request) {
    BioSampleRetrievalSearchCriteria criteria = new BioSampleRetrievalSearchCriteria();
    criteria.setWorkflowStatus(filterStatus);
    criteria.setIdentityPattern(wrapLikePattern(identity));
    criteria.setBarcodePattern(wrapLikePattern(barcode));
    criteria.setAccessionPattern(wrapLikePattern(accessionNumber));
    criteria.setSampleTypeIds(matchingTypeIds);
    criteria.setSampleTypeDescriptionPattern(sampleTypeDescriptionPattern);
    criteria.setOriginLabPattern(wrapLikePattern(originLab));
    criteria.setProjectIdPattern(wrapLikePattern(projectId));
    criteria.setCollectionDateFrom(parseCollectionDateStart(collectionDateFrom));
    criteria.setCollectionDateTo(parseCollectionDateEnd(collectionDateTo));
    criteria.setLimit(limit);

    List<BioSample> bioSamples = bioSampleService.searchForRetrieval(criteria);
    List<BioSampleListDTO> result = new ArrayList<>();
    for (BioSample bioSample : bioSamples) {
      if (bioSample.getSampleItem() == null) {
        continue;
      }
      BioSampleListDTO dto = buildSearchResultDTO(bioSample, filterStatus);
      if (dto != null) {
        result.add(dto);
      }
    }
    if (!departmentIsolationService.hasUnrestrictedDepartmentAccess(request)) {
      result =
          result.stream()
              .filter(
                  d ->
                      d.getSampleItemId() != null
                          && departmentIsolationService.canAccessSampleItemIdentifier(
                              String.valueOf(d.getSampleItemId()), request))
              .collect(Collectors.toList());
    }
    return result;
  }

  private List<BioSampleListDTO> filterExactAccession(List<BioSampleListDTO> candidates, String accessionNumber) {
    return candidates.stream()
        .filter(
            dto ->
                dto.getAccessionNumber() != null
                    && dto.getAccessionNumber().equalsIgnoreCase(accessionNumber.trim()))
        .collect(Collectors.toCollection(ArrayList::new));
  }

  private List<BioSampleListDTO> filterExactIdentity(List<BioSampleListDTO> candidates, String identity) {
    String normalized = identity != null ? identity.trim() : null;
    if (normalized == null || normalized.isEmpty()) {
      return List.of();
    }
    return candidates.stream()
        .filter(
            dto ->
                (dto.getAccessionNumber() != null
                        && dto.getAccessionNumber().equalsIgnoreCase(normalized))
                    || (dto.getBarcode() != null && dto.getBarcode().equalsIgnoreCase(normalized)))
        .collect(Collectors.toCollection(ArrayList::new));
  }

  private List<BioSampleListDTO> filterExactBarcode(List<BioSampleListDTO> candidates, String barcode) {
    return candidates.stream()
        .filter(
            dto ->
                dto.getBarcode() != null && dto.getBarcode().equalsIgnoreCase(barcode.trim()))
        .collect(Collectors.toCollection(ArrayList::new));
  }

  private List<BioSampleListDTO> dedupeById(List<BioSampleListDTO> items) {
    Map<Integer, BioSampleListDTO> deduped = new LinkedHashMap<>();
    for (BioSampleListDTO item : items) {
      if (item != null && item.getId() != null) {
        deduped.putIfAbsent(item.getId(), item);
      }
    }
    return new ArrayList<>(deduped.values());
  }

  private List<BioSampleListDTO> applyScoringAndSort(
      List<BioSampleListDTO> results,
      String identity,
      String barcode,
      String accessionNumber,
      String sampleType,
      String originLab,
      String projectId,
      FulfillmentSearchMetadata metadata) {
    String identityTerm = identity != null ? identity.trim().toLowerCase(Locale.ROOT) : null;
    String barcodeTerm = barcode != null ? barcode.trim().toLowerCase(Locale.ROOT) : null;
    String accessionTerm = accessionNumber != null ? accessionNumber.trim().toLowerCase(Locale.ROOT) : null;
    String sampleTypeTerm = sampleType != null ? sampleType.trim().toLowerCase(Locale.ROOT) : null;
    String originLabTerm = originLab != null ? originLab.trim().toLowerCase(Locale.ROOT) : null;
    String projectIdTerm = projectId != null ? projectId.trim().toLowerCase(Locale.ROOT) : null;

    results.forEach(
        dto -> {
          int score =
              fulfillmentMatchScore(
                  dto, identityTerm, barcodeTerm, accessionTerm, sampleTypeTerm, originLabTerm, projectIdTerm);
          String matchReason =
              determineMatchReason(
                  dto, identityTerm, barcodeTerm, accessionTerm, sampleTypeTerm, originLabTerm, projectIdTerm);
          boolean exactIdentityMatch = isExactIdentityMatchReason(matchReason);
          Boolean sampleTypeMatchesRequested =
              determineSampleTypeMatch(dto, sampleTypeTerm, exactIdentityMatch);
          String mismatchReason =
              determineMismatchReason(sampleTypeMatchesRequested, exactIdentityMatch);
          dto.setMatchReason(matchReason);
          dto.setMatchScore(score);
          dto.setExactIdentityMatch(exactIdentityMatch);
          dto.setSampleTypeMatchesRequested(sampleTypeMatchesRequested);
          dto.setMismatchReason(mismatchReason);
          dto.setFallbackUsed(
              Boolean.valueOf(
                  metadata.isHasExactIdentityInput()
                      && !exactIdentityMatch
                      && !metadata.isExactIdentityMatchesFound()
                      && metadata.isFallbackUsed()));
        });

    results.sort(
        Comparator.comparingInt((BioSampleListDTO dto) -> dto.getMatchScore() != null ? dto.getMatchScore() : 0)
            .reversed()
            .thenComparing(
                BioSampleListDTO::getCollectionDate, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(BioSampleListDTO::getId, Comparator.nullsLast(Comparator.reverseOrder())));
    return results;
  }

  private int fulfillmentMatchScore(
      BioSampleListDTO dto,
      String identityTerm,
      String barcodeTerm,
      String accessionTerm,
      String sampleTypeTerm,
      String originLabTerm,
      String projectIdTerm) {
    int score = 0;
    if (identityTerm != null) {
      if (resolveExactIdentityMatchReason(dto, identityTerm) != null) {
        score += 240;
      } else if ((dto.getBarcode() != null
                  && dto.getBarcode().toLowerCase(Locale.ROOT).contains(identityTerm))
              || (dto.getAccessionNumber() != null
                  && dto.getAccessionNumber().toLowerCase(Locale.ROOT).contains(identityTerm))) {
        score += 150;
      }
    } else {
      if (barcodeTerm != null
          && dto.getBarcode() != null
          && dto.getBarcode().equalsIgnoreCase(barcodeTerm)) {
        score += 200;
      } else if (barcodeTerm != null
          && dto.getBarcode() != null
          && dto.getBarcode().toLowerCase(Locale.ROOT).contains(barcodeTerm)) {
        score += 120;
      }
      if (accessionTerm != null
          && dto.getAccessionNumber() != null
          && dto.getAccessionNumber().equalsIgnoreCase(accessionTerm)) {
        score += 220;
      } else if (accessionTerm != null
          && dto.getAccessionNumber() != null
          && dto.getAccessionNumber().toLowerCase(Locale.ROOT).contains(accessionTerm)) {
        score += 140;
      }
    }
    if (matchesTerm(
        dto.getSampleType() != null ? dto.getSampleType().getDescription() : null, sampleTypeTerm)) {
      score += 40;
    }
    if (matchesTerm(dto.getOriginLab(), originLabTerm)) {
      score += 35;
    }
    if (matchesTerm(dto.getProjectId(), projectIdTerm)) {
      score += 20;
    }
    if (sampleTypeTerm != null
        && originLabTerm != null
        && matchesTerm(
            dto.getSampleType() != null ? dto.getSampleType().getDescription() : null, sampleTypeTerm)
        && matchesTerm(dto.getOriginLab(), originLabTerm)) {
      score += 25;
    }
    return score;
  }

  private String determineMatchReason(
      BioSampleListDTO dto,
      String identityTerm,
      String barcodeTerm,
      String accessionTerm,
      String sampleTypeTerm,
      String originLabTerm,
      String projectIdTerm) {
    if (identityTerm != null) {
      String identityReason = resolveExactIdentityMatchReason(dto, identityTerm);
      if (identityReason != null) {
        return identityReason;
      }
    }
    if (accessionTerm != null) {
      String accessionReason = resolveExactIdentityMatchReason(dto, accessionTerm);
      if (accessionReason != null) {
        return accessionReason;
      }
    }
    if (barcodeTerm != null) {
      String barcodeReason = resolveExactIdentityMatchReason(dto, barcodeTerm);
      if (barcodeReason != null) {
        return barcodeReason;
      }
    }

    boolean sameType =
        matchesTerm(
            dto.getSampleType() != null ? dto.getSampleType().getDescription() : null, sampleTypeTerm);
    boolean sameOrigin = matchesTerm(dto.getOriginLab(), originLabTerm);
    boolean sameProject = matchesTerm(dto.getProjectId(), projectIdTerm);

    if (sameType && sameOrigin) {
      return "SAME_TYPE_ORIGIN";
    }
    if (sameProject) {
      return "RELATED_PROJECT";
    }
    if (sameType) {
      return "SAME_TYPE";
    }
    if (sameOrigin) {
      return "SAME_ORIGIN";
    }
    return null;
  }

  private boolean matchesTerm(String value, String term) {
    return term != null && value != null && value.toLowerCase(Locale.ROOT).contains(term);
  }

  private List<BioSampleListDTO> searchStoredInventoryByIdentity(
      String identity,
      String accessionNumber,
      String barcode,
      WorkflowStatus filterStatus,
      HttpServletRequest request) {
    List<String> terms = new ArrayList<>();
    if (identity != null) {
      terms.add(identity);
    }
    if (accessionNumber != null && terms.stream().noneMatch(t -> t.equalsIgnoreCase(accessionNumber))) {
      terms.add(accessionNumber);
    }
    if (barcode != null && terms.stream().noneMatch(t -> t.equalsIgnoreCase(barcode))) {
      terms.add(barcode);
    }

    for (String term : terms) {
      SampleItem sampleItem = sampleStorageService.resolveSampleItemByIdentifier(term);
      if (sampleItem == null) {
        sampleItem = sampleStorageService.findAssignedSampleItemByPartialIdentifier(term);
      }
      if (sampleItem == null) {
        continue;
      }
      BioSample bioSample = bioSampleService.ensureBioSampleForStoredSampleItem(sampleItem, null);
      if (bioSample == null) {
        continue;
      }
      BioSampleListDTO dto = buildSearchResultDTO(bioSample, filterStatus);
      if (dto == null || !canAccessFulfillmentDto(dto, request)) {
        continue;
      }
      List<BioSampleListDTO> matches = new ArrayList<>();
      matches.add(dto);
      return matches;
    }
    return new ArrayList<>();
  }

  private List<BioSampleListDTO> browseStoredAssignments(
      FulfillmentSearchInput input,
      WorkflowStatus filterStatus,
      int limit,
      HttpServletRequest request) {
    List<Map<String, Object>> rows = sampleStorageService.getAllSamplesWithAssignments();
    String sampleTypeTerm = input != null ? input.getSampleType() : null;
    String normalizedType =
        sampleTypeTerm != null ? sampleTypeTerm.trim().toLowerCase(Locale.ROOT) : null;
    String normalizedDept =
        input != null && input.getRequesterLabUnit() != null
            ? input.getRequesterLabUnit().trim().toLowerCase(Locale.ROOT)
            : null;

    List<BioSampleListDTO> typedMatches =
        collectStoredAssignmentDtos(
            rows, normalizedType, true, normalizedDept, true, filterStatus, limit, request);
    if (!typedMatches.isEmpty()) {
      return typedMatches;
    }
    if (normalizedType != null) {
      List<BioSampleListDTO> untypedMatches =
          collectStoredAssignmentDtos(
              rows, normalizedType, false, normalizedDept, true, filterStatus, limit, request);
      if (!untypedMatches.isEmpty()) {
        return untypedMatches;
      }
      if (normalizedDept != null) {
        return collectStoredAssignmentDtos(
            rows, normalizedType, false, normalizedDept, false, filterStatus, limit, request);
      }
      return untypedMatches;
    }
    return typedMatches;
  }

  private List<BioSampleListDTO> collectStoredAssignmentDtos(
      List<Map<String, Object>> rows,
      String normalizedType,
      boolean requireTypeMatch,
      String normalizedRequesterDept,
      boolean requireDeptMatch,
      WorkflowStatus filterStatus,
      int limit,
      HttpServletRequest request) {
    List<BioSampleListDTO> result = new ArrayList<>();
    int cap = Math.max(limit, STORAGE_BROWSE_CAP);
    for (Map<String, Object> row : rows) {
      if (result.size() >= cap) {
        break;
      }
      String location = row.get("location") != null ? String.valueOf(row.get("location")) : "";
      if (location.isBlank()) {
        continue;
      }
      if (requireTypeMatch && normalizedType != null) {
        String type = row.get("type") != null ? String.valueOf(row.get("type")).toLowerCase(Locale.ROOT) : "";
        if (!type.contains(normalizedType)) {
          continue;
        }
      }
      if (requireDeptMatch && normalizedRequesterDept != null) {
        String department =
            row.get("departmentName") != null
                ? String.valueOf(row.get("departmentName")).toLowerCase(Locale.ROOT)
                : "";
        if (!department.contains(normalizedRequesterDept)) {
          continue;
        }
      }
      String sampleItemId = row.get("id") != null ? String.valueOf(row.get("id")) : null;
      if (sampleItemId == null || sampleItemId.isBlank()) {
        continue;
      }
      SampleItem sampleItem = sampleStorageService.resolveSampleItemByIdentifier(sampleItemId);
      if (sampleItem == null) {
        continue;
      }
      BioSample bioSample = bioSampleService.ensureBioSampleForStoredSampleItem(sampleItem, null);
      if (bioSample == null) {
        continue;
      }
      BioSampleListDTO dto = buildSearchResultDTO(bioSample, filterStatus);
      if (dto == null || !canAccessFulfillmentDto(dto, request)) {
        continue;
      }
      result.add(dto);
    }
    return result;
  }

  private boolean canAccessFulfillmentDto(BioSampleListDTO dto, HttpServletRequest request) {
    if (departmentIsolationService.hasUnrestrictedDepartmentAccess(request)) {
      return true;
    }
    return dto.getSampleItemId() != null
        && departmentIsolationService.canAccessSampleItemIdentifier(
            String.valueOf(dto.getSampleItemId()), request);
  }

  private boolean isExactIdentityMatchReason(String matchReason) {
    return "EXACT_ACCESSION".equals(matchReason) || "EXACT_BARCODE".equals(matchReason);
  }

  private String resolveExactIdentityMatchReason(BioSampleListDTO dto, String term) {
    if (term == null || term.isBlank()) {
      return null;
    }
    String normalized = term.trim().toLowerCase(Locale.ROOT);
    if (dto.getAccessionNumber() != null) {
      String accession = dto.getAccessionNumber().trim().toLowerCase(Locale.ROOT);
      if (accession.equals(normalized) || accession.contains(normalized)) {
        return "EXACT_ACCESSION";
      }
    }
    if (dto.getBarcode() != null) {
      String barcode = dto.getBarcode().trim().toLowerCase(Locale.ROOT);
      if (barcode.equals(normalized) || barcode.contains(normalized)) {
        return "EXACT_BARCODE";
      }
    }
    if (dto.getSampleItemId() != null && String.valueOf(dto.getSampleItemId()).equals(term.trim())) {
      return "EXACT_BARCODE";
    }
    return null;
  }

  private String deriveExactIdentityInput(String inputIdentity, String accessionNumber, String barcode) {
    String identity = trimToNull(inputIdentity);
    if (identity != null) {
      return identity;
    }
    if (accessionNumber != null && barcode != null) {
      return accessionNumber.equalsIgnoreCase(barcode) ? accessionNumber : null;
    }
    return accessionNumber != null ? accessionNumber : barcode;
  }

  private Boolean determineSampleTypeMatch(
      BioSampleListDTO dto, String sampleTypeTerm, boolean exactIdentityMatch) {
    if (sampleTypeTerm == null) {
      return null;
    }

    boolean matches =
        matchesTerm(
            dto.getSampleType() != null ? dto.getSampleType().getDescription() : null, sampleTypeTerm);
    if (exactIdentityMatch) {
      return Boolean.valueOf(matches);
    }
    return matches ? Boolean.TRUE : null;
  }

  private String determineMismatchReason(Boolean sampleTypeMatchesRequested, boolean exactIdentityMatch) {
    if (!exactIdentityMatch || sampleTypeMatchesRequested == null || sampleTypeMatchesRequested.booleanValue()) {
      return null;
    }
    return "TYPE_MISMATCH";
  }

  private BioSampleListDTO buildSearchResultDTO(BioSample bioSample, WorkflowStatus filterStatus) {
    if (bioSample == null || bioSample.getSampleItem() == null) {
      return null;
    }
    if (filterStatus != null) {
      WorkflowStatus sampleStatus = bioSample.getWorkflowStatus();
      if (sampleStatus == null) {
        sampleStatus = WorkflowStatus.REGISTERED;
      }
      if (!filterStatus.equals(sampleStatus) && !isFulfillmentEligibleDespiteStatus(bioSample, filterStatus)) {
        return null;
      }
    }

    SampleItem sampleItem = bioSample.getSampleItem();
    BioSampleListDTO dto = new BioSampleListDTO();
    dto.setId(bioSample.getId());
    dto.setSampleItemId(Integer.valueOf(sampleItem.getId()));
    dto.setBarcode(sampleItem.getExternalId());
    dto.setWorkflowStatus(
        bioSample.getWorkflowStatus() != null ? bioSample.getWorkflowStatus().name() : "REGISTERED");
    dto.setBiosafetyLevel(
        bioSample.getBiosafetyLevel() != null ? bioSample.getBiosafetyLevel().name() : null);
    dto.setRetentionExpiryDate(bioSample.getRetentionExpiryDate());
    dto.setOriginLab(bioSample.getOriginLab());
    dto.setProjectId(bioSample.getProjectId());

    if (sampleItem.getTypeOfSample() != null) {
      dto.setSampleType(
          new BioSampleListDTO.SampleTypeDTO(
              sampleItem.getTypeOfSample().getId(), sampleItem.getTypeOfSample().getDescription()));
    }
    if (sampleItem.getSample() != null) {
      dto.setAccessionNumber(sampleItem.getSample().getAccessionNumber());
      dto.setSampleId(Integer.valueOf(sampleItem.getSample().getId()));
    }
    if (sampleItem.getCollectionDate() != null) {
      dto.setCollectionDate(sampleItem.getCollectionDate());
    }
    populateQuantityFields(dto, sampleItem);
    if (bioSample.getRetentionPolicyId() != null) {
      RetentionPolicy policy = retentionPolicyService.get(bioSample.getRetentionPolicyId());
      if (policy != null) {
        dto.setRetentionPolicyName(policy.getPolicyName());
        dto.setRetentionPolicyId(policy.getId());
      }
    }
    populateStorageLocation(dto, sampleItem);
    return dto;
  }

  private void populateQuantityFields(BioSampleListDTO dto, SampleItem sampleItem) {
    if (sampleItem.getQuantity() != null) {
      dto.setQuantity(sampleItem.getQuantity());
    }
    BigDecimal remaining = sampleItem.getEffectiveRemainingQuantity();
    if (remaining != null && remaining.compareTo(BigDecimal.ZERO) > 0) {
      dto.setRemainingQuantity(remaining.doubleValue());
    } else if (sampleItem.getQuantity() != null && sampleItem.getQuantity() > 0) {
      dto.setRemainingQuantity(sampleItem.getQuantity());
    } else {
      Map<String, Object> location = sampleStorageService.getSampleItemLocation(sampleItem.getId());
      boolean hasStorage =
          location != null
              && !location.isEmpty()
              && (location.get("location") != null || location.get("hierarchicalPath") != null);
      if (hasStorage) {
        dto.setRemainingQuantity(1.0);
      }
    }
    if (sampleItem.getUnitOfMeasureName() != null && !sampleItem.getUnitOfMeasureName().isBlank()) {
      dto.setUnitOfMeasure(sampleItem.getUnitOfMeasureName());
    }
  }

  private void populateStorageLocation(BioSampleListDTO dto, SampleItem sampleItem) {
    if (sampleItem.getId() == null) {
      return;
    }
    Map<String, Object> location = sampleStorageService.getSampleItemLocation(sampleItem.getId());
    if (location == null || location.isEmpty()) {
      return;
    }
    dto.setRoomName(asTrimmedString(location.get("roomName")));
    dto.setDeviceName(asTrimmedString(location.get("deviceName")));
    dto.setShelfLabel(asTrimmedString(location.get("shelfLabel")));
    dto.setRackLabel(asTrimmedString(location.get("rackLabel")));
    dto.setBoxLabel(asTrimmedString(location.get("boxLabel")));
    dto.setPositionCoordinate(asTrimmedString(location.get("positionCoordinate")));
    String hierarchicalPath = asTrimmedString(location.get("hierarchicalPath"));
    if (hierarchicalPath == null) {
      hierarchicalPath = asTrimmedString(location.get("location"));
    }
    dto.setHierarchicalPath(hierarchicalPath);
    dto.setSamplePath(Brf02SamplePathFormatter.format(location));
  }

  private String asTrimmedString(Object value) {
    if (value == null) {
      return null;
    }
    String text = String.valueOf(value).trim();
    return text.isEmpty() ? null : text;
  }

  private boolean isFulfillmentEligibleDespiteStatus(BioSample bioSample, WorkflowStatus filterStatus) {
    if (!WorkflowStatus.STORED.equals(filterStatus) || bioSample.getSampleItem() == null) {
      return false;
    }
    Map<String, Object> location =
        sampleStorageService.getSampleItemLocation(bioSample.getSampleItem().getId());
    return location != null
        && !location.isEmpty()
        && (location.get("location") != null || location.get("hierarchicalPath") != null);
  }

  private String wrapLikePattern(String value) {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    return "%" + value.trim() + "%";
  }

  private String trimToNull(String value) {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    return value.trim();
  }

  private Timestamp parseCollectionDateStart(String value) {
    LocalDate date = parseCollectionDate(value);
    return date != null ? Timestamp.valueOf(date.atStartOfDay()) : null;
  }

  private Timestamp parseCollectionDateEnd(String value) {
    LocalDate date = parseCollectionDate(value);
    return date != null ? Timestamp.valueOf(date.atTime(23, 59, 59)) : null;
  }

  private LocalDate parseCollectionDate(String value) {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    try {
      return LocalDate.parse(value.trim());
    } catch (DateTimeParseException e) {
      return null;
    }
  }
}
