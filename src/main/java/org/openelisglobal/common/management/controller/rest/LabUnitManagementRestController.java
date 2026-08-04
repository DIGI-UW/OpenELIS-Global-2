package org.openelisglobal.common.management.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.domain.Domain;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.service.SupportedLocaleService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.localization.valueholder.SupportedLocale;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.systemmodule.valueholder.SystemModule;
import org.openelisglobal.systemusermodule.valueholder.RoleModule;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testconfiguration.service.TestSectionCreateService;
import org.openelisglobal.testconfiguration.service.TestSectionTestAssignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * OGC-189 (Lab Units Management Redesign): unified admin CRUD surface for lab
 * units (test sections). Mirrors the SampleTypeManagementRestController shape
 * so the two catalog admin screens stay consistent.
 *
 * <p>
 * Names are locale-generic: the DTO carries a {@code names} map keyed by locale
 * code, sourced from the multi-language localization_value mechanism
 * (OGC-1112), so any active locale — not just English/French — can be managed.
 * Creation is handled here (with the full names map) but still wires the
 * workplan/results/validation system+role modules exactly like the legacy
 * /rest/TestSectionCreate flow. The legacy TestSection* endpoints remain in
 * place.
 */
@RestController
@RequestMapping("/rest")
@PreAuthorize("hasRole('ADMIN')")
public class LabUnitManagementRestController extends BaseRestController {

    // test_section column limits: NAME VARCHAR(20), DESCRIPTION VARCHAR(60)
    private static final int NAME_MAX_LENGTH = 20;
    private static final int DESCRIPTION_MAX_LENGTH = 60;

    @Autowired
    private TestSectionService testSectionService;

    @Autowired
    private LocalizationService localizationService;

    @Autowired
    private SupportedLocaleService supportedLocaleService;

    @Autowired
    private TestService testService;

    @Autowired
    private TestSectionCreateService testSectionCreateService;

    @Autowired
    private TestSectionTestAssignService testSectionTestAssignService;

    @Autowired
    private RoleService roleService;

    /** DTO for the unified Lab Units list and editor. */
    public static class LabUnitManagementDTO {
        private String id;
        private String name;
        /** Locale code → name, one entry per language with a stored value. */
        private Map<String, String> names;
        private String description;
        private String domain;
        private Boolean isActive;
        private Boolean isExternal;
        private int sortOrder;
        private int testCount;

        public LabUnitManagementDTO() {
        }

        public LabUnitManagementDTO(TestSection testSection) {
            this.id = testSection.getId();
            String nameValue = testSection.getTestSectionName();
            Localization localization = testSection.getLocalization();
            if (localization != null) {
                this.names = localization.getValuesAsMap();
                String localized = localization.getLocalizedValue();
                if (localized != null && !localized.trim().isEmpty()) {
                    nameValue = localized;
                }
            } else {
                this.names = new HashMap<>();
            }
            this.name = nameValue;
            this.description = testSection.getDescription();
            this.domain = Domain.normalize(testSection.getDomain());
            this.isActive = "Y".equals(testSection.getIsActive());
            this.isExternal = "Y".equals(testSection.getIsExternal());
            this.sortOrder = testSection.getSortOrderInt();
        }

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

        public Map<String, String> getNames() {
            return names;
        }

        public void setNames(Map<String, String> names) {
            this.names = names;
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

        public Boolean getIsActive() {
            return isActive;
        }

        public void setIsActive(Boolean isActive) {
            this.isActive = isActive;
        }

        public Boolean getIsExternal() {
            return isExternal;
        }

        public void setIsExternal(Boolean isExternal) {
            this.isExternal = isExternal;
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
    }

    /** Response wrapper matching the sample-types endpoints. */
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;

        public ApiResponse(boolean success, String message, T data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

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

    @GetMapping(value = "/lab-units-management")
    public ResponseEntity<ApiResponse<List<LabUnitManagementDTO>>> getAllLabUnits() {
        try {
            List<TestSection> sections = new ArrayList<>(testSectionService.getAllTestSections());
            sections.sort(Comparator.comparingInt(TestSection::getSortOrderInt));
            List<LabUnitManagementDTO> dtos = new ArrayList<>();
            for (TestSection section : sections) {
                LabUnitManagementDTO dto = new LabUnitManagementDTO(section);
                dto.setTestCount(countTestsInSection(section.getId()));
                dtos.add(dto);
            }
            return ResponseEntity.ok(new ApiResponse<>(true, "Lab units retrieved successfully", dtos));
        } catch (Exception e) {
            LogEvent.logError("LabUnitManagementRestController", "getAllLabUnits", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error retrieving lab units: " + e.getMessage(), null));
        }
    }

    @GetMapping(value = "/lab-units-management/{labUnitId}")
    public ResponseEntity<ApiResponse<LabUnitManagementDTO>> getLabUnit(@PathVariable String labUnitId) {
        TestSection section = testSectionService.getTestSectionById(labUnitId);
        if (section == null) {
            return ResponseEntity.notFound().build();
        }
        LabUnitManagementDTO dto = new LabUnitManagementDTO(section);
        dto.setTestCount(countTestsInSection(labUnitId));
        return ResponseEntity.ok(new ApiResponse<>(true, "Lab unit retrieved successfully", dto));
    }

    /**
     * Create a lab unit. Accepts the full locale-keyed names map; the fallback
     * locale's name is the identifying name (test_section.NAME, 20 chars). Wires
     * the same Workplan/LogbookResults/ResultValidation system+role modules as the
     * legacy create so results entry and validation work for the new unit.
     */
    @PostMapping(value = "/lab-units-management")
    public ResponseEntity<ApiResponse<LabUnitManagementDTO>> createLabUnit(HttpServletRequest request,
            @RequestBody LabUnitManagementDTO labUnitDTO) {
        String fallbackCode = fallbackLocaleCode();
        Map<String, String> names = labUnitDTO.getNames();
        String invalidLocale = firstInvalidLocale(names);
        if (invalidLocale != null) {
            return ResponseEntity.unprocessableEntity()
                    .body(new ApiResponse<>(false, "Unsupported locale code: " + invalidLocale, null));
        }
        String identifyingName = names == null ? null : trimToNull(names.get(fallbackCode));
        if (identifyingName == null) {
            return ResponseEntity.unprocessableEntity().body(
                    new ApiResponse<>(false, "names." + fallbackCode + " (fallback locale name) is required", null));
        }
        if (identifyingName.length() > NAME_MAX_LENGTH) {
            return ResponseEntity.unprocessableEntity()
                    .body(new ApiResponse<>(false, "name must be at most " + NAME_MAX_LENGTH + " characters", null));
        }
        String description = trimToNull(labUnitDTO.getDescription());
        if (description == null) {
            description = identifyingName;
        }
        if (description.length() > DESCRIPTION_MAX_LENGTH) {
            return ResponseEntity.unprocessableEntity().body(new ApiResponse<>(false,
                    "description must be at most " + DESCRIPTION_MAX_LENGTH + " characters", null));
        }

        String userId = getSysUserId(request);

        Localization localization = new Localization();
        localization.setDescription("test unit name");
        localization.setSysUserId(userId);
        for (Map.Entry<String, String> entry : names.entrySet()) {
            String value = trimToNull(entry.getValue());
            if (value != null) {
                localization.setLocalizedValue(entry.getKey(), value);
            }
        }

        TestSection testSection = new TestSection();
        testSection.setDescription(description);
        testSection.setTestSectionName(identifyingName);
        // Inactive until tests are assigned (a section with no tests is not
        // orderable); the editor's Active toggle covers explicit activation.
        testSection.setIsActive("N");
        testSection.setNameKey("testSection." + identifyingName.replaceAll(" ", "_"));
        testSection.setSortOrderInt(Integer.MAX_VALUE);
        testSection.setDomain(Domain.normalize(labUnitDTO.getDomain()));
        testSection.setSysUserId(userId);

        SystemModule workplanModule = createSystemModule("Workplan", identifyingName, userId);
        SystemModule resultModule = createSystemModule("LogbookResults", identifyingName, userId);
        SystemModule validationModule = createSystemModule("ResultValidation", identifyingName, userId);

        Role resultsEntryRole = roleService.getRoleByName(Constants.ROLE_RESULTS);
        Role validationRole = roleService.getRoleByName(Constants.ROLE_VALIDATION);

        try {
            testSectionCreateService.insertTestSection(localization, testSection, workplanModule, resultModule,
                    validationModule, createRoleModule(userId, workplanModule, resultsEntryRole),
                    createRoleModule(userId, resultModule, resultsEntryRole),
                    createRoleModule(userId, validationModule, validationRole));
        } catch (LIMSDuplicateRecordException e) {
            return ResponseEntity.unprocessableEntity()
                    .body(new ApiResponse<>(false, "A lab unit with this name already exists", null));
        } catch (Exception e) {
            LogEvent.logError("LabUnitManagementRestController", "createLabUnit", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error creating lab unit: " + e.getMessage(), null));
        }

        refreshLabUnitLists();

        LabUnitManagementDTO responseDTO = new LabUnitManagementDTO(testSection);
        responseDTO.setTestCount(0);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Lab unit created successfully", responseDTO));
    }

    @PutMapping(value = "/lab-units-management/{labUnitId}")
    public ResponseEntity<ApiResponse<LabUnitManagementDTO>> updateLabUnit(HttpServletRequest request,
            @PathVariable String labUnitId, @RequestBody LabUnitManagementDTO labUnitDTO) {
        try {
            TestSection section = testSectionService.getTestSectionById(labUnitId);
            if (section == null) {
                return ResponseEntity.notFound().build();
            }
            if (labUnitDTO.getDescription() != null
                    && labUnitDTO.getDescription().trim().length() > DESCRIPTION_MAX_LENGTH) {
                return ResponseEntity.unprocessableEntity().body(new ApiResponse<>(false,
                        "description must be at most " + DESCRIPTION_MAX_LENGTH + " characters", null));
            }
            Map<String, String> names = labUnitDTO.getNames();
            String invalidLocale = firstInvalidLocale(names);
            if (invalidLocale != null) {
                return ResponseEntity.unprocessableEntity()
                        .body(new ApiResponse<>(false, "Unsupported locale code: " + invalidLocale, null));
            }
            String fallbackCode = fallbackLocaleCode();
            if (names != null && names.containsKey(fallbackCode) && trimToNull(names.get(fallbackCode)) == null) {
                return ResponseEntity.unprocessableEntity().body(new ApiResponse<>(false,
                        "names." + fallbackCode + " (fallback locale name) cannot be blank", null));
            }

            String userId = getSysUserId(request);

            // Rename updates the EXISTING localization in place — its
            // localization_value rows carry every language, so creating a fresh
            // Localization would orphan them. Locales absent from the map are
            // left untouched; a blank value clears that translation.
            if (names != null && !names.isEmpty()) {
                Localization localization = section.getLocalization();
                boolean isNew = localization == null;
                if (isNew) {
                    localization = new Localization();
                    localization.setDescription("test unit name");
                    section.setLocalization(localization);
                }
                for (Map.Entry<String, String> entry : names.entrySet()) {
                    localization.setLocalizedValue(entry.getKey(),
                            entry.getValue() == null ? "" : entry.getValue().trim());
                }
                localization.setSysUserId(userId);
                if (isNew) {
                    localizationService.insert(localization);
                } else {
                    localizationService.update(localization);
                }
            }

            if (labUnitDTO.getDescription() != null && !labUnitDTO.getDescription().trim().isEmpty()) {
                section.setDescription(labUnitDTO.getDescription().trim());
            }

            // Only rewritten when the admin actually changed it, so legacy rows
            // keep their stored value until the domain genuinely changes. The
            // change is forward-looking (OGC-361 CFG-4) — no data migration.
            if (labUnitDTO.getDomain() != null
                    && !Domain.normalize(labUnitDTO.getDomain()).equals(Domain.normalize(section.getDomain()))) {
                String previousDomain = section.getDomain();
                section.setDomain(Domain.normalize(labUnitDTO.getDomain()));
                LogEvent.logInfo(this.getClass().getSimpleName(), "updateLabUnit",
                        "Lab unit " + section.getTestSectionName() + " domain changed from " + previousDomain + " to "
                                + section.getDomain() + " by " + userId);
            }

            // Null means "not sent" — section saves must not flip status.
            if (labUnitDTO.getIsActive() != null) {
                section.setIsActive(labUnitDTO.getIsActive() ? "Y" : "N");
            }

            section.setSysUserId(userId);
            testSectionService.update(section);

            refreshLabUnitLists();

            LabUnitManagementDTO responseDTO = new LabUnitManagementDTO(section);
            responseDTO.setTestCount(countTestsInSection(labUnitId));
            return ResponseEntity.ok(new ApiResponse<>(true, "Lab unit updated successfully", responseDTO));
        } catch (Exception e) {
            LogEvent.logError("LabUnitManagementRestController", "updateLabUnit", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error updating lab unit: " + e.getMessage(), null));
        }
    }

    /** Body for the display-order move: a 1-based target position. */
    public static class DisplayOrderRequest {
        public Integer position;
    }

    @PutMapping(value = "/lab-units-management/{labUnitId}/display-order")
    public ResponseEntity<ApiResponse<List<LabUnitManagementDTO>>> updateDisplayOrder(HttpServletRequest request,
            @PathVariable String labUnitId, @RequestBody DisplayOrderRequest body) {
        if (body == null || body.position == null || body.position < 1) {
            return ResponseEntity.unprocessableEntity()
                    .body(new ApiResponse<>(false, "position must be a 1-based integer", null));
        }
        if (testSectionService.getTestSectionById(labUnitId) == null) {
            return ResponseEntity.notFound().build();
        }
        List<TestSection> ordered = testSectionService.moveToSortOrderPosition(labUnitId, body.position,
                getSysUserId(request));

        refreshLabUnitLists();

        List<LabUnitManagementDTO> dtos = new ArrayList<>();
        for (TestSection section : ordered) {
            dtos.add(new LabUnitManagementDTO(section));
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Display order updated successfully", dtos));
    }

    /** A test row for the Assigned Tests section. */
    public static class AssignedTestDto {
        public String id;
        public String name;
        public String domain;
        public boolean active;

        public AssignedTestDto(Test test) {
            this.id = test.getId();
            this.name = org.openelisglobal.test.service.TestServiceImpl.getLocalizedTestNameWithType(test);
            this.domain = Domain.normalize(test.getDomain());
            this.active = "Y".equals(test.getIsActive());
        }
    }

    @GetMapping(value = "/lab-units-management/{labUnitId}/tests", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AssignedTestDto>> getAssignedTests(@PathVariable String labUnitId) {
        if (testSectionService.getTestSectionById(labUnitId) == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(assignedTestDtos(labUnitId));
    }

    /** A candidate test for the Assign dialog, with its current lab unit. */
    public static class AssignableTestDto extends AssignedTestDto {
        public String currentLabUnitId;
        public String currentLabUnitName;

        public AssignableTestDto(Test test, TestSectionService testSectionService) {
            super(test);
            TestSection currentSection = test.getTestSection();
            if (currentSection != null) {
                this.currentLabUnitId = currentSection.getId();
                this.currentLabUnitName = testSectionService.getUserLocalizedTesSectionName(currentSection);
            }
        }
    }

    /**
     * Active tests NOT currently assigned to this lab unit, for the bulk Assign
     * dialog. Each row carries the test's current lab unit so the admin can see
     * where it moves from (assignment lives on the test — moving it here removes it
     * there).
     */
    @GetMapping(value = "/lab-units-management/{labUnitId}/assignable-tests", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AssignableTestDto>> getAssignableTests(@PathVariable String labUnitId,
            @RequestParam(required = false) String search) {
        if (testSectionService.getTestSectionById(labUnitId) == null) {
            return ResponseEntity.notFound().build();
        }
        String needle = trimToNull(search) == null ? null : search.trim().toLowerCase();
        List<AssignableTestDto> candidates = new ArrayList<>();
        for (Test test : testService.getAllTests(false)) {
            if (!"Y".equals(test.getIsActive())) {
                continue;
            }
            if (test.getTestSection() != null && labUnitId.equals(test.getTestSection().getId())) {
                continue;
            }
            AssignableTestDto dto = new AssignableTestDto(test, testSectionService);
            if (needle != null && !dto.name.toLowerCase().contains(needle)) {
                continue;
            }
            candidates.add(dto);
        }
        candidates.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        return ResponseEntity.ok(candidates);
    }

    /** Body for the bulk assign/reassign actions. */
    public static class BulkTestAssignmentRequest {
        public List<String> testIds;
        public String destinationLabUnitId;
    }

    /** Bulk-assign tests INTO this lab unit (moves them off their current unit). */
    @PostMapping(value = "/lab-units-management/{labUnitId}/tests/assign")
    public ResponseEntity<ApiResponse<List<AssignedTestDto>>> assignTests(HttpServletRequest request,
            @PathVariable String labUnitId, @RequestBody BulkTestAssignmentRequest body) {
        if (body == null || body.testIds == null || body.testIds.isEmpty()) {
            return ResponseEntity.unprocessableEntity()
                    .body(new ApiResponse<>(false, "testIds must be a non-empty list", null));
        }
        if (testSectionService.getTestSectionById(labUnitId) == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            testSectionTestAssignService.assignTestsToSection(body.testIds, labUnitId, getSysUserId(request));
        } catch (Exception e) {
            LogEvent.logError("LabUnitManagementRestController", "assignTests", e.getMessage());
            return ResponseEntity.unprocessableEntity()
                    .body(new ApiResponse<>(false, "Error assigning tests: " + e.getMessage(), null));
        }
        refreshLabUnitLists();
        return ResponseEntity.ok(new ApiResponse<>(true, "Tests assigned successfully", assignedTestDtos(labUnitId)));
    }

    /**
     * Bulk-reassign tests OUT of this lab unit into a destination unit. Every
     * requested test must currently belong to this unit — the confirmation dialog's
     * test list and what actually moves stay in lockstep.
     */
    @PostMapping(value = "/lab-units-management/{labUnitId}/tests/reassign")
    public ResponseEntity<ApiResponse<List<AssignedTestDto>>> reassignTests(HttpServletRequest request,
            @PathVariable String labUnitId, @RequestBody BulkTestAssignmentRequest body) {
        if (body == null || body.testIds == null || body.testIds.isEmpty()
                || trimToNull(body.destinationLabUnitId) == null) {
            return ResponseEntity.unprocessableEntity()
                    .body(new ApiResponse<>(false, "testIds and destinationLabUnitId are required", null));
        }
        if (labUnitId.equals(body.destinationLabUnitId)) {
            return ResponseEntity.unprocessableEntity()
                    .body(new ApiResponse<>(false, "destination must differ from the source lab unit", null));
        }
        if (testSectionService.getTestSectionById(labUnitId) == null
                || testSectionService.getTestSectionById(body.destinationLabUnitId) == null) {
            return ResponseEntity.notFound().build();
        }
        Set<String> assignedIds = new HashSet<>();
        for (Test test : testSectionService.getTestsInSection(labUnitId)) {
            assignedIds.add(test.getId());
        }
        for (String testId : body.testIds) {
            if (!assignedIds.contains(testId)) {
                return ResponseEntity.unprocessableEntity()
                        .body(new ApiResponse<>(false, "Test " + testId + " is not assigned to this lab unit", null));
            }
        }
        try {
            testSectionTestAssignService.assignTestsToSection(body.testIds, body.destinationLabUnitId,
                    getSysUserId(request));
        } catch (Exception e) {
            LogEvent.logError("LabUnitManagementRestController", "reassignTests", e.getMessage());
            return ResponseEntity.unprocessableEntity()
                    .body(new ApiResponse<>(false, "Error reassigning tests: " + e.getMessage(), null));
        }
        refreshLabUnitLists();
        return ResponseEntity.ok(new ApiResponse<>(true, "Tests reassigned successfully", assignedTestDtos(labUnitId)));
    }

    private List<AssignedTestDto> assignedTestDtos(String labUnitId) {
        List<AssignedTestDto> dtos = new ArrayList<>();
        for (Test test : testSectionService.getTestsInSection(labUnitId)) {
            dtos.add(new AssignedTestDto(test));
        }
        dtos.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        return dtos;
    }

    private int countTestsInSection(String testSectionId) {
        try {
            return testSectionService.getTestsInSection(testSectionId).size();
        } catch (Exception e) {
            LogEvent.logWarn("LabUnitManagementRestController", "countTestsInSection",
                    "Failed to get test count for lab unit " + testSectionId + ": " + e.getMessage());
            return 0;
        }
    }

    /**
     * Every mutation must refresh the cached section lists so order entry and the
     * other admin screens see the change immediately.
     */
    private void refreshLabUnitLists() {
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.TEST_SECTION_ACTIVE);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.TEST_SECTION_INACTIVE);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.TEST_SECTION_BY_NAME);
        testSectionService.refreshNames();
    }

    /**
     * The fallback locale's name is the identifying/required one. Defaults to "en"
     * if no fallback is configured (matching Localization's universal fallback).
     */
    private String fallbackLocaleCode() {
        String code = supportedLocaleService.getFallbackLocaleCode();
        return trimToNull(code) == null ? "en" : code;
    }

    /**
     * Returns the first locale code in the map that is not an active supported
     * locale, or null when all are valid. Keeps typos from silently creating
     * localization_value rows no screen can ever display.
     */
    private String firstInvalidLocale(Map<String, String> names) {
        if (names == null || names.isEmpty()) {
            return null;
        }
        Set<String> activeCodes = new HashSet<>();
        for (SupportedLocale locale : supportedLocaleService.getAllActive()) {
            activeCodes.add(locale.getLocaleCode());
        }
        for (String code : names.keySet()) {
            if (!activeCodes.contains(code)) {
                return code;
            }
        }
        return null;
    }

    private SystemModule createSystemModule(String menuItem, String identifyingName, String userId) {
        SystemModule module = new SystemModule();
        module.setSystemModuleName(menuItem + ":" + identifyingName);
        module.setDescription(menuItem + "=>" + identifyingName);
        module.setSysUserId(userId);
        module.setHasAddFlag("Y");
        module.setHasDeleteFlag("Y");
        module.setHasSelectFlag("Y");
        module.setHasUpdateFlag("Y");
        return module;
    }

    private RoleModule createRoleModule(String userId, SystemModule module, Role role) {
        RoleModule roleModule = new RoleModule();
        roleModule.setRole(role);
        roleModule.setSystemModule(module);
        roleModule.setSysUserId(userId);
        roleModule.setHasAdd("Y");
        roleModule.setHasDelete("Y");
        roleModule.setHasSelect("Y");
        roleModule.setHasUpdate("Y");
        return roleModule;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
