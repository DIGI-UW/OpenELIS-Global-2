/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 */
package org.openelisglobal.order.controller.rest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.service.OrganizationTypeService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.organization.valueholder.OrganizationType;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.program.service.ProgramService;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Order Workflow APIs.
 *
 * <p>
 * Provides endpoints for: - Organization/Site search (ORD-8) - Provider search
 * (ORD-8a) - Program configuration with dynamic fields (ORD-10) - Lab unit
 * configuration - ICD-10 diagnosis search (ORD-6)
 *
 * @see Organization
 * @see Provider
 * @see Program
 */
@RestController
@RequestMapping("/rest")
public class OrderWorkflowRestController extends BaseRestController {

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private OrganizationTypeService organizationTypeService;

    @Autowired
    private ProviderService providerService;

    @Autowired
    private PersonService personService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private TestService testService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private TypeOfSampleTestService typeOfSampleTestService;

    @Autowired
    private UnitOfMeasureService unitOfMeasureService;

    /**
     * Search organizations/sites by name or code (ORD-8).
     *
     * <p>
     * Returns paginated list of organizations matching the search query. Supports
     * filtering by organization type (e.g., "referring clinic").
     *
     * @param search   search query for organization name or short name
     * @param type     organization type filter (optional)
     * @param page     page number (1-based, default: 1)
     * @param pageSize items per page (default: 20)
     * @return List of matching organizations
     */
    @GetMapping(value = "/organization/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> searchOrganizations(@RequestParam(required = false) String search,
            @RequestParam(required = false) String type, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        try {
            LogEvent.logInfo(this.getClass().getName(), "searchOrganizations",
                    "Searching organizations with query: " + search);

            List<Organization> organizations;
            int totalCount;

            if (!GenericValidator.isBlankOrNull(search)) {
                // Search by name/filter
                organizations = organizationService.searchOrganizationsWithTypes(search);
                LogEvent.logInfo(this.getClass().getName(), "searchOrganizations", "Found " + organizations.size()
                        + " organizations matching '" + search + "' before type filter");

                // Filter by type - default to referring clinic and patient referral types for
                // site search
                if (!GenericValidator.isBlankOrNull(type)) {
                    organizations = filterByType(organizations, type);
                } else {
                    // Default filter: only show organizations that are "referring clinic" or
                    // "patient referral"
                    organizations = filterByReferringSiteTypes(organizations);
                }

                LogEvent.logInfo(this.getClass().getName(), "searchOrganizations",
                        "After type filter: " + organizations.size() + " organizations");

                totalCount = organizations.size();

                // Apply pagination
                int startIndex = (page - 1) * pageSize;
                int endIndex = Math.min(startIndex + pageSize, organizations.size());
                if (startIndex < organizations.size()) {
                    organizations = organizations.subList(startIndex, endIndex);
                } else {
                    organizations = new ArrayList<>();
                }
            } else {
                // Get all organizations with pagination - also filter by referring site types
                int startRecNo = (page - 1) * pageSize;
                organizations = organizationService.getPageOfOrganizations(startRecNo);
                organizations = filterByReferringSiteTypes(organizations);
                totalCount = organizations.size();
            }

            // Convert to response format
            List<Map<String, Object>> results = new ArrayList<>();
            for (Organization org : organizations) {
                Map<String, Object> orgData = new HashMap<>();
                orgData.put("id", org.getId());
                orgData.put("organizationName", org.getOrganizationName());
                orgData.put("name", org.getOrganizationName()); // alias for compatibility
                orgData.put("shortName", org.getShortName());
                orgData.put("code", org.getOrganizationLocalAbbreviation());
                orgData.put("streetAddress", org.getStreetAddress());
                orgData.put("city", org.getCity() != null ? org.getCity() : "");
                orgData.put("state", org.getState());
                orgData.put("isActive", "Y".equals(org.getIsActive()));

                // Get organization type name for display
                List<String> typeIds = organizationService.getTypeIdsForOrganizationId(org.getId());
                String orgTypeName = "";
                if (!typeIds.isEmpty()) {
                    OrganizationType orgType = organizationTypeService.get(typeIds.get(0));
                    if (orgType != null && orgType.getName() != null) {
                        orgTypeName = orgType.getName();
                    }
                }
                orgData.put("organizationType", orgTypeName);

                // Include parent organization if exists
                if (org.getOrganization() != null) {
                    Map<String, Object> parentData = new HashMap<>();
                    parentData.put("id", org.getOrganization().getId());
                    parentData.put("name", org.getOrganization().getOrganizationName());
                    orgData.put("parent", parentData);
                }

                results.add(orgData);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("organizations", results);
            response.put("totalCount", totalCount);
            response.put("page", page);
            response.put("pageSize", pageSize);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "searchOrganizations",
                    "Error searching organizations: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Search providers/practitioners (ORD-8a).
     *
     * <p>
     * Returns paginated list of providers matching the search query. Supports
     * searching by name or phone number.
     *
     * @param search   search query for provider name
     * @param phone    search query for provider phone number
     * @param page     page number (1-based, default: 1)
     * @param pageSize items per page (default: 20)
     * @return List of matching providers with name, phone, and fax
     */
    @GetMapping(value = "/provider/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> searchProviders(@RequestParam(required = false) String search,
            @RequestParam(required = false) String phone, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        try {
            LogEvent.logInfo(this.getClass().getName(), "searchProviders",
                    "Searching providers with name: " + search + ", phone: " + phone);

            List<Provider> providers;
            int totalCount;
            // DAO expects 1-based starting record number, not 0-based
            int startRecNo = ((page - 1) * pageSize) + 1;

            if (!GenericValidator.isBlankOrNull(phone)) {
                // Search by phone number
                providers = providerService.getPagesOfSearchedProvidersByPhone(startRecNo, phone);
                totalCount = providerService.getTotalSearchedProviderCountByPhone(phone);
                LogEvent.logInfo(this.getClass().getName(), "searchProviders",
                        "Found " + providers.size() + " providers matching phone '" + phone + "'");
            } else if (!GenericValidator.isBlankOrNull(search)) {
                // Search by name
                providers = providerService.getPagesOfSearchedProviders(startRecNo, search);
                totalCount = providerService.getTotalSearchedProviderCount(search);
                LogEvent.logInfo(this.getClass().getName(), "searchProviders",
                        "Found " + providers.size() + " providers matching name '" + search + "'");
            } else {
                // No search query - return all providers
                providers = providerService.getPageOfProviders(startRecNo);
                totalCount = (int) providerService.getCount();
                LogEvent.logInfo(this.getClass().getName(), "searchProviders",
                        "Retrieved " + providers.size() + " providers (no search query)");
            }

            LogEvent.logInfo(this.getClass().getName(), "searchProviders", "Total provider count: " + totalCount);

            // Convert to response format - include name, phone, and fax like Provider
            // Management
            List<Map<String, Object>> results = new ArrayList<>();
            for (Provider provider : providers) {
                Map<String, Object> providerData = new HashMap<>();
                providerData.put("id", provider.getId());

                // Get person details
                if (provider.getPerson() != null) {
                    personService.getData(provider.getPerson());
                    // Include personId so frontend can use existing provider without creating new
                    // one
                    providerData.put("personId", provider.getPerson().getId());
                    providerData.put("firstName", provider.getPerson().getFirstName());
                    providerData.put("lastName", provider.getPerson().getLastName());
                    // Combined name for display
                    String fullName = "";
                    if (provider.getPerson().getLastName() != null) {
                        fullName = provider.getPerson().getLastName();
                    }
                    if (provider.getPerson().getFirstName() != null) {
                        if (!fullName.isEmpty()) {
                            fullName += ", ";
                        }
                        fullName += provider.getPerson().getFirstName();
                    }
                    providerData.put("name", fullName);
                    // Use primaryPhone or fall back to workPhone
                    String providerPhone = provider.getPerson().getPrimaryPhone();
                    if (GenericValidator.isBlankOrNull(providerPhone)) {
                        providerPhone = provider.getPerson().getWorkPhone();
                    }
                    providerData.put("phone", providerPhone);
                    providerData.put("fax", provider.getPerson().getFax());
                    providerData.put("email", provider.getPerson().getEmail());
                }

                providerData.put("externalId", provider.getExternalId());
                providerData.put("isActive", "Y".equals(provider.getActive()));

                results.add(providerData);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("providers", results);
            response.put("totalCount", totalCount);
            response.put("page", page);
            response.put("pageSize", pageSize);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "searchProviders",
                    "Error searching providers: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get lab unit configuration.
     *
     * <p>
     * Returns configuration settings for the lab unit including workflow type
     * (Clinical, Environmental, or Both).
     *
     * @return Lab unit configuration
     */
    @GetMapping(value = "/labUnit/config", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getLabUnitConfig() {

        try {
            Map<String, Object> config = new HashMap<>();

            // Workflow type: "PrePrinted", "OnDemand", or "Both"
            // TODO: Add proper configuration property for workflow type
            String workflowType = "Both";
            config.put("workflowType", workflowType);

            // Get other relevant configuration
            config.put("labName", ConfigurationProperties.getInstance().getPropertyValue(Property.SiteName));
            config.put("useAccessionNumberValidation", ConfigurationProperties.getInstance()
                    .isPropertyValueEqual(Property.ACCESSION_NUMBER_VALIDATE, "true"));
            config.put("accessionFormat",
                    ConfigurationProperties.getInstance().getPropertyValue(Property.AccessionFormat));

            return ResponseEntity.ok(config);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "getLabUnitConfig",
                    "Error fetching lab unit config: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get compatible sample types for given test IDs.
     *
     * <p>
     * Returns which sample types can be used for each test. This is used in Step 2
     * (Collect Sample) to show which sample types are compatible with ordered
     * tests.
     *
     * @param testIds comma-separated list of test IDs
     * @return List of tests with their compatible sample types
     */
    @GetMapping(value = "/test-sample-types", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getTestSampleTypes(@RequestParam String testIds) {

        try {
            List<Map<String, Object>> testsResult = new ArrayList<>();

            if (GenericValidator.isBlankOrNull(testIds)) {
                Map<String, Object> response = new HashMap<>();
                response.put("tests", testsResult);
                return ResponseEntity.ok(response);
            }

            String[] ids = testIds.split(",");
            for (String testId : ids) {
                String trimmedId = testId.trim();
                if (trimmedId.isEmpty()) {
                    continue;
                }

                Test test = testService.get(trimmedId);
                if (test == null) {
                    continue;
                }

                Map<String, Object> testData = new HashMap<>();
                testData.put("testId", test.getId());
                testData.put("testName",
                        test.getLocalizedTestName() != null ? test.getLocalizedTestName().getLocalizedValue()
                                : test.getName());

                // Get compatible sample types for this test
                List<TypeOfSampleTest> sampleTests = typeOfSampleTestService.getTypeOfSampleTestsForTest(trimmedId);
                List<Map<String, Object>> compatibleTypes = new ArrayList<>();

                for (TypeOfSampleTest sampleTest : sampleTests) {
                    TypeOfSample sampleType = typeOfSampleService.get(sampleTest.getTypeOfSampleId());
                    if (sampleType != null && "Y".equals(sampleType.getIsActive())) {
                        Map<String, Object> typeData = new HashMap<>();
                        typeData.put("id", sampleType.getId());
                        typeData.put("name", sampleType.getLocalizedName() != null ? sampleType.getLocalizedName()
                                : sampleType.getDescription());
                        typeData.put("code", "all"); // Default code
                        compatibleTypes.add(typeData);
                    }
                }

                testData.put("compatibleSampleTypes", compatibleTypes);
                testsResult.add(testData);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("tests", testsResult);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "getTestSampleTypes",
                    "Error getting test sample types: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get current server time.
     *
     * <p>
     * Returns the current date and time from the server. Used for auto-populating
     * "Received at Lab" fields in Step 2 (Collect Sample).
     *
     * @return Current server date, time, and timezone
     */
    @GetMapping(value = "/server-time", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getServerTime() {

        try {
            Map<String, Object> response = new HashMap<>();

            ZoneId zoneId = ZoneId.systemDefault();
            LocalDate now = LocalDate.now(zoneId);
            LocalTime time = LocalTime.now(zoneId);

            // Format date as YYYY-MM-DD (ISO format)
            response.put("date", now.format(DateTimeFormatter.ISO_LOCAL_DATE));

            // Format time as HH:mm
            response.put("time", time.format(DateTimeFormatter.ofPattern("HH:mm")));

            // Include timezone for reference
            response.put("timezone", zoneId.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "getServerTime",
                    "Error getting server time: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // Helper methods

    private List<Organization> filterByType(List<Organization> organizations, String typeName) {
        List<Organization> filtered = new ArrayList<>();
        OrganizationType targetType = organizationTypeService.getOrganizationTypeByName(typeName);

        if (targetType == null) {
            return organizations;
        }

        for (Organization org : organizations) {
            List<String> typeIds = organizationService.getTypeIdsForOrganizationId(org.getId());
            if (typeIds.contains(targetType.getId())) {
                filtered.add(org);
            }
        }

        return filtered;
    }

    /**
     * Filter organizations to only include those with "referring clinic" or
     * "patient referral" types. These are the organization types appropriate for
     * site/facility selection in the order workflow.
     *
     * This method strictly filters organizations - if no organizations match the
     * filter, an empty list is returned. This ensures only valid referring sites
     * are shown in the order workflow.
     */
    private List<Organization> filterByReferringSiteTypes(List<Organization> organizations) {
        List<Organization> filtered = new ArrayList<>();

        // Get the organization type IDs for referring clinic and patient referral
        OrganizationType referringClinicType = organizationTypeService.getOrganizationTypeByName("referring clinic");
        OrganizationType patientReferralType = organizationTypeService.getOrganizationTypeByName("patient referral");

        LogEvent.logInfo(this.getClass().getName(), "filterByReferringSiteTypes",
                "Looking for org types - 'referring clinic': "
                        + (referringClinicType != null ? referringClinicType.getId() : "NOT FOUND")
                        + ", 'patient referral': "
                        + (patientReferralType != null ? patientReferralType.getId() : "NOT FOUND"));

        Set<String> validTypeIds = new HashSet<>();
        if (referringClinicType != null) {
            validTypeIds.add(referringClinicType.getId());
        }
        if (patientReferralType != null) {
            validTypeIds.add(patientReferralType.getId());
        }

        // If no valid types found in DB, return empty list (strict filtering)
        if (validTypeIds.isEmpty()) {
            LogEvent.logWarn(this.getClass().getName(), "filterByReferringSiteTypes",
                    "No 'referring clinic' or 'patient referral' organization types found in database - returning empty list");
            return filtered;
        }

        for (Organization org : organizations) {
            List<String> typeIds = organizationService.getTypeIdsForOrganizationId(org.getId());
            // Check if org has any of the valid types
            for (String typeId : typeIds) {
                if (validTypeIds.contains(typeId)) {
                    filtered.add(org);
                    break;
                }
            }
        }

        LogEvent.logInfo(this.getClass().getName(), "filterByReferringSiteTypes",
                "Filtered from " + organizations.size() + " to " + filtered.size() + " organizations with valid types");

        return filtered;
    }

    /**
     * Get units of measure by type.
     *
     * <p>
     * Returns list of UOMs filtered by type. Used for sample collection (type =
     * SAMPLE_COLLECTION) or test results (type = RESULT).
     *
     * @param type the UOM type (SAMPLE_COLLECTION, RESULT, or omit for all)
     * @return List of UOMs with id and value (name)
     */
    @GetMapping(value = "/uom", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, String>>> getUnitOfMeasuresByType(
            @RequestParam(required = false) String type) {
        try {
            List<UnitOfMeasure> uoms;

            if (type != null && !type.trim().isEmpty()) {
                uoms = unitOfMeasureService.getUnitOfMeasuresByType(type);
            } else {
                uoms = unitOfMeasureService.getAll();
            }

            List<Map<String, String>> result = new ArrayList<>();
            for (UnitOfMeasure uom : uoms) {
                Map<String, String> uomData = new HashMap<>();
                uomData.put("id", uom.getId());
                uomData.put("value", uom.getUnitOfMeasureName());
                result.add(uomData);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "getUnitOfMeasuresByType",
                    "Error fetching UOMs: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
