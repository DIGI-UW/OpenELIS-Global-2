package org.openelisglobal.address.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.address.service.AddressHierarchyConfigurationHandler;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.service.OrganizationTypeService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.organization.valueholder.OrganizationType;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for address hierarchy navigation. Provides endpoints for
 * retrieving hierarchy levels and their values.
 */
@RestController
@RequestMapping(value = "/rest/address-hierarchy")
public class AddressHierarchyRestController {

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private OrganizationTypeService organizationTypeService;

    @Autowired
    private SiteInformationService siteInformationService;

    /**
     * Get all configured address hierarchy levels.
     *
     * @return List of hierarchy levels with their type names and default values
     */
    @GetMapping(value = "/levels", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AddressHierarchyLevel> getLevels() {
        List<AddressHierarchyLevel> levels = new ArrayList<>();
        List<OrganizationType> allTypes = organizationTypeService.getAllOrganizationTypes();

        for (OrganizationType orgType : allTypes) {
            int level = AddressHierarchyConfigurationHandler.getHierarchyLevel(orgType);
            if (level > 0) {
                String defaultValue = getDefaultValueForLevel(level);
                String defaultId = resolveDefaultValueToId(defaultValue, orgType.getName());
                levels.add(
                        new AddressHierarchyLevel(level, orgType.getId(), orgType.getName(), defaultValue, defaultId));
            }
        }

        // Sort by level number
        levels.sort(Comparator.comparingInt(AddressHierarchyLevel::getLevel));

        // If no hierarchy levels configured, return legacy Health Region/Health
        // District
        if (levels.isEmpty()) {
            OrganizationType healthRegion = organizationTypeService.getOrganizationTypeByName("Health Region");
            OrganizationType healthDistrict = organizationTypeService.getOrganizationTypeByName("Health District");

            if (healthRegion != null) {
                String defaultValue = getDefaultValueForLevel(1);
                String defaultId = resolveDefaultValueToId(defaultValue, healthRegion.getName());
                levels.add(new AddressHierarchyLevel(1, healthRegion.getId(), healthRegion.getName(), defaultValue,
                        defaultId));
            }
            if (healthDistrict != null) {
                String defaultValue = getDefaultValueForLevel(2);
                String defaultId = resolveDefaultValueToId(defaultValue, healthDistrict.getName());
                levels.add(new AddressHierarchyLevel(2, healthDistrict.getId(), healthDistrict.getName(), defaultValue,
                        defaultId));
            }
        }

        return levels;
    }

    private String getDefaultValueForLevel(int level) {
        String siteInfoName = AddressHierarchyConfigurationHandler.getDefaultValueSiteInfoName(level);
        SiteInformation siteInfo = siteInformationService.getSiteInformationByName(siteInfoName);
        String value = siteInfo != null ? siteInfo.getValue() : null;
        System.out.println(
                "DEBUG getDefaultValueForLevel: level=" + level + " siteInfoName=" + siteInfoName + " value=" + value);
        return value;
    }

    private String resolveDefaultValueToId(String defaultValue, String typeName) {
        if (GenericValidator.isBlankOrNull(defaultValue) || GenericValidator.isBlankOrNull(typeName)) {
            System.out.println("DEBUG resolveDefaultValueToId: defaultValue or typeName is blank, returning null");
            return null;
        }
        // Find the organization with this name and type
        List<Organization> orgs = organizationService.getOrganizationsByTypeName("organizationName", typeName);
        System.out.println("DEBUG resolveDefaultValueToId: looking for '" + defaultValue + "' in " + orgs.size()
                + " orgs of type '" + typeName + "'");
        for (Organization org : orgs) {
            if (defaultValue.equals(org.getOrganizationName())) {
                System.out.println("DEBUG resolveDefaultValueToId: FOUND match, id=" + org.getId());
                return org.getId();
            }
        }
        System.out.println("DEBUG resolveDefaultValueToId: NO match found for '" + defaultValue + "'");
        return null;
    }

    /**
     * Get all values at a specific hierarchy level.
     *
     * @param levelNumber The hierarchy level number (1 = top level)
     * @return List of locations at that level
     */
    @GetMapping(value = "/level/{levelNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<IdValuePair> getValuesAtLevel(@PathVariable int levelNumber) {
        String typeName = getTypeNameForLevel(levelNumber);
        if (typeName == null) {
            return Collections.emptyList();
        }

        List<Organization> orgs = organizationService.getOrganizationsByTypeName("organizationName", typeName);
        return orgs.stream().sorted(Comparator.comparing(Organization::getOrganizationName))
                .map(org -> new IdValuePair(org.getId(), org.getOrganizationName())).collect(Collectors.toList());
    }

    /**
     * Get children of a specific location by parent ID. This endpoint is backward
     * compatible with the existing health-districts-for-region endpoint.
     *
     * @param parentId The parent organization ID
     * @return List of child locations
     */
    @GetMapping(value = "/children", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<IdValuePair> getChildren(HttpServletRequest request, @RequestParam(required = false) String parentId,
            @RequestParam(required = false) String parentCode) {

        Organization parent = null;

        if (!GenericValidator.isBlankOrNull(parentId)) {
            parent = organizationService.getOrganizationById(parentId);
        } else if (!GenericValidator.isBlankOrNull(parentCode)) {
            parent = organizationService.getOrganizationByCode(parentCode);
        }

        if (parent == null) {
            return Collections.emptyList();
        }

        List<Organization> children = organizationService.getOrganizationsByParentId(parent.getId());
        return children.stream().sorted(Comparator.comparing(Organization::getOrganizationName))
                .map(org -> new IdValuePair(org.getId(), org.getOrganizationName())).collect(Collectors.toList());
    }

    /**
     * Get the full hierarchy path for a location (from top level down to the
     * specified location).
     *
     * @param organizationId The organization ID
     * @return List of locations from top level to the specified location
     */
    @GetMapping(value = "/path/{organizationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<IdValuePair> getHierarchyPath(@PathVariable String organizationId) {
        List<IdValuePair> path = new ArrayList<>();

        Organization current = organizationService.getOrganizationById(organizationId);
        while (current != null) {
            path.add(0, new IdValuePair(current.getId(), current.getOrganizationName()));
            current = current.getOrganization(); // Get parent
        }

        return path;
    }

    /**
     * Search for locations by name across all levels.
     *
     * @param query The search query
     * @return List of matching locations
     */
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AddressSearchResult> search(@RequestParam String query) {
        if (GenericValidator.isBlankOrNull(query) || query.length() < 2) {
            return Collections.emptyList();
        }

        List<AddressSearchResult> results = new ArrayList<>();
        List<Organization> orgs = organizationService.getOrganizations(query);

        for (Organization org : orgs) {
            // Check if this organization is part of an address hierarchy
            if (isAddressHierarchyOrganization(org)) {
                AddressSearchResult result = new AddressSearchResult();
                result.setId(org.getId());
                result.setCode(org.getCode());
                result.setName(org.getOrganizationName());
                result.setFullPath(buildFullPath(org));
                results.add(result);
            }
        }

        return results.stream().sorted(Comparator.comparing(AddressSearchResult::getName)).limit(50)
                .collect(Collectors.toList());
    }

    private String getTypeNameForLevel(int levelNumber) {
        List<OrganizationType> allTypes = organizationTypeService.getAllOrganizationTypes();

        for (OrganizationType orgType : allTypes) {
            int level = AddressHierarchyConfigurationHandler.getHierarchyLevel(orgType);
            if (level == levelNumber) {
                return orgType.getName();
            }
        }

        // Fall back to legacy types
        if (levelNumber == 1) {
            OrganizationType healthRegion = organizationTypeService.getOrganizationTypeByName("Health Region");
            return healthRegion != null ? healthRegion.getName() : null;
        } else if (levelNumber == 2) {
            OrganizationType healthDistrict = organizationTypeService.getOrganizationTypeByName("Health District");
            return healthDistrict != null ? healthDistrict.getName() : null;
        }

        return null;
    }

    private boolean isAddressHierarchyOrganization(Organization org) {
        if (org.getOrganizationTypes() == null) {
            return false;
        }

        for (OrganizationType type : org.getOrganizationTypes()) {
            if (AddressHierarchyConfigurationHandler.isAddressHierarchyType(type)) {
                return true;
            }
            // Also check legacy types
            if ("Health Region".equals(type.getName()) || "Health District".equals(type.getName())) {
                return true;
            }
        }
        return false;
    }

    private String buildFullPath(Organization org) {
        List<String> pathParts = new ArrayList<>();
        Organization current = org;
        while (current != null) {
            pathParts.add(0, current.getOrganizationName());
            current = current.getOrganization();
        }
        return String.join(" > ", pathParts);
    }

    /**
     * DTO for address hierarchy level information.
     */
    public static class AddressHierarchyLevel {
        private int level;
        private String typeId;
        private String typeName;
        private String defaultValue;
        private String defaultId;

        public AddressHierarchyLevel(int level, String typeId, String typeName) {
            this(level, typeId, typeName, null, null);
        }

        public AddressHierarchyLevel(int level, String typeId, String typeName, String defaultValue, String defaultId) {
            this.level = level;
            this.typeId = typeId;
            this.typeName = typeName;
            this.defaultValue = defaultValue;
            this.defaultId = defaultId;
        }

        public int getLevel() {
            return level;
        }

        public void setLevel(int level) {
            this.level = level;
        }

        public String getTypeId() {
            return typeId;
        }

        public void setTypeId(String typeId) {
            this.typeId = typeId;
        }

        public String getTypeName() {
            return typeName;
        }

        public void setTypeName(String typeName) {
            this.typeName = typeName;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public String getDefaultId() {
            return defaultId;
        }

        public void setDefaultId(String defaultId) {
            this.defaultId = defaultId;
        }
    }

    /**
     * DTO for address search results.
     */
    public static class AddressSearchResult {
        private String id;
        private String code;
        private String name;
        private String fullPath;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getFullPath() {
            return fullPath;
        }

        public void setFullPath(String fullPath) {
            this.fullPath = fullPath;
        }
    }
}
