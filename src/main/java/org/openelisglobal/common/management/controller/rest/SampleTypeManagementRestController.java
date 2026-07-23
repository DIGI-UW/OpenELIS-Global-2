package org.openelisglobal.common.management.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.sampletypeterminology.service.SampleTypeTerminologyMappingService;
import org.openelisglobal.sampletypeterminology.valueholder.SampleTypeTerminologyMapping;
import org.openelisglobal.testconfiguration.service.SampleTypeCreateService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class SampleTypeManagementRestController extends BaseRestController {

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private SampleTypeCreateService sampleTypeCreateService;

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
        private String containerType;
        private String storageTemperature;
        private boolean isActive = true;
        private int sortOrder;
        private int testCount;
        private String lastUpdated;

        // Constructors
        public SampleTypeManagementDTO() {
        }

        public SampleTypeManagementDTO(TypeOfSample typeOfSample) {
            this.id = typeOfSample.getId();

            // Debug the localization to see what's causing "en" to appear
            String nameValue = typeOfSample.getDescription(); // Default to description
            if (typeOfSample.getLocalization() != null) {
                String localizedValue = typeOfSample.getLocalization().getLocalizedValue("en");
                LogEvent.logInfo("SampleTypeManagementDTO", "constructor",
                        "DEBUG - ID: " + typeOfSample.getId() + ", Raw localized value: '" + localizedValue + "'"
                                + ", Description: '" + typeOfSample.getDescription() + "'");

                // Use localized value if it's clean, otherwise fall back to description
                if (localizedValue != null && !localizedValue.trim().isEmpty() && !localizedValue.contains("en")) {
                    nameValue = localizedValue;
                } else {
                    nameValue = typeOfSample.getDescription();
                    LogEvent.logWarn("SampleTypeManagementDTO", "constructor",
                            "Localized value contained 'en' or was empty, using description instead: '" + nameValue
                                    + "'");
                }
            }

            this.name = nameValue;
            this.description = typeOfSample.getDescription();
            this.domain = mapBackendDomainToFrontend(typeOfSample.getDomain()); // Map domain to frontend format
            this.abbreviation = typeOfSample.getLocalAbbreviation();
            this.whonetCode = typeOfSample.getWhonetCode();
            this.isActive = typeOfSample.getIsActive();
            this.sortOrder = typeOfSample.getSortOrder();
            // Note: containerType, storageTemperature are not yet in TypeOfSample entity
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

        public String getContainerType() {
            return containerType;
        }

        public void setContainerType(String containerType) {
            this.containerType = containerType;
        }

        public String getStorageTemperature() {
            return storageTemperature;
        }

        public void setStorageTemperature(String storageTemperature) {
            this.storageTemperature = storageTemperature;
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

    @GetMapping(value = "/sample-types/test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("Sample Type Management REST Controller is working!");
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

    @PostMapping(value = "/sample-types")
    public ResponseEntity<ApiResponse<SampleTypeManagementDTO>> createSampleType(HttpServletRequest request,
            @RequestBody @Valid SampleTypeManagementDTO sampleTypeDTO, BindingResult result) {

        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Validation errors occurred", null));
        }

        try {
            String userId = getSysUserId(request);

            // Create localization for the sample type name
            Localization localization = createLocalization(sampleTypeDTO.getName(), sampleTypeDTO.getName(), userId);

            // Create TypeOfSample entity
            TypeOfSample typeOfSample = createTypeOfSample(sampleTypeDTO, userId);

            // Save the sample type using the existing service
            try {
                sampleTypeCreateService.createAndInsertSampleType(localization, typeOfSample, null, null, null, null,
                        null, null);
            } catch (LIMSRuntimeException e) {
                LogEvent.logError("SampleTypeManagementRestController", "createSampleType", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ApiResponse<>(false, "Error creating sample type: " + e.getMessage(), null));
            }

            // Clear TypeOfSample cache and refresh the display lists to ensure new sample
            // types appear in order entry
            typeOfSampleService.clearCache();
            DisplayListService.getInstance().refreshList(DisplayListService.ListType.SAMPLE_TYPE);
            DisplayListService.getInstance().refreshList(DisplayListService.ListType.SAMPLE_TYPE_ACTIVE);
            DisplayListService.getInstance().refreshList(DisplayListService.ListType.SAMPLE_TYPE_INACTIVE);

            // Return the created sample type
            SampleTypeManagementDTO responseDTO = new SampleTypeManagementDTO(typeOfSample);
            return ResponseEntity.ok(new ApiResponse<>(true, "Sample type created successfully", responseDTO));

        } catch (Exception e) {
            LogEvent.logError("SampleTypeManagementRestController", "createSampleType", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error creating sample type: " + e.getMessage(), null));
        }
    }

    @PostMapping(value = "/sample-types/update")
    // @PreAuthorize("hasRole('ROLE_ADMIN')") // Temporarily removed to debug
    // persistence issues
    public ResponseEntity<ApiResponse<SampleTypeManagementDTO>> updateSampleType(HttpServletRequest request,
            @RequestBody @Valid SampleTypeManagementDTO sampleTypeDTO, BindingResult result) {

        if (result.hasErrors()) {
            // Log detailed validation errors for debugging
            StringBuilder errorDetails = new StringBuilder("Validation errors: ");
            result.getAllErrors().forEach(error -> {
                errorDetails.append(error.getDefaultMessage()).append("; ");
                LogEvent.logError("SampleTypeManagementRestController", "updateSampleType",
                        "Validation error: " + error.getDefaultMessage());
            });
            LogEvent.logError("SampleTypeManagementRestController", "updateSampleType",
                    "Update failed due to validation errors: " + errorDetails.toString());
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, errorDetails.toString(), null));
        }

        if (sampleTypeDTO.getId() == null || sampleTypeDTO.getId().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Sample type ID is required for updates", null));
        }

        try {
            // Log received data for debugging
            LogEvent.logInfo("SampleTypeManagementRestController", "updateSampleType",
                    "Received update request for sample type ID: " + sampleTypeDTO.getId());
            LogEvent.logInfo("SampleTypeManagementRestController", "updateSampleType",
                    "Received data - Name: " + sampleTypeDTO.getName() + ", Description: "
                            + sampleTypeDTO.getDescription() + ", Domain: " + sampleTypeDTO.getDomain()
                            + ", Abbreviation: " + sampleTypeDTO.getAbbreviation() + ", WHONET: "
                            + sampleTypeDTO.getWhonetCode() + ", Active: " + sampleTypeDTO.getIsActive());

            String userId = getSysUserId(request);

            // Find the existing sample type
            TypeOfSample existingTypeOfSample = typeOfSampleService.getTypeOfSampleById(sampleTypeDTO.getId());
            if (existingTypeOfSample == null) {
                LogEvent.logError("SampleTypeManagementRestController", "updateSampleType",
                        "Sample type not found in database with ID: " + sampleTypeDTO.getId());
                return ResponseEntity.badRequest().body(
                        new ApiResponse<>(false, "Sample type not found with ID: " + sampleTypeDTO.getId(), null));
            }

            LogEvent.logInfo("SampleTypeManagementRestController", "updateSampleType",
                    "Found existing sample type - Current name: "
                            + (existingTypeOfSample.getLocalization() != null
                                    ? existingTypeOfSample.getLocalization().getLocalizedValue()
                                    : existingTypeOfSample.getDescription())
                            + ", Current abbreviation: " + existingTypeOfSample.getLocalAbbreviation()
                            + ", Current domain: " + existingTypeOfSample.getDomain());

            LogEvent.logInfo("SampleTypeManagementRestController", "updateSampleType",
                    "Updating sample type ID: " + sampleTypeDTO.getId() + ", Name: " + sampleTypeDTO.getName());

            // Update the sample type properties with validation
            if (sampleTypeDTO.getDescription() != null && !sampleTypeDTO.getDescription().trim().isEmpty()) {
                existingTypeOfSample.setDescription(sampleTypeDTO.getDescription().trim());
                LogEvent.logInfo("SampleTypeManagementRestController", "updateSampleType",
                        "Updated description: '" + sampleTypeDTO.getDescription().trim() + "'");
            }

            // Map domain values from frontend to backend format
            if (sampleTypeDTO.getDomain() != null) {
                String domain = mapDomainValue(sampleTypeDTO.getDomain());
                existingTypeOfSample.setDomain(domain);
                LogEvent.logInfo("SampleTypeManagementRestController", "updateSampleType",
                        "Updated domain from " + sampleTypeDTO.getDomain() + " to " + domain);
            }

            // Set abbreviation - use same simple pattern as Description
            if (sampleTypeDTO.getAbbreviation() != null) {
                String abbreviation = sampleTypeDTO.getAbbreviation().trim();
                // Allow empty values to clear the field, but limit length
                if (abbreviation.length() <= 10) {
                    existingTypeOfSample.setLocalAbbreviation(abbreviation);
                    LogEvent.logInfo("SampleTypeManagementRestController", "updateSampleType",
                            "Updated abbreviation: '" + abbreviation + "'");
                } else {
                    LogEvent.logWarn("SampleTypeManagementRestController", "updateSampleType",
                            "Abbreviation too long (>10 chars), skipping: " + abbreviation);
                }
            }

            // Update WHONET code — allow empty string to clear, cap length at 5
            if (sampleTypeDTO.getWhonetCode() != null) {
                String whonetCode = sampleTypeDTO.getWhonetCode().trim();
                if (whonetCode.length() <= 5) {
                    existingTypeOfSample.setWhonetCode(whonetCode.isEmpty() ? null : whonetCode);
                    LogEvent.logInfo("SampleTypeManagementRestController", "updateSampleType",
                            "Updated WHONET code: '" + whonetCode + "'");
                } else {
                    LogEvent.logWarn("SampleTypeManagementRestController", "updateSampleType",
                            "WHONET code too long (>5 chars), skipping: " + whonetCode);
                }
            }

            if (sampleTypeDTO.getContainerType() != null && !sampleTypeDTO.getContainerType().trim().isEmpty()) {
                String containerType = sampleTypeDTO.getContainerType().trim();
                // TODO: Container type storage - TypeOfSample entity needs containerType field
                // For now, log the value for future implementation
                LogEvent.logInfo("SampleTypeManagementRestController", "updateSampleType",
                        "Container type received but not persisted (entity field needed): " + containerType);
            }

            if (sampleTypeDTO.getStorageTemperature() != null
                    && !sampleTypeDTO.getStorageTemperature().trim().isEmpty()) {
                String storageTemp = sampleTypeDTO.getStorageTemperature().trim();
                // TODO: Storage temperature storage - TypeOfSample entity needs
                // storageTemperature field
                // For now, log the value for future implementation
                LogEvent.logInfo("SampleTypeManagementRestController", "updateSampleType",
                        "Storage temperature received but not persisted (entity field needed): " + storageTemp);
            }

            // Update sort order if provided
            if (sampleTypeDTO.getSortOrder() > 0) {
                existingTypeOfSample.setSortOrder(sampleTypeDTO.getSortOrder());
            }

            // Update active status
            existingTypeOfSample.setIsActive(sampleTypeDTO.getIsActive());
            existingTypeOfSample.setSysUserId(userId);

            // Handle name update - Fixed localization approach (English only to avoid FR
            // issues)
            if (sampleTypeDTO.getName() != null) {
                String newName = sampleTypeDTO.getName().trim();
                if (!newName.isEmpty()) {
                    // Create new localization with English only to avoid French language issues
                    Localization newLocalization = new Localization();
                    newLocalization.setLocalizedValue("en", newName); // English only
                    newLocalization.setDescription("type of sample name");
                    newLocalization.setSysUserId(userId);

                    existingTypeOfSample.setLocalization(newLocalization);

                    LogEvent.logInfo("SampleTypeManagementRestController", "updateSampleType",
                            "Updated name with English-only localization: '" + newName + "'");
                } else {
                    LogEvent.logWarn("SampleTypeManagementRestController", "updateSampleType",
                            "Empty name provided, skipping name update");
                }
            }

            // Save the updated sample type
            try {
                LogEvent.logInfo("SampleTypeManagementRestController", "updateSampleType",
                        "Saving updated sample type to database");

                typeOfSampleService.save(existingTypeOfSample);

                LogEvent.logInfo("SampleTypeManagementRestController", "updateSampleType",
                        "Successfully saved sample type: " + existingTypeOfSample.getId());

                // Clear TypeOfSample cache and refresh display lists to ensure updated sample
                // types appear correctly
                typeOfSampleService.clearCache();
                DisplayListService.getInstance().refreshList(DisplayListService.ListType.SAMPLE_TYPE);
                DisplayListService.getInstance().refreshList(DisplayListService.ListType.SAMPLE_TYPE_ACTIVE);
                DisplayListService.getInstance().refreshList(DisplayListService.ListType.SAMPLE_TYPE_INACTIVE);

            } catch (Exception e) {
                LogEvent.logError("SampleTypeManagementRestController", "updateSampleType",
                        "Database save error: " + e.getMessage());
                LogEvent.logError("SampleTypeManagementRestController", "updateSampleType",
                        "Full exception: " + e.toString());

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ApiResponse<>(false, "Database update failed: " + e.getMessage(), null));
            }

            // Clear TypeOfSample cache and refresh the display lists to ensure updated
            // sample types appear in order entry
            typeOfSampleService.clearCache();
            DisplayListService.getInstance().refreshList(DisplayListService.ListType.SAMPLE_TYPE);
            DisplayListService.getInstance().refreshList(DisplayListService.ListType.SAMPLE_TYPE_ACTIVE);
            DisplayListService.getInstance().refreshList(DisplayListService.ListType.SAMPLE_TYPE_INACTIVE);

            // Return the updated sample type
            SampleTypeManagementDTO responseDTO = new SampleTypeManagementDTO(existingTypeOfSample);
            return ResponseEntity.ok(new ApiResponse<>(true, "Sample type updated successfully", responseDTO));

        } catch (Exception e) {
            LogEvent.logError("SampleTypeManagementRestController", "updateSampleType", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error updating sample type: " + e.getMessage(), null));
        }
    }

    private Localization createLocalization(String french, String english, String currentUserId) {
        Localization localization = new Localization();
        // Use the new non-deprecated method
        localization.setLocalizedValue("en", english);
        localization.setLocalizedValue("fr", french != null ? french : english); // Use English as fallback for French
        localization.setDescription("type of sample name");
        localization.setSysUserId(currentUserId);
        return localization;
    }

    private TypeOfSample createTypeOfSample(SampleTypeManagementDTO dto, String userId) {
        TypeOfSample typeOfSample = new TypeOfSample();
        typeOfSample.setDescription(dto.getDescription());

        // Map domain values from frontend to backend format
        String domain = mapDomainValue(dto.getDomain());
        typeOfSample.setDomain(domain);

        // Set abbreviation, default to first 10 characters of name if not provided
        String abbreviation = dto.getAbbreviation();
        if (abbreviation == null || abbreviation.trim().isEmpty()) {
            abbreviation = dto.getName().length() > 10 ? dto.getName().substring(0, 10) : dto.getName();
        }
        typeOfSample.setLocalAbbreviation(abbreviation);

        // Persist WHONET code if provided (max 5 chars)
        if (dto.getWhonetCode() != null && !dto.getWhonetCode().trim().isEmpty()) {
            String whonetCode = dto.getWhonetCode().trim();
            if (whonetCode.length() <= 5) {
                typeOfSample.setWhonetCode(whonetCode);
            }
        }

        typeOfSample.setIsActive(true); // New sample types are active by default
        typeOfSample.setSortOrder(Integer.MAX_VALUE); // Add at the end
        typeOfSample.setSysUserId(userId);

        // Create name key for localization
        String nameKey = dto.getName().replaceAll(" ", "_").replaceAll("[^a-zA-Z0-9_]", "");
        typeOfSample.setNameKey("Sample.type." + nameKey);

        return typeOfSample;
    }

    private String mapDomainValue(String frontendDomain) {
        if ("CLINICAL".equalsIgnoreCase(frontendDomain)) {
            return "H"; // Human
        } else if ("ENVIRONMENTAL".equalsIgnoreCase(frontendDomain)) {
            return "E"; // Environmental
        } else if ("VECTOR".equalsIgnoreCase(frontendDomain)) {
            return "V"; // Vector
        } else if ("BOTH".equalsIgnoreCase(frontendDomain)) {
            return "B"; // Both
        }
        return "H"; // Default to Human
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
     * Maps backend domain codes to frontend domain values. Backend: H
     * (Human/Clinical), E (Environmental), V (Vector), A (Animal) Frontend:
     * CLINICAL, ENVIRONMENTAL, VECTOR
     */
    private static String mapBackendDomainToFrontend(String backendDomain) {
        if (backendDomain == null || backendDomain.trim().isEmpty()) {
            return "CLINICAL"; // Default to Clinical
        }

        switch (backendDomain.toUpperCase()) {
        case "H":
            return "CLINICAL"; // Human/Clinical
        case "E":
            return "ENVIRONMENTAL"; // Environmental
        case "V":
            return "VECTOR"; // Vector surveillance
        case "A":
            return "CLINICAL"; // Animal samples show as Clinical for now
        case "B":
            return "CLINICAL"; // Both - default to Clinical for now
        default:
            return "CLINICAL"; // Default to Clinical for unknown domains
        }
    }

}