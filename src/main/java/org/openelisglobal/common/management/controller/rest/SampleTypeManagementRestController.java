package org.openelisglobal.common.management.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openelisglobal.common.domain.Domain;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.sampletypeterminology.service.SampleTypeTerminologyMappingService;
import org.openelisglobal.sampletypeterminology.valueholder.SampleTypeTerminologyMapping;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest")
@PreAuthorize("hasRole('ADMIN')")
public class SampleTypeManagementRestController extends BaseRestController {

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private SampleTypeTerminologyMappingService terminologyService;

    @Autowired
    private org.openelisglobal.test.service.TestService testService;

    @Autowired
    private org.openelisglobal.typeofsample.service.TypeOfSampleTestService typeOfSampleTestService;

    // Kept in sync with the frontend `SOURCES` array in TerminologySection.jsx.
    private static final Set<String> TERM_SOURCES = new HashSet<>(
            Arrays.asList("LOINC", "SNOMED", "CIEL", "OCL", "WHONET"));
    private static final Set<String> TERM_RELATIONSHIPS = new HashSet<>(
            Arrays.asList("SAME_AS", "BROADER_THAN", "NARROWER_THAN"));

    /**
     * DTO for Sample Type Management
     */
    public static class SampleTypeManagementDTO {
        private String id;
        private String name;
        private String description;
        private String domain;
        private String abbreviation;
        private String whonetCode;
        private String disposalInstructions;
        private Boolean isActive;
        private int sortOrder;
        private int testCount;
        private String lastUpdated;

        // Constructors
        public SampleTypeManagementDTO() {
        }

        public SampleTypeManagementDTO(TypeOfSample typeOfSample) {
            this.id = typeOfSample.getId();

            String nameValue = typeOfSample.getDescription();
            if (typeOfSample.getLocalization() != null) {
                String localizedValue = typeOfSample.getLocalization().getLocalizedValue("en");
                if (localizedValue != null && !localizedValue.trim().isEmpty()) {
                    nameValue = localizedValue;
                }
            }
            this.name = nameValue;
            this.description = typeOfSample.getDescription();
            this.domain = mapBackendDomainToFrontend(typeOfSample.getDomain()); // Map domain to frontend format
            this.abbreviation = typeOfSample.getLocalAbbreviation();
            this.whonetCode = typeOfSample.getWhonetCode();
            this.disposalInstructions = typeOfSample.getDisposalInstructions();
            this.isActive = typeOfSample.getIsActive();
            this.sortOrder = typeOfSample.getSortOrder();
        }

        // Getters and Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getDomain() {
            return domain;
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }

        public String getAbbreviation() {
            return abbreviation;
        }

        public void setAbbreviation(String abbreviation) {
            this.abbreviation = abbreviation;
        }

        public String getWhonetCode() {
            return whonetCode;
        }

        public void setWhonetCode(String whonetCode) {
            this.whonetCode = whonetCode;
        }

        public String getDisposalInstructions() {
            return disposalInstructions;
        }

        public void setDisposalInstructions(String disposalInstructions) {
            this.disposalInstructions = disposalInstructions;
        }

        public Boolean getIsActive() {
            return isActive;
        }

        public void setIsActive(Boolean isActive) {
            this.isActive = isActive;
        }

        public int getSortOrder() {
            return sortOrder;
        }

        public void setSortOrder(int sortOrder) {
            this.sortOrder = sortOrder;
        }

        public int getTestCount() {
            return testCount;
        }

        public void setTestCount(int testCount) {
            this.testCount = testCount;
        }

        public String getLastUpdated() {
            return lastUpdated;
        }

        public void setLastUpdated(String lastUpdated) {
            this.lastUpdated = lastUpdated;
        }
    }

    /**
     * Response wrapper for API responses
     */
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;

        public ApiResponse(boolean success, String message, T data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        // Getters
        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public T getData() {
            return data;
        }
    }

    @GetMapping(value = "/sample-types")
    public ResponseEntity<ApiResponse<List<SampleTypeManagementDTO>>> getAllSampleTypes() {
        try {
            List<TypeOfSample> typeOfSamples = typeOfSampleService.getAllTypeOfSamplesSortOrdered();
            List<SampleTypeManagementDTO> sampleTypeDTOs = new ArrayList<>();

            for (TypeOfSample typeOfSample : typeOfSamples) {
                SampleTypeManagementDTO dto = new SampleTypeManagementDTO(typeOfSample);

                // Calculate and set actual test count for this sample type
                try {
                    int testCount = typeOfSampleService.getAllTestsBySampleTypeId(typeOfSample.getId()).size();
                    dto.setTestCount(testCount);
                } catch (Exception e) {
                    LogEvent.logWarn("SampleTypeManagementRestController", "getAllSampleTypes",
                            "Failed to get test count for sample type " + typeOfSample.getId() + ": " + e.getMessage());
                    dto.setTestCount(0); // Default to 0 if count fails
                }

                sampleTypeDTOs.add(dto);
            }

            return ResponseEntity.ok(new ApiResponse<>(true, "Sample types retrieved successfully", sampleTypeDTOs));
        } catch (Exception e) {
            LogEvent.logError("SampleTypeManagementRestController", "getAllSampleTypes", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error retrieving sample types: " + e.getMessage(), null));
        }
    }

    @GetMapping(value = "/sample-types/{sampleTypeId}")
    public ResponseEntity<ApiResponse<SampleTypeManagementDTO>> getSampleType(@PathVariable String sampleTypeId) {
        TypeOfSample typeOfSample = typeOfSampleService.getTypeOfSampleById(sampleTypeId);
        if (typeOfSample == null) {
            return ResponseEntity.notFound().build();
        }
        SampleTypeManagementDTO dto = new SampleTypeManagementDTO(typeOfSample);
        dto.setTestCount(typeOfSampleService.getAllTestsBySampleTypeId(sampleTypeId).size());
        return ResponseEntity.ok(new ApiResponse<>(true, "Sample type retrieved successfully", dto));
    }

    /** Body for the display-order move: a 1-based target position. */
    public static class DisplayOrderRequest {
        public Integer position;
    }

    @PutMapping(value = "/sample-types/{sampleTypeId}/display-order")
    public ResponseEntity<ApiResponse<List<SampleTypeManagementDTO>>> updateDisplayOrder(HttpServletRequest request,
            @PathVariable String sampleTypeId, @RequestBody DisplayOrderRequest body) {
        if (body == null || body.position == null || body.position < 1) {
            return ResponseEntity.unprocessableEntity()
                    .body(new ApiResponse<>(false, "position must be a 1-based integer", null));
        }
        if (typeOfSampleService.getTypeOfSampleById(sampleTypeId) == null) {
            return ResponseEntity.notFound().build();
        }
        List<TypeOfSample> ordered = typeOfSampleService.moveToSortOrderPosition(sampleTypeId, body.position,
                getSysUserId(request));

        // Reflect the new ordering in order entry immediately.
        typeOfSampleService.clearCache();
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.SAMPLE_TYPE);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.SAMPLE_TYPE_ACTIVE);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.SAMPLE_TYPE_INACTIVE);

        List<SampleTypeManagementDTO> dtos = new ArrayList<>();
        for (TypeOfSample type : ordered) {
            dtos.add(new SampleTypeManagementDTO(type));
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Display order updated successfully", dtos));
    }

    @PutMapping(value = "/sample-types/{sampleTypeId}")
    public ResponseEntity<ApiResponse<SampleTypeManagementDTO>> updateSampleType(HttpServletRequest request,
            @PathVariable String sampleTypeId, @RequestBody @Valid SampleTypeManagementDTO sampleTypeDTO,
            BindingResult result) {

        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Validation errors occurred", null));
        }

        try {
            TypeOfSample existingTypeOfSample = typeOfSampleService.getTypeOfSampleById(sampleTypeId);
            if (existingTypeOfSample == null) {
                return ResponseEntity.notFound().build();
            }

            String userId = getSysUserId(request);

            if (sampleTypeDTO.getDescription() != null && !sampleTypeDTO.getDescription().trim().isEmpty()) {
                existingTypeOfSample.setDescription(sampleTypeDTO.getDescription().trim());
            }

            // Domain (single, required — OGC-296 v2.1). Stored as the enum
            // value since the Dependency-4 migration; only rewritten when the
            // admin actually changed it, so any legacy-coded row keeps its
            // original code until its domain genuinely changes.
            if (sampleTypeDTO.getDomain() != null && !sampleTypeDTO.getDomain()
                    .equals(mapBackendDomainToFrontend(existingTypeOfSample.getDomain()))) {
                existingTypeOfSample.setDomain(Domain.normalize(sampleTypeDTO.getDomain()));
            }

            if (sampleTypeDTO.getAbbreviation() != null) {
                String abbreviation = sampleTypeDTO.getAbbreviation().trim();
                if (abbreviation.length() <= 10) {
                    existingTypeOfSample.setLocalAbbreviation(abbreviation);
                }
            }

            // WHONET code — empty string clears it; the column caps at 5 chars.
            if (sampleTypeDTO.getWhonetCode() != null) {
                String whonetCode = sampleTypeDTO.getWhonetCode().trim();
                if (whonetCode.length() <= 5) {
                    existingTypeOfSample.setWhonetCode(whonetCode.isEmpty() ? null : whonetCode);
                }
            }

            // Disposal instructions (OGC-296 v2.1) — free-text reference;
            // empty string clears it.
            if (sampleTypeDTO.getDisposalInstructions() != null) {
                String disposal = sampleTypeDTO.getDisposalInstructions().trim();
                existingTypeOfSample.setDisposalInstructions(disposal.isEmpty() ? null : disposal);
            }

            if (sampleTypeDTO.getSortOrder() > 0) {
                existingTypeOfSample.setSortOrder(sampleTypeDTO.getSortOrder());
            }

            // Null means "not sent" — section saves (e.g. Disposal) must not
            // flip an inactive type back to active.
            if (sampleTypeDTO.getIsActive() != null) {
                existingTypeOfSample.setIsActive(sampleTypeDTO.getIsActive());
            }
            existingTypeOfSample.setSysUserId(userId);

            // Rename updates the EXISTING localization in place (the mapping
            // cascades) — creating a fresh Localization here would orphan the
            // old row and drop the non-English values.
            if (sampleTypeDTO.getName() != null && !sampleTypeDTO.getName().trim().isEmpty()) {
                String newName = sampleTypeDTO.getName().trim();
                Localization localization = existingTypeOfSample.getLocalization();
                if (localization == null) {
                    localization = new Localization();
                    localization.setDescription("type of sample name");
                    existingTypeOfSample.setLocalization(localization);
                }
                localization.setLocalizedValue("en", newName);
                localization.setSysUserId(userId);
            }

            typeOfSampleService.save(existingTypeOfSample);

            // Reflect the change in order entry immediately.
            typeOfSampleService.clearCache();
            DisplayListService.getInstance().refreshList(DisplayListService.ListType.SAMPLE_TYPE);
            DisplayListService.getInstance().refreshList(DisplayListService.ListType.SAMPLE_TYPE_ACTIVE);
            DisplayListService.getInstance().refreshList(DisplayListService.ListType.SAMPLE_TYPE_INACTIVE);

            SampleTypeManagementDTO responseDTO = new SampleTypeManagementDTO(existingTypeOfSample);
            return ResponseEntity.ok(new ApiResponse<>(true, "Sample type updated successfully", responseDTO));

        } catch (Exception e) {
            LogEvent.logError("SampleTypeManagementRestController", "updateSampleType", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error updating sample type: " + e.getMessage(), null));
        }
    }

    /** One terminology mapping for a sample type. */
    public static class TerminologyMappingDto {
        public String id;
        public String source;
        public String code;
        public String relationship;

        public TerminologyMappingDto() {
        }

        public TerminologyMappingDto(SampleTypeTerminologyMapping m) {
            this.id = m.getId();
            this.source = m.getSource();
            this.code = m.getCode();
            this.relationship = m.getRelationship();
        }
    }

    public static class TerminologyResponse {
        public String sampleTypeId;
        public List<TerminologyMappingDto> mappings = new ArrayList<>();
    }

    @GetMapping(value = "/sample-types/{sampleTypeId}/terminology", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TerminologyResponse> getTerminology(@PathVariable String sampleTypeId) {
        if (typeOfSampleService.getTypeOfSampleById(sampleTypeId) == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toTerminology(sampleTypeId));
    }

    @PutMapping(value = "/sample-types/{sampleTypeId}/terminology", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TerminologyResponse> saveTerminology(@PathVariable String sampleTypeId,
            @RequestBody TerminologyResponse body, HttpServletRequest request) {
        if (typeOfSampleService.getTypeOfSampleById(sampleTypeId) == null) {
            return ResponseEntity.notFound().build();
        }
        // (source, code) unique within the request — the DB enforces it per sample
        // type, but reject early + cleanly rather than surfacing a raw 500.
        Set<String> seen = new HashSet<>();
        List<SampleTypeTerminologyMapping> desired = new ArrayList<>();
        for (TerminologyMappingDto m : body.mappings) {
            if (isBlank(m.source) || !TERM_SOURCES.contains(m.source) || isBlank(m.code)) {
                return ResponseEntity.unprocessableEntity().build();
            }
            if (!isBlank(m.relationship) && !TERM_RELATIONSHIPS.contains(m.relationship)) {
                return ResponseEntity.unprocessableEntity().build();
            }
            if (!seen.add(m.source + " " + m.code)) {
                return ResponseEntity.unprocessableEntity().build();
            }
            SampleTypeTerminologyMapping e = new SampleTypeTerminologyMapping();
            e.setSource(m.source);
            e.setCode(m.code);
            e.setRelationship(isBlank(m.relationship) ? null : m.relationship);
            desired.add(e);
        }
        terminologyService.saveMappingsForSampleType(sampleTypeId, desired, getSysUserId(request));
        return ResponseEntity.ok(toTerminology(sampleTypeId));
    }

    private TerminologyResponse toTerminology(String sampleTypeId) {
        TerminologyResponse resp = new TerminologyResponse();
        resp.sampleTypeId = sampleTypeId;
        for (SampleTypeTerminologyMapping m : terminologyService.getActiveBySampleTypeId(sampleTypeId)) {
            resp.mappings.add(new TerminologyMappingDto(m));
        }
        return resp;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** A test row for the Associated Tests section (both linked and candidates). */
    public static class AssociatedTestDto {
        public String id;
        public String name;
        public String domain;
        public boolean active;

        public AssociatedTestDto(org.openelisglobal.test.valueholder.Test test) {
            this.id = test.getId();
            this.name = org.openelisglobal.test.service.TestServiceImpl.getLocalizedTestNameWithType(test);
            this.domain = Domain.normalize(test.getDomain());
            this.active = "Y".equals(test.getIsActive());
        }
    }

    /**
     * Tests NOT yet linked to this sample type, for the Associated Tests
     * autocomplete. Always constrained to this sample type's own domain (D-030), so
     * an environmental type never offers clinical tests and vice versa. An optional
     * {@code sampleTypeFilter} narrows further to tests currently linked to that
     * other sample type. {@code search} is an optional name substring (the UI
     * filters client-side, but this keeps the endpoint usable directly). This is
     * the sample-type side of the bidirectional test↔sample-type link.
     */
    @GetMapping(value = "/sample-types/{sampleTypeId}/associable-tests", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AssociatedTestDto>> getAssociableTests(@PathVariable String sampleTypeId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String search,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String sampleTypeFilter) {
        TypeOfSample sampleType = typeOfSampleService.getTypeOfSampleById(sampleTypeId);
        if (sampleType == null) {
            return ResponseEntity.notFound().build();
        }
        // This sample type's domain (D-030). Null (legacy/blank) → no domain
        // restriction, so legacy data never hides candidates.
        Domain ownDomain = Domain.fromRaw(sampleType.getDomain());

        Set<String> linkedTestIds = new HashSet<>();
        for (org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest link : typeOfSampleTestService
                .getTypeOfSampleTestsForSampleType(sampleTypeId)) {
            linkedTestIds.add(link.getTestId());
        }

        // Optional "tests currently on another sample type" narrowing.
        Set<String> filterTestIds = null;
        if (!isBlank(sampleTypeFilter)) {
            filterTestIds = new HashSet<>();
            for (org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest link : typeOfSampleTestService
                    .getTypeOfSampleTestsForSampleType(sampleTypeFilter)) {
                filterTestIds.add(link.getTestId());
            }
        }

        String needle = isBlank(search) ? null : search.trim().toLowerCase();
        List<AssociatedTestDto> candidates = new ArrayList<>();
        for (org.openelisglobal.test.valueholder.Test test : testService.getAllTests(false)) {
            if (linkedTestIds.contains(test.getId())) {
                continue;
            }
            if (ownDomain != null && Domain.fromRaw(test.getDomain()) != ownDomain) {
                continue;
            }
            if (filterTestIds != null && !filterTestIds.contains(test.getId())) {
                continue;
            }
            AssociatedTestDto dto = new AssociatedTestDto(test);
            if (needle != null && !dto.name.toLowerCase().contains(needle)) {
                continue;
            }
            candidates.add(dto);
        }
        candidates.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        return ResponseEntity.ok(candidates);
    }

    /** Link an existing test to this sample type (idempotent). */
    @PutMapping(value = "/sample-types/{sampleTypeId}/tests/{testId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> addTestToSampleType(HttpServletRequest request,
            @PathVariable String sampleTypeId, @PathVariable String testId) {
        if (typeOfSampleService.getTypeOfSampleById(sampleTypeId) == null || testService.getTestById(testId) == null) {
            return ResponseEntity.notFound().build();
        }
        boolean alreadyLinked = typeOfSampleTestService.getTypeOfSampleTestsForSampleType(sampleTypeId).stream()
                .anyMatch(link -> testId.equals(link.getTestId()));
        if (!alreadyLinked) {
            String userId = getSysUserId(request);
            org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest link = new org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest();
            link.setTypeOfSampleId(sampleTypeId);
            link.setTestId(testId);
            link.setSysUserId(userId);
            typeOfSampleTestService.insert(link);
            refreshOrderEntryLists();
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Test linked to sample type", null));
    }

    /** Unlink a test from this sample type. */
    @org.springframework.web.bind.annotation.DeleteMapping(value = "/sample-types/{sampleTypeId}/tests/{testId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> removeTestFromSampleType(HttpServletRequest request,
            @PathVariable String sampleTypeId, @PathVariable String testId) {
        if (typeOfSampleService.getTypeOfSampleById(sampleTypeId) == null) {
            return ResponseEntity.notFound().build();
        }
        String userId = getSysUserId(request);
        boolean removed = false;
        for (org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest link : typeOfSampleTestService
                .getTypeOfSampleTestsForSampleType(sampleTypeId)) {
            if (testId.equals(link.getTestId())) {
                typeOfSampleTestService.delete(link.getId(), userId);
                removed = true;
            }
        }
        if (removed) {
            refreshOrderEntryLists();
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Test unlinked from sample type", null));
    }

    private void refreshOrderEntryLists() {
        typeOfSampleService.clearCache();
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.SAMPLE_TYPE);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.SAMPLE_TYPE_ACTIVE);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.SAMPLE_TYPE_INACTIVE);
    }

    /**
     * The column stores the enum value since the OGC-296 Dependency-4 migration;
     * this stays bilingual for legacy one-character rows (D-030) that arrive from
     * fixtures or plugin inserts.
     */
    private static String mapBackendDomainToFrontend(String backendDomain) {
        return Domain.normalize(backendDomain);
    }
}