package org.openelisglobal.reports.dataexport.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.service.OrganizationTypeService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.organization.valueholder.OrganizationType;
import org.openelisglobal.reports.dataexport.dao.ExportLocationDAO;
import org.openelisglobal.reports.dataexport.form.ReportingVariable;
import org.openelisglobal.requester.service.RequesterTypeService;
import org.openelisglobal.requester.service.SampleRequesterService;
import org.openelisglobal.requester.valueholder.RequesterType;
import org.openelisglobal.requester.valueholder.SampleRequester;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Location field group of Custom Data Export (OGC-1652): where a specimen
 * came from, as Organization Management records it. The requesting facility and
 * the ward or department are the order's requesters; each geographic area
 * column is the nearest parent of the facility whose organization type is that
 * level (for example Health District, then Health Region). The area columns are
 * the organization types found above facilities on this installation, so no
 * country is hard-coded and nothing needs configuring.
 */
@Service
@Transactional(readOnly = true)
public class ExportLocations {

    public static final String GROUP = "location";
    public static final String FACILITY = "referringSite";
    public static final String FACILITY_CODE = "referringSiteCode";
    public static final String WARD = "wardDept";
    public static final String AREA_PREFIX = "area:";
    private static final String FACILITY_TYPE = "referring clinic";
    private static final String WARD_TYPE = "dept";

    @Autowired
    private OrganizationTypeService organizationTypeService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private SampleRequesterService sampleRequesterService;
    @Autowired
    private RequesterTypeService requesterTypeService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private ExportLocationDAO exportLocationDAO;

    /**
     * The group's fields: facility, its code, the ward, then one column per
     * geographic level, the level nearest the facility first.
     */
    public List<ReportingVariable> catalog(List<String> layouts) {
        List<ReportingVariable> fields = new ArrayList<>();
        fields.add(new ReportingVariable(FACILITY, "Requesting Facility", "text", GROUP, false, layouts));
        fields.add(new ReportingVariable(FACILITY_CODE, "Requesting Facility Code", "text", GROUP, false, layouts));
        fields.add(new ReportingVariable(WARD, "Ward / Dept", "text", GROUP, false, layouts));
        for (OrganizationType level : levels()) {
            fields.add(
                    new ReportingVariable(AREA_PREFIX + level.getId(), level.getName(), "text", GROUP, false, layouts));
        }
        return fields;
    }

    /** Every field id of the group, in catalog order. */
    public List<String> fieldIds() {
        return catalog(List.of()).stream().map(ReportingVariable::id).toList();
    }

    private List<OrganizationType> levels() {
        Map<String, OrganizationType> types = organizationTypeService.getAllOrganizationTypes().stream()
                .collect(Collectors.toMap(OrganizationType::getId, Function.identity(), (first, second) -> first));
        return exportLocationDAO.areaTypeIdsNearestFirst(FACILITY_TYPE, List.of(FACILITY_TYPE, WARD_TYPE)).stream()
                .map(types::get).filter(Objects::nonNull).toList();
    }

    /**
     * The group's values for one sample, cached per export so the rows of one
     * sample cost one lookup.
     */
    public Map<String, String> forSample(Sample sample, Map<String, Map<String, String>> cache) {
        if (sample == null || sample.getId() == null) {
            return Map.of();
        }
        return cache.computeIfAbsent(sample.getId(), id -> resolve(sample));
    }

    /** As {@link #forSample}, for a row that names its order by lab number only. */
    public Map<String, String> forAccession(String accessionNumber, Map<String, Map<String, String>> cache) {
        if (GenericValidator.isBlankOrNull(accessionNumber)) {
            return Map.of();
        }
        return cache.computeIfAbsent("accession:" + accessionNumber,
                key -> resolve(sampleService.getSampleByAccessionNumber(accessionNumber)));
    }

    private Map<String, String> resolve(Sample sample) {
        Map<String, String> values = new LinkedHashMap<>();
        if (sample == null || sample.getId() == null) {
            return values;
        }
        RequesterType organizationRequester = requesterTypeService.getRequesterTypeByName("organization");
        Organization facility = null;
        Organization ward = null;
        for (SampleRequester requester : sampleRequesterService.getRequestersForSampleId(sample.getId())) {
            if (organizationRequester == null
                    || requester.getRequesterTypeId() != Long.parseLong(organizationRequester.getId())) {
                continue;
            }
            Organization organization = organizationService
                    .getOrganizationById(String.valueOf(requester.getRequesterId()));
            if (facility == null && hasType(organization, FACILITY_TYPE)) {
                facility = organization;
            } else if (ward == null && hasType(organization, WARD_TYPE)) {
                ward = organization;
            }
        }
        if (ward != null) {
            values.put(WARD, ward.getOrganizationName());
        }
        if (facility == null) {
            return values;
        }
        values.put(FACILITY, facility.getOrganizationName());
        values.put(FACILITY_CODE,
                GenericValidator.isBlankOrNull(facility.getCode()) ? facility.getShortName() : facility.getCode());
        Set<String> seen = new HashSet<>();
        seen.add(facility.getId());
        Organization area = facility.getOrganization();
        while (area != null && area.getId() != null && seen.add(area.getId())) {
            if (area.getOrganizationTypes() != null) {
                for (OrganizationType type : area.getOrganizationTypes()) {
                    values.putIfAbsent(AREA_PREFIX + type.getId(), area.getOrganizationName());
                }
            }
            area = area.getOrganization();
        }
        return values;
    }

    /** Order entry files the facility and the ward under these type names. */
    private static boolean hasType(Organization organization, String typeName) {
        return organization != null && organization.getOrganizationTypes() != null && organization
                .getOrganizationTypes().stream().anyMatch(type -> typeName.equalsIgnoreCase(type.getName()));
    }
}
