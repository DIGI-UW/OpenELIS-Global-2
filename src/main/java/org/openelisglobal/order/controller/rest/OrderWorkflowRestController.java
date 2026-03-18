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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
     * Get departments/wards for a site (ORD-8).
     *
     * @param siteId the parent organization/site ID
     * @return List of child organizations (departments/wards)
     */
    @GetMapping(value = "/organization/{siteId}/departments", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getDepartments(@PathVariable String siteId) {

        try {
            List<Organization> departments = organizationService.getOrganizationsByParentId(siteId);

            List<Map<String, Object>> results = new ArrayList<>();
            for (Organization dept : departments) {
                Map<String, Object> deptData = new HashMap<>();
                deptData.put("id", dept.getId());
                deptData.put("name", dept.getOrganizationName());
                deptData.put("shortName", dept.getShortName());
                deptData.put("isActive", "Y".equals(dept.getIsActive()));
                results.add(deptData);
            }

            return ResponseEntity.ok(results);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "getDepartments",
                    "Error fetching departments: " + e.getMessage());
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
     * Get program configuration with dynamic fields (ORD-10).
     *
     * <p>
     * Returns the program definition including any additional fields that should be
     * displayed when this program is selected.
     *
     * @param programId the program ID
     * @return Program configuration with dynamic fields
     */
    @GetMapping(value = "/program/{programId}/fields", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getProgramFields(@PathVariable String programId) {

        try {
            Program program = programService.get(programId);

            if (program == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("id", program.getId());
            response.put("code", program.getCode());
            response.put("name", program.getProgramName());
            response.put("isActive", true); // Programs don't have active flag, assume active

            // Define dynamic fields based on program
            // This is configurable per program - VL program has specific fields
            List<Map<String, Object>> fields = new ArrayList<>();

            String programCode = program.getCode();
            if ("VL".equalsIgnoreCase(programCode) || "Viral Load".equalsIgnoreCase(program.getProgramName())) {
                // VL Program specific fields
                fields.add(createField("arvRegimen", "order.vl.arvRegimen", "select", true, getArvRegimenOptions()));
                fields.add(createField("arvDuration", "order.vl.arvDuration", "number", false, null));
                fields.add(
                        createField("vlIndication", "order.vl.indication", "select", true, getVlIndicationOptions()));
                fields.add(createField("pregnancyStatus", "order.vl.pregnancyStatus", "select", false,
                        getPregnancyStatusOptions()));
                fields.add(createField("lastVlDate", "order.vl.lastVlDate", "date", false, null));
                fields.add(createField("lastVlResult", "order.vl.lastVlResult", "text", false, null));
            } else if ("EID".equalsIgnoreCase(programCode)) {
                // Early Infant Diagnosis fields
                fields.add(createField("infantAge", "order.eid.infantAge", "number", true, null));
                fields.add(createField("feedingStatus", "order.eid.feedingStatus", "select", true,
                        getFeedingStatusOptions()));
                fields.add(createField("motherArvStatus", "order.eid.motherArvStatus", "select", false, null));
            }
            // Add more program-specific fields as needed

            response.put("fields", fields);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "getProgramFields",
                    "Error fetching program fields: " + e.getMessage());
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

            // Get workflow type from configuration
            String workflowType = ConfigurationProperties.getInstance()
                    .getPropertyValue(Property.ACCESSION_NUMBER_PREFIX);

            // Default to "Both" if not configured
            if (GenericValidator.isBlankOrNull(workflowType)) {
                workflowType = "Both";
            }

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
     * Search ICD-10 diagnosis codes (ORD-6).
     *
     * <p>
     * Returns matching ICD-10 codes for the search query. This is a placeholder
     * implementation - should be connected to actual ICD-10 data source.
     *
     * @param search search query for ICD-10 code or description
     * @param limit  maximum results to return (default: 20)
     * @return List of matching ICD-10 codes
     */
    @GetMapping(value = "/icd10/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> searchIcd10(@RequestParam String search,
            @RequestParam(defaultValue = "20") int limit) {

        try {
            LogEvent.logInfo(this.getClass().getName(), "searchIcd10", "Searching ICD-10 codes with query: " + search);

            // TODO: Connect to actual ICD-10 data source
            // For now, return sample data for demonstration
            List<Map<String, Object>> results = new ArrayList<>();

            // Sample ICD-10 codes for demonstration
            String searchLower = search.toLowerCase();
            List<String[]> sampleCodes = List.of(new String[] { "B20", "Human immunodeficiency virus [HIV] disease" },
                    new String[] { "B20.0", "HIV disease resulting in mycobacterial infection" },
                    new String[] { "B20.1", "HIV disease resulting in other bacterial infections" },
                    new String[] { "B20.2", "HIV disease resulting in cytomegaloviral disease" },
                    new String[] { "B20.3", "HIV disease resulting in other viral infections" },
                    new String[] { "B24", "Unspecified human immunodeficiency virus [HIV] disease" },
                    new String[] { "A15", "Respiratory tuberculosis" },
                    new String[] { "A15.0", "Tuberculosis of lung" },
                    new String[] { "A15.3", "Tuberculosis of intrathoracic lymph nodes" },
                    new String[] { "J18.9", "Pneumonia, unspecified organism" },
                    new String[] { "D50.9", "Iron deficiency anaemia, unspecified" },
                    new String[] { "E11.9", "Type 2 diabetes mellitus without complications" },
                    new String[] { "I10", "Essential (primary) hypertension" },
                    new String[] { "Z21", "Asymptomatic human immunodeficiency virus [HIV] infection status" });

            for (String[] code : sampleCodes) {
                if (code[0].toLowerCase().contains(searchLower) || code[1].toLowerCase().contains(searchLower)) {
                    Map<String, Object> icdData = new HashMap<>();
                    icdData.put("code", code[0]);
                    icdData.put("description", code[1]);
                    icdData.put("displayName", code[0] + " - " + code[1]);
                    results.add(icdData);

                    if (results.size() >= limit) {
                        break;
                    }
                }
            }

            return ResponseEntity.ok(results);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "searchIcd10",
                    "Error searching ICD-10 codes: " + e.getMessage());
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

    private Map<String, Object> createField(String name, String labelKey, String type, boolean required,
            List<Map<String, String>> options) {
        Map<String, Object> field = new HashMap<>();
        field.put("name", name);
        field.put("labelKey", labelKey);
        field.put("type", type);
        field.put("required", required);
        if (options != null) {
            field.put("options", options);
        }
        return field;
    }

    private List<Map<String, String>> getArvRegimenOptions() {
        List<Map<String, String>> options = new ArrayList<>();
        options.add(Map.of("value", "TDF/3TC/DTG", "label", "TDF/3TC/DTG"));
        options.add(Map.of("value", "TDF/3TC/EFV", "label", "TDF/3TC/EFV"));
        options.add(Map.of("value", "AZT/3TC/NVP", "label", "AZT/3TC/NVP"));
        options.add(Map.of("value", "AZT/3TC/EFV", "label", "AZT/3TC/EFV"));
        options.add(Map.of("value", "ABC/3TC/DTG", "label", "ABC/3TC/DTG"));
        options.add(Map.of("value", "other", "label", "Other"));
        return options;
    }

    private List<Map<String, String>> getVlIndicationOptions() {
        List<Map<String, String>> options = new ArrayList<>();
        options.add(Map.of("value", "routine", "label", "Routine Monitoring"));
        options.add(Map.of("value", "suspected_failure", "label", "Suspected Treatment Failure"));
        options.add(Map.of("value", "confirm_failure", "label", "Confirm Treatment Failure"));
        options.add(Map.of("value", "baseline", "label", "Baseline"));
        options.add(Map.of("value", "pregnancy", "label", "Pregnancy"));
        options.add(Map.of("value", "breastfeeding", "label", "Breastfeeding"));
        return options;
    }

    private List<Map<String, String>> getPregnancyStatusOptions() {
        List<Map<String, String>> options = new ArrayList<>();
        options.add(Map.of("value", "not_pregnant", "label", "Not Pregnant"));
        options.add(Map.of("value", "pregnant", "label", "Pregnant"));
        options.add(Map.of("value", "breastfeeding", "label", "Breastfeeding"));
        options.add(Map.of("value", "unknown", "label", "Unknown"));
        options.add(Map.of("value", "not_applicable", "label", "Not Applicable"));
        return options;
    }

    private List<Map<String, String>> getFeedingStatusOptions() {
        List<Map<String, String>> options = new ArrayList<>();
        options.add(Map.of("value", "exclusive_breastfeeding", "label", "Exclusive Breastfeeding"));
        options.add(Map.of("value", "mixed_feeding", "label", "Mixed Feeding"));
        options.add(Map.of("value", "formula_feeding", "label", "Formula Feeding"));
        options.add(Map.of("value", "weaned", "label", "Weaned"));
        return options;
    }
}
