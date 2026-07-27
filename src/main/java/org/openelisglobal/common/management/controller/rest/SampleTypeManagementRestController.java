package org.openelisglobal.common.management.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        private boolean isActive = true;
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

        public boolean getIsActive() {
            return isActive;
        }

        public void setIsActive(boolean isActive) {
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

    @PutMapping(value = "/sample-types/{sampleTypeId}")
    public ResponseEntity<ApiResponse<SampleTypeManagementDTO>> updateSampleType(HttpServletRequest request,
            @PathVariable String sampleTypeId, @RequestBody @Valid SampleTypeManagementDTO sampleTypeDTO,
            BindingResult result) {

        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Validation errors occurred", null));
        }

        try {
            String userId = getSysUserId(request);

            TypeOfSample existingTypeOfSample = typeOfSampleService.getTypeOfSampleById(sampleTypeId);
            if (existingTypeOfSample == null) {
                return ResponseEntity.notFound().build();
            }

            if (sampleTypeDTO.getDescription() != null && !sampleTypeDTO.getDescription().trim().isEmpty()) {
                existingTypeOfSample.setDescription(sampleTypeDTO.getDescription().trim());
            }

            // Domain (single, required — OGC-296 v2.1) is stored on the legacy
            // char column using the D-030 mapping (OGC-1145). Only rewrite the
            // char when the admin actually changed the domain, so legacy codes
            // that alias to the same domain (e.g. 'N' → Clinical) are preserved.
            if (sampleTypeDTO.getDomain() != null && !sampleTypeDTO.getDomain()
                    .equals(mapBackendDomainToFrontend(existingTypeOfSample.getDomain()))) {
                existingTypeOfSample.setDomain(mapFrontendDomainToLegacyCode(sampleTypeDTO.getDomain()));
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

            if (sampleTypeDTO.getSortOrder() > 0) {
                existingTypeOfSample.setSortOrder(sampleTypeDTO.getSortOrder());
            }

            existingTypeOfSample.setIsActive(sampleTypeDTO.getIsActive());
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

    /**
     * Legacy {@code sample_domain} char → Clinical/Environmental/Vector, per the
     * D-030 mapping shipped with OGC-1145 (H uman + N ewborn = Clinical, E
     * nvironmental, A nimal = Vector). The enum-valued domain column remains a
     * declared migration (OGC-296 v2.1 Dependency 4).
     */
    private static String mapBackendDomainToFrontend(String backendDomain) {
        if (backendDomain == null || backendDomain.trim().isEmpty()) {
            return "CLINICAL";
        }
        switch (backendDomain.toUpperCase()) {
        case "E":
            return "ENVIRONMENTAL";
        case "A":
            return "VECTOR";
        default:
            return "CLINICAL";
        }
    }

    /** Clinical/Environmental/Vector → the legacy char (see above). */
    private static String mapFrontendDomainToLegacyCode(String frontendDomain) {
        if (frontendDomain == null || frontendDomain.trim().isEmpty()) {
            return "H";
        }
        switch (frontendDomain.toUpperCase()) {
        case "ENVIRONMENTAL":
            return "E";
        case "VECTOR":
            return "A";
        default:
            return "H";
        }
    }

}