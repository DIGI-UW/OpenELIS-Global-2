package org.openelisglobal.common.management.controller.rest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.management.form.SampleTypeManagementForm;
import org.openelisglobal.common.management.valueholder.SampleTypeDisplay;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.typeofsample.dao.TypeOfSampleDAO;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.springframework.validation.BindingResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.annotation.JsonProperty;

@RestController
@RequestMapping("/rest")
@PreAuthorize("hasRole('ADMIN')")
public class SampleTypeManagementRestController extends BaseController {

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private TypeOfSampleTestService typeOfSampleTestService;

    @Autowired
    private LocalizationService localizationService;

    @RequestMapping(value = "/SampleTypeManagement", method = RequestMethod.GET)
    public SampleTypeManagementForm showSampleTypeManagement(
            HttpServletRequest request,
            @RequestParam(required = false, defaultValue = "N") String search,
            @RequestParam(required = false, defaultValue = "1") int startingRecNo,
            @RequestParam(required = false) String searchString,
            @RequestParam(required = false) String filter) {

        SampleTypeManagementForm form = new SampleTypeManagementForm();

        try {
            // Get all human sample types (domain = HUMAN)
            List<TypeOfSample> sampleTypes = typeOfSampleService.getTypesForDomain(TypeOfSampleDAO.SampleDomain.HUMAN);
            List<SampleTypeDisplay> displayList = new ArrayList<>();

            // Convert to display objects
            for (TypeOfSample sampleType : sampleTypes) {
                if (sampleType != null) {
                    // Skip inactive sample types by default (only show active types unless "showAll" filter is specified)
                    if (!sampleType.isActive() && !"showAll".equals(filter)) {
                        continue;
                    }

                    // Skip placeholder entries
                    if (sampleType.getDescription() != null &&
                        sampleType.getDescription().toLowerCase().contains("actual type will be selected")) {
                        continue;
                    }

                    SampleTypeDisplay display = new SampleTypeDisplay();
                    display.setId(sampleType.getId());
                    display.setDescription(sampleType.getDescription());
                    display.setLocalAbbreviation(sampleType.getLocalAbbreviation());
                    display.setSortOrder(String.valueOf(sampleType.getSortOrder()));
                    display.setActive(sampleType.isActive());
                    display.setDomain(sampleType.getDomain());

                    // Get test count for this sample type
                    List<TypeOfSampleTest> sampleTypeTests = typeOfSampleTestService.getTypeOfSampleTestsForSampleType(sampleType.getId());
                    display.setTestCount(sampleTypeTests != null ? sampleTypeTests.size() : 0);

                    // Set WHONET code (placeholder for now - to be implemented based on requirements)
                    display.setWhonetCode("");

                    // Set storage defaults (placeholder for now - to be implemented based on requirements)
                    display.setStorageDefaults("");

                    // Apply search filter
                    boolean matchesSearch = true;
                    if (searchString != null && !searchString.trim().isEmpty()) {
                        String searchTerm = searchString.toLowerCase().trim();
                        matchesSearch = (sampleType.getDescription() != null &&
                                        sampleType.getDescription().toLowerCase().contains(searchTerm)) ||
                                       (sampleType.getLocalAbbreviation() != null &&
                                        sampleType.getLocalAbbreviation().toLowerCase().contains(searchTerm));
                    }

                    // Apply active filter (this now only affects further filtering if "isActive" is specifically requested)
                    boolean matchesActiveFilter = true;
                    if ("isActive".equals(filter)) {
                        matchesActiveFilter = sampleType.isActive();
                    }

                    if (matchesSearch && matchesActiveFilter) {
                        displayList.add(display);
                    }
                }
            }

            form.setMenuList(displayList);
            form.setTotalRecordCount(String.valueOf(displayList.size()));
            form.setFromRecordCount("1");
            form.setToRecordCount(String.valueOf(displayList.size()));
            form.setSearchString(searchString != null ? searchString : "");

        } catch (LIMSRuntimeException e) {
            // Handle error gracefully
            form.setMenuList(new ArrayList<>());
            form.setTotalRecordCount("0");
            form.setFromRecordCount("0");
            form.setToRecordCount("0");
        }

        return form;
    }

    @RequestMapping(value = "/SampleTypeManagement/deactivate", method = RequestMethod.POST)
    public ResponseEntity<?> deactivateSampleTypes(HttpServletRequest request, @RequestParam String IDS) {
        try {
            String userId = getSysUserId(request);

            if (IDS != null && !IDS.trim().isEmpty()) {
                String[] ids = IDS.split(",");

                for (String id : ids) {
                    if (id != null && !id.trim().isEmpty()) {
                        TypeOfSample sampleType = typeOfSampleService.get(id.trim());
                        if (sampleType != null) {
                            sampleType.setIsActive(false);
                            sampleType.setSysUserId(userId);
                            sampleType.setLastupdatedFields();
                            typeOfSampleService.update(sampleType);
                        }
                    }
                }
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @RequestMapping(value = "/SampleTypeManagement/export", method = RequestMethod.GET)
    public ResponseEntity<byte[]> exportSampleTypes(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) String searchString,
            @RequestParam(required = false) String filter) {

        try {
            // Get filtered data using the same logic as the main endpoint
            List<TypeOfSample> sampleTypes = typeOfSampleService.getTypesForDomain(TypeOfSampleDAO.SampleDomain.HUMAN);
            List<SampleTypeDisplay> displayList = new ArrayList<>();

            for (TypeOfSample sampleType : sampleTypes) {
                if (sampleType != null) {
                    // Apply search filter
                    boolean matchesSearch = true;
                    if (searchString != null && !searchString.trim().isEmpty()) {
                        String searchTerm = searchString.toLowerCase().trim();
                        matchesSearch = (sampleType.getDescription() != null &&
                                        sampleType.getDescription().toLowerCase().contains(searchTerm)) ||
                                       (sampleType.getLocalAbbreviation() != null &&
                                        sampleType.getLocalAbbreviation().toLowerCase().contains(searchTerm));
                    }

                    // Apply active filter
                    boolean matchesActiveFilter = true;
                    if ("isActive".equals(filter)) {
                        matchesActiveFilter = sampleType.isActive();
                    }

                    if (matchesSearch && matchesActiveFilter) {
                        SampleTypeDisplay display = new SampleTypeDisplay();
                        display.setId(sampleType.getId());
                        display.setDescription(sampleType.getDescription());
                        display.setLocalAbbreviation(sampleType.getLocalAbbreviation());
                        display.setSortOrder(String.valueOf(sampleType.getSortOrder()));
                        display.setActive(sampleType.isActive());
                        display.setDomain(sampleType.getDomain());
                        displayList.add(display);
                    }
                }
            }

            // Generate CSV
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
                 CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

                // Header
                csvPrinter.printRecord(Arrays.asList(
                    MessageUtil.getMessage("sample.type.name"),
                    MessageUtil.getMessage("sample.type.display.order"),
                    MessageUtil.getMessage("sample.type.whonet.code"),
                    MessageUtil.getMessage("sample.type.test.count"),
                    MessageUtil.getMessage("sample.type.storage.defaults"),
                    MessageUtil.getMessage("label.status")
                ));

                // Data rows
                for (SampleTypeDisplay display : displayList) {
                    csvPrinter.printRecord(Arrays.asList(
                        display.getDescription(),
                        display.getSortOrder(),
                        display.getWhonetCode() != null ? display.getWhonetCode() : "",
                        String.valueOf(display.getTestCount()),
                        display.getStorageDefaults() != null ? display.getStorageDefaults() : "",
                        display.isActive() ? "Active" : "Inactive"
                    ));
                }

                csvPrinter.flush();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=\"sample_types_export.csv\"");
            headers.add("Content-Type", "text/csv; charset=UTF-8");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(outputStream.toByteArray());

        } catch (IOException | LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "sampleTypeManagementDefinition";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return null;
    }

    @Override
    protected String getPageSubtitleKey() {
        return null;
    }

    /**
     * Create a new sample type
     */
    @RequestMapping(value = "/SampleTypeManagement/create", method = RequestMethod.POST)
    public ResponseEntity<CreateSampleTypeResponse> createSampleType(
            HttpServletRequest request,
            @Valid @RequestBody CreateSampleTypeRequest createRequest,
            BindingResult bindingResult) {

        CreateSampleTypeResponse response = new CreateSampleTypeResponse();

        if (bindingResult.hasErrors()) {
            response.setSuccess(false);
            response.setMessage("Validation errors occurred");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            String userId = getSysUserId(request);

            // Create TypeOfSample entity
            TypeOfSample typeOfSample = new TypeOfSample();
            typeOfSample.setDescription(createRequest.getName());
            typeOfSample.setLocalAbbreviation(createRequest.getName().substring(0,
                Math.min(createRequest.getName().length(), 10)));
            typeOfSample.setSortOrder(createRequest.getSortOrder());
            typeOfSample.setDomain("HUMAN"); // Default domain
            typeOfSample.setActive(createRequest.isActive());
            typeOfSample.setSysUserId(userId);

            // Save TypeOfSample directly without localization for now
            String sampleTypeId = typeOfSampleService.insert(typeOfSample);
            typeOfSample.setId(sampleTypeId);

            response.setSuccess(true);
            response.setMessage("Sample type created successfully");
            response.setSampleTypeId(sampleTypeId);

            return ResponseEntity.ok(response);

        } catch (LIMSRuntimeException e) {
            response.setSuccess(false);
            response.setMessage("Error creating sample type: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("Unexpected error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Request class for creating sample types
     */
    public static class CreateSampleTypeRequest {
        @NotBlank
        private String name;

        private String description = "";

        private int sortOrder = 999;

        private String whonetCode = "";

        private String storageDefaults = "";

        @JsonProperty("isActive")
        private boolean isActive = true;

        // Getters and setters
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

        public int getSortOrder() {
            return sortOrder;
        }

        public void setSortOrder(int sortOrder) {
            this.sortOrder = sortOrder;
        }

        public String getWhonetCode() {
            return whonetCode;
        }

        public void setWhonetCode(String whonetCode) {
            this.whonetCode = whonetCode;
        }

        public String getStorageDefaults() {
            return storageDefaults;
        }

        public void setStorageDefaults(String storageDefaults) {
            this.storageDefaults = storageDefaults;
        }

        public boolean isActive() {
            return isActive;
        }

        public void setActive(boolean active) {
            isActive = active;
        }
    }

    /**
     * Response class for sample type creation
     */
    public static class CreateSampleTypeResponse {
        private boolean success;
        private String message;
        private String sampleTypeId;

        // Getters and setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getSampleTypeId() {
            return sampleTypeId;
        }

        public void setSampleTypeId(String sampleTypeId) {
            this.sampleTypeId = sampleTypeId;
        }
    }

    /**
     * Update an existing sample type
     */
    @RequestMapping(value = "/SampleTypeManagement/update", method = RequestMethod.POST)
    public ResponseEntity<UpdateSampleTypeResponse> updateSampleType(
            HttpServletRequest request,
            @Valid @RequestBody UpdateSampleTypeRequest updateRequest,
            BindingResult bindingResult) {

        UpdateSampleTypeResponse response = new UpdateSampleTypeResponse();

        if (bindingResult.hasErrors()) {
            response.setSuccess(false);
            response.setMessage("Validation errors occurred");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            String userId = getSysUserId(request);

            // Find existing TypeOfSample
            TypeOfSample typeOfSample = typeOfSampleService.get(updateRequest.getId());
            if (typeOfSample == null) {
                response.setSuccess(false);
                response.setMessage("Sample type not found");
                return ResponseEntity.notFound().build();
            }

            // Update TypeOfSample fields
            typeOfSample.setDescription(updateRequest.getSampleTypeEnglishName());
            typeOfSample.setLocalAbbreviation(updateRequest.getSampleTypeEnglishName().substring(0,
                Math.min(updateRequest.getSampleTypeEnglishName().length(), 10)));
            typeOfSample.setSortOrder(updateRequest.getDisplayOrder());
            typeOfSample.setSysUserId(userId);

            // Update localization if it exists
            Localization localization = typeOfSample.getLocalization();
            if (localization != null) {
                localization.setDescription(updateRequest.getSampleTypeEnglishName());
                localization.setEnglish(updateRequest.getSampleTypeEnglishName());
                localization.setFrench(updateRequest.getSampleTypeFrenchName());
                localization.setSysUserId(userId);
                localizationService.update(localization);
            } else {
                // Create new localization if none exists
                localization = new Localization();
                localization.setDescription(updateRequest.getSampleTypeEnglishName());
                localization.setEnglish(updateRequest.getSampleTypeEnglishName());
                localization.setFrench(updateRequest.getSampleTypeFrenchName());
                localization.setSysUserId(userId);
                localizationService.insert(localization);
                typeOfSample.setLocalization(localization);
            }

            // Update TypeOfSample
            typeOfSampleService.update(typeOfSample);

            response.setSuccess(true);
            response.setMessage("Sample type updated successfully");
            response.setSampleTypeId(typeOfSample.getId());

            return ResponseEntity.ok(response);

        } catch (LIMSRuntimeException e) {
            response.setSuccess(false);
            response.setMessage("Error updating sample type: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get sample type details for editing
     */
    @RequestMapping(value = "/SampleTypeManagement/{id}", method = RequestMethod.GET)
    public ResponseEntity<SampleTypeDetailResponse> getSampleTypeDetail(
            HttpServletRequest request,
            @PathVariable("id") String sampleTypeId) {

        try {
            TypeOfSample typeOfSample = typeOfSampleService.get(sampleTypeId);
            if (typeOfSample == null) {
                return ResponseEntity.notFound().build();
            }

            SampleTypeDetailResponse response = new SampleTypeDetailResponse();
            response.setId(typeOfSample.getId());
            response.setSampleTypeEnglishName(typeOfSample.getDescription());
            response.setDisplayOrder(typeOfSample.getSortOrder());
            response.setWhonetCode(""); // Placeholder
            response.setStorageDefaults(""); // Placeholder

            // Get French name from localization
            Localization localization = typeOfSample.getLocalization();
            if (localization != null) {
                response.setSampleTypeFrenchName(localization.getFrench());
            } else {
                response.setSampleTypeFrenchName("");
            }

            return ResponseEntity.ok(response);

        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Request class for updating sample types
     */
    public static class UpdateSampleTypeRequest {
        @NotBlank
        private String id;

        @NotBlank
        private String sampleTypeEnglishName;

        @NotBlank
        private String sampleTypeFrenchName;

        private int displayOrder = 999;

        private String whonetCode = "";

        private String storageDefaults = "";

        // Getters and setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSampleTypeEnglishName() {
            return sampleTypeEnglishName;
        }

        public void setSampleTypeEnglishName(String sampleTypeEnglishName) {
            this.sampleTypeEnglishName = sampleTypeEnglishName;
        }

        public String getSampleTypeFrenchName() {
            return sampleTypeFrenchName;
        }

        public void setSampleTypeFrenchName(String sampleTypeFrenchName) {
            this.sampleTypeFrenchName = sampleTypeFrenchName;
        }

        public int getDisplayOrder() {
            return displayOrder;
        }

        public void setDisplayOrder(int displayOrder) {
            this.displayOrder = displayOrder;
        }

        public String getWhonetCode() {
            return whonetCode;
        }

        public void setWhonetCode(String whonetCode) {
            this.whonetCode = whonetCode;
        }

        public String getStorageDefaults() {
            return storageDefaults;
        }

        public void setStorageDefaults(String storageDefaults) {
            this.storageDefaults = storageDefaults;
        }
    }

    /**
     * Response class for sample type update
     */
    public static class UpdateSampleTypeResponse {
        private boolean success;
        private String message;
        private String sampleTypeId;

        // Getters and setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getSampleTypeId() {
            return sampleTypeId;
        }

        public void setSampleTypeId(String sampleTypeId) {
            this.sampleTypeId = sampleTypeId;
        }
    }

    /**
     * Response class for sample type details
     */
    public static class SampleTypeDetailResponse {
        private String id;
        private String sampleTypeEnglishName;
        private String sampleTypeFrenchName;
        private int displayOrder;
        private String whonetCode;
        private String storageDefaults;

        // Getters and setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSampleTypeEnglishName() {
            return sampleTypeEnglishName;
        }

        public void setSampleTypeEnglishName(String sampleTypeEnglishName) {
            this.sampleTypeEnglishName = sampleTypeEnglishName;
        }

        public String getSampleTypeFrenchName() {
            return sampleTypeFrenchName;
        }

        public void setSampleTypeFrenchName(String sampleTypeFrenchName) {
            this.sampleTypeFrenchName = sampleTypeFrenchName;
        }

        public int getDisplayOrder() {
            return displayOrder;
        }

        public void setDisplayOrder(int displayOrder) {
            this.displayOrder = displayOrder;
        }

        public String getWhonetCode() {
            return whonetCode;
        }

        public void setWhonetCode(String whonetCode) {
            this.whonetCode = whonetCode;
        }

        public String getStorageDefaults() {
            return storageDefaults;
        }

        public void setStorageDefaults(String storageDefaults) {
            this.storageDefaults = storageDefaults;
        }
    }
}
