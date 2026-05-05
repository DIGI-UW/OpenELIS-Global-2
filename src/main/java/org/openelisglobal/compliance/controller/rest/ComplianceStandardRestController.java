package org.openelisglobal.compliance.controller.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.compliance.service.ComplianceStandardService;
import org.openelisglobal.compliance.service.ComplianceThresholdService;
import org.openelisglobal.compliance.service.ParameterGroupService;
import org.openelisglobal.compliance.valueholder.ComplianceStandard;
import org.openelisglobal.compliance.valueholder.ComplianceStandard.ComplianceStandardStatus;
import org.openelisglobal.compliance.valueholder.ParameterGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Permission policy (FRS S-01 Section 11): - View endpoints: any authenticated
 * lab role (GLOBAL_ADMIN, RECEPTION, RESULTS) - covers Lab Technician + Lab
 * Manager view-only access. - Mutation endpoints
 * (create/update/archive/copy/import): GLOBAL_ADMIN only - covers System
 * Administrator full access. The FRS-level permission keys
 * (compliance.standard.view, ...modify, etc.) map onto these existing OpenELIS
 * roles; method-level overrides below tighten write paths.
 */
@RestController
@RequestMapping("/rest/compliance/standards")
@PreAuthorize("hasAnyRole('GLOBAL_ADMIN', 'RECEPTION', 'RESULTS')")
public class ComplianceStandardRestController extends BaseRestController {

    @Autowired
    private ComplianceStandardService complianceStandardService;

    @Autowired
    private ParameterGroupService parameterGroupService;

    @Autowired
    private ComplianceThresholdService complianceThresholdService;

    @GetMapping
    public ResponseEntity<List<ComplianceStandardListItem>> getAllStandards(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        try {
            page = Math.max(0, page);
            size = Math.min(Math.max(1, size), 1000);

            long startingRecNoLong = ((long) page * size) + 1;
            int startingRecNo = startingRecNoLong > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) startingRecNoLong;

            List<ComplianceStandard> standards = complianceStandardService.getPageOfStandards(startingRecNo);
            return ResponseEntity.ok(toListItems(standards));
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/active")
    public ResponseEntity<List<ComplianceStandard>> getActiveStandards() {
        try {
            List<ComplianceStandard> activeStandards = complianceStandardService.getActiveComplianceStandards();
            return ResponseEntity.ok(activeStandards);
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<ComplianceStandard>> searchStandards(@RequestParam(required = false) String name,
            @RequestParam(required = false) String issuingBody, @RequestParam(required = false) String regulationNumber,
            @RequestParam(required = false) ComplianceStandardStatus status,
            @RequestParam(required = false) String countryRegion, @RequestParam(required = false) String sampleType) {
        try {
            List<ComplianceStandard> results = complianceStandardService.searchStandards(name, issuingBody,
                    regulationNumber, status, countryRegion, sampleType);
            return ResponseEntity.ok(results);
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getStandard(@PathVariable String id) {
        try {
            ComplianceStandard standard = complianceStandardService.get(id);
            if (standard != null) {
                return ResponseEntity.ok(standard);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (org.hibernate.ObjectNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<ComplianceStandard> createStandard(@Valid @RequestBody ComplianceStandard standard,
            HttpServletRequest request) {
        try {
            String sysUserId = ControllerUtills.getSysUserId(request);
            if (sysUserId != null && !sysUserId.trim().isEmpty()) {
                standard.setSysUserId(sysUserId);
            }
            ComplianceStandard saved = complianceStandardService.save(standard);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (jakarta.persistence.PersistenceException | LIMSRuntimeException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<ComplianceStandard> updateStandard(@PathVariable String id,
            @Valid @RequestBody ComplianceStandard standard, HttpServletRequest request) {
        try {
            ComplianceStandard existing = complianceStandardService.get(id);
            if (existing == null) {
                return ResponseEntity.notFound().build();
            }

            existing.setName(standard.getName());
            existing.setIssuingBody(standard.getIssuingBody());
            existing.setRegulationNumber(standard.getRegulationNumber());
            existing.setVersion(standard.getVersion());
            existing.setEffectiveDate(standard.getEffectiveDate());
            existing.setExpiryDate(standard.getExpiryDate());
            existing.setCountryRegion(standard.getCountryRegion());
            existing.setApplicableSampleTypes(standard.getApplicableSampleTypes());
            existing.setStatus(standard.getStatus());
            existing.setSupersededByStandard(standard.getSupersededByStandard());
            existing.setDescription(standard.getDescription());
            existing.setRegulatoryContext(standard.getRegulatoryContext());
            existing.setEnforcementAuthority(standard.getEnforcementAuthority());
            existing.setIsPreSeeded(standard.getIsPreSeeded());

            String sysUserId = ControllerUtills.getSysUserId(request);
            if (sysUserId != null && !sysUserId.trim().isEmpty()) {
                existing.setSysUserId(sysUserId);
            }

            ComplianceStandard updated = complianceStandardService.update(existing);
            return ResponseEntity.ok(updated);
        } catch (org.hibernate.ObjectNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (jakarta.persistence.OptimisticLockException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<Void> deleteStandard(@PathVariable String id) {
        try {
            ComplianceStandard standard = complianceStandardService.get(id);
            if (standard == null) {
                return ResponseEntity.notFound().build();
            }
            // FR-6-004 / BR-007: pre-seeded standards cannot be deleted, only
            // archived. Pre-check here so the API returns 403 (the FRS-required
            // status code) rather than the generic 400 the service-level
            // LIMSRuntimeException catch would yield.
            if (Boolean.TRUE.equals(standard.getIsPreSeeded())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            complianceStandardService.delete(standard);
            return ResponseEntity.noContent().build();
        } catch (org.hibernate.ObjectNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (jakarta.persistence.OptimisticLockException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * FR-1-006 / BR-002: archive (soft-delete) a standard. Sets status to ARCHIVED
     * so previously evaluated results keep their lineage but the standard
     * disappears from active selection (registration dropdowns, Link Test
     * typeahead) — the spec calls this out as the standard destructive-action
     * surface; hard delete via DELETE remains for pre-seeded-free, threshold-free,
     * evaluation-free rows only.
     */
    @PostMapping("/{id}/archive")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<ComplianceStandard> archiveStandard(@PathVariable String id) {
        try {
            ComplianceStandard standard = complianceStandardService.get(id);
            if (standard == null) {
                return ResponseEntity.notFound().build();
            }
            complianceStandardService.archive(id);
            return ResponseEntity.ok(complianceStandardService.get(id));
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/{id}/parameter-groups")
    public ResponseEntity<List<ParameterGroupListItem>> getParameterGroups(@PathVariable String id) {
        try {
            List<ParameterGroup> groups = parameterGroupService.getGroupsByStandardId(id);
            // Project to a slim DTO so we don't drag the entity's lazy
            // complianceThresholds collection / standard back-ref through
            // Jackson — that path was throwing HttpMessageNotWritableException.
            List<ParameterGroupListItem> items = new java.util.ArrayList<>(groups != null ? groups.size() : 0);
            if (groups != null) {
                for (ParameterGroup g : groups) {
                    items.add(new ParameterGroupListItem(g));
                }
            }
            return ResponseEntity.ok(items);
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/parameter-groups")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<ParameterGroup> createParameterGroup(@PathVariable String id,
            @RequestBody ParameterGroup group, HttpServletRequest request) {
        // No @Valid here on purpose: ParameterGroup.standard carries
        // @NotNull, but the JSON payload omits it (it's wired via the path
        // variable below) so request-binding validation would always fail
        // before our setStandard(...) call ever runs. The service layer
        // re-runs bean validation at persist time, after the standard ref
        // is in place.
        try {
            ComplianceStandard standard = complianceStandardService.get(id);
            if (standard == null) {
                return ResponseEntity.notFound().build();
            }
            group.setStandard(standard);

            // Only set sysUserId from request if we have a valid value
            String sysUserId = ControllerUtills.getSysUserId(request);
            if (sysUserId != null && !sysUserId.trim().isEmpty()) {
                group.setSysUserId(sysUserId);
            }

            ParameterGroup saved = parameterGroupService.save(group);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{id}/parameter-groups/{groupId}")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<ParameterGroup> updateParameterGroup(@PathVariable String id, @PathVariable String groupId,
            @Valid @RequestBody ParameterGroup group, HttpServletRequest request) {
        try {
            ParameterGroup existing = parameterGroupService.get(groupId);
            if (existing == null) {
                return ResponseEntity.notFound().build();
            }

            existing.setName(group.getName());
            existing.setDescription(group.getDescription());
            existing.setSortOrder(group.getSortOrder());
            existing.setIsMandatory(group.getIsMandatory());

            String sysUserId = ControllerUtills.getSysUserId(request);
            if (sysUserId != null && !sysUserId.trim().isEmpty()) {
                existing.setSysUserId(sysUserId);
            }

            ParameterGroup updated = parameterGroupService.update(existing);
            return ResponseEntity.ok(updated);
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/{id}/parameter-groups/{groupId}")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<Void> deleteParameterGroup(@PathVariable String id, @PathVariable String groupId) {
        try {
            ParameterGroup group = parameterGroupService.get(groupId);
            if (group != null) {
                parameterGroupService.delete(group);
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @GetMapping("/{id}/linked-tests")
    public ResponseEntity<Map<String, Object>> getLinkedTests(@PathVariable String id) {
        try {
            ComplianceStandard standard = complianceStandardService.get(id);
            if (standard == null) {
                return ResponseEntity.notFound().build();
            }

            List<Map<String, Object>> linkedTests = complianceStandardService.getLinkedTests(id);

            Map<String, Object> response = Map.of("standard", standard, "linkedTests", linkedTests, "count",
                    linkedTests.size());
            return ResponseEntity.ok(response);
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Distinct list of {@code countryRegion} values currently in use across
     * standards. Backs the FR-1-007 type-ahead ComboBox so admins can pick an
     * existing region rather than re-typing.
     */
    @GetMapping("/country-regions")
    public ResponseEntity<List<String>> getDistinctCountryRegions() {
        try {
            return ResponseEntity.ok(complianceStandardService.getDistinctCountryRegions());
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * FR-7-004: Copy Standard. Duplicates the standard and all of its parameter
     * groups + thresholds + threshold value mappings as a new {@code DRAFT} record.
     * The new record's {@code version} is suffixed " - Copy" so the natural-key
     * uniqueness on (name, regulationNumber, version) doesn't collide with the
     * source.
     */
    @PostMapping("/{id}/copy")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<ComplianceStandard> copyStandard(@PathVariable String id, HttpServletRequest request) {
        try {
            ComplianceStandard original = complianceStandardService.get(id);
            if (original == null) {
                return ResponseEntity.notFound().build();
            }
            ComplianceStandard copy = complianceStandardService.copyStandard(original,
                    ControllerUtills.getSysUserId(request));
            return ResponseEntity.status(HttpStatus.CREATED).body(copy);
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Wraps each standard so the response carries a {@code parameterGroupCount}
     * sibling to the standard's other fields. Counts are loaded with a single GROUP
     * BY query (see {@link ParameterGroupService#countGroupsByStandardIds}) instead
     * of one count-per-standard, eliminating the N+1 fan-out this loop would
     * otherwise produce.
     */
    private List<ComplianceStandardListItem> toListItems(List<ComplianceStandard> standards) {
        if (standards == null || standards.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<String> ids = new java.util.ArrayList<>(standards.size());
        for (ComplianceStandard s : standards) {
            if (s.getId() != null) {
                ids.add(s.getId());
            }
        }
        // Two bulk aggregates → no N+1. Standards with zero of either count
        // are absent from the maps, so getOrDefault(0) backfills them.
        java.util.Map<String, Integer> groupCounts = parameterGroupService.countGroupsByStandardIds(ids);
        java.util.Map<String, Integer> testCounts = complianceThresholdService.countLinkedTestsByStandardIds(ids);
        List<ComplianceStandardListItem> items = new java.util.ArrayList<>(standards.size());
        for (ComplianceStandard s : standards) {
            String sid = s.getId();
            Integer groupCount = sid != null ? groupCounts.getOrDefault(sid, 0) : 0;
            Integer testCount = sid != null ? testCounts.getOrDefault(sid, 0) : 0;
            items.add(new ComplianceStandardListItem(s, groupCount, testCount));
        }
        return items;
    }

    /**
     * Response wrapper for the standards-list endpoints that adds a derived
     * {@code parameterGroupCount} alongside the {@link ComplianceStandard}'s own
     * fields.
     *
     * <p>
     * The count cannot live on the entity as a {@code @Transient} field:
     * {@code Hibernate5JakartaModule} honors {@code @jakarta.persistence.Transient}
     * and excludes those fields from JSON regardless of any {@code @JsonProperty}.
     * Wrapping with {@link JsonUnwrapped} keeps the wire format flat — the frontend
     * reads {@code s.parameterGroupCount} at the same level as {@code s.name} —
     * without polluting the entity.
     */
    public static class ComplianceStandardListItem {

        @JsonUnwrapped
        private final ComplianceStandard standard;

        private final Integer parameterGroupCount;
        private final Integer linkedTestCount;

        public ComplianceStandardListItem(ComplianceStandard standard, Integer parameterGroupCount,
                Integer linkedTestCount) {
            this.standard = standard;
            this.parameterGroupCount = parameterGroupCount;
            this.linkedTestCount = linkedTestCount;
        }

        public ComplianceStandard getStandard() {
            return standard;
        }

        @JsonProperty("parameterGroupCount")
        public Integer getParameterGroupCount() {
            return parameterGroupCount;
        }

        @JsonProperty("linkedTestCount")
        public Integer getLinkedTestCount() {
            return linkedTestCount;
        }
    }

    /**
     * Slim response wrapper for the parameter-groups list endpoint.
     *
     * <p>
     * Returning the {@link ParameterGroup} entity directly fails Jackson
     * serialization (HttpMessageNotWritableException) because the entity carries a
     * lazy {@code complianceThresholds} collection plus the back-reference to the
     * owning {@code standard}; Hibernate proxies and the managed/back-ref pair
     * combine to break the writer outside the transaction. The accordion UI only
     * reads id/name/description/sortOrder/isMandatory and fetches its own
     * thresholds via {@code /rest/compliance/thresholds?groupId=...}, so a
     * field-by-field copy is enough.
     */
    public static class ParameterGroupListItem {

        private final String id;
        private final UUID fhirUuid;
        private final String name;
        private final String description;
        private final Integer sortOrder;
        private final Boolean isMandatory;

        public ParameterGroupListItem(ParameterGroup g) {
            this.id = g.getId();
            this.fhirUuid = g.getFhirUuid();
            this.name = g.getName();
            this.description = g.getDescription();
            this.sortOrder = g.getSortOrder();
            this.isMandatory = g.getIsMandatory();
        }

        public String getId() {
            return id;
        }

        public UUID getFhirUuid() {
            return fhirUuid;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public Integer getSortOrder() {
            return sortOrder;
        }

        public Boolean getIsMandatory() {
            return isMandatory;
        }
    }
}