package org.openelisglobal.biorepository.controller.rest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openelisglobal.biorepository.dao.BioSampleRetrievalSearchCriteria;
import org.openelisglobal.biorepository.controller.rest.dto.BioSampleLifecycleEventDTO;
import org.openelisglobal.biorepository.controller.rest.dto.BioSampleListDTO;
import org.openelisglobal.biorepository.controller.rest.dto.BulkRegistrationResponse;
import org.openelisglobal.biorepository.controller.rest.dto.ManifestImportRequest;
import org.openelisglobal.biorepository.controller.rest.dto.ManifestValidationResponse;
import org.openelisglobal.biorepository.controller.rest.dto.SampleRegistrationDTO;
import org.openelisglobal.biorepository.service.BioSampleLifecycleService;
import org.openelisglobal.biorepository.service.BioSampleService;
import org.openelisglobal.biorepository.util.Brf02SamplePathFormatter;
import org.openelisglobal.biorepository.service.BiorepositoryApprovedSampleTypeService;
import org.openelisglobal.biorepository.service.RetentionPolicyService;
import org.openelisglobal.biorepository.service.ShipmentService;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BioSample.BiosafetyLevel;
import org.openelisglobal.biorepository.valueholder.BiorepositoryApprovedSampleType;
import org.openelisglobal.biorepository.valueholder.BiorepositoryApprovedSampleType.SampleCategory;
import org.openelisglobal.biorepository.valueholder.RetentionPolicy;
import org.openelisglobal.biorepository.valueholder.Shipment;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.department.service.DepartmentIsolationService;
import org.openelisglobal.rbac.RbacAction;
import org.openelisglobal.rbac.RbacPermissionService;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.sample.dao.SampleDAO;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.util.AccessionNumberHandler;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.storage.service.SampleStorageService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for BioSample operations in the Biorepository module.
 *
 * BioSample is an extension record for SampleItem, containing only
 * biorepository-specific metadata. Core sample data (barcode, type, quantity,
 * dates) is stored in Sample and SampleItem entities.
 *
 * This controller provides: - Sample registration (creates Sample + SampleItem
 * + BioSample extension) - Barcode generation for new samples - BioSample
 * extension lookup by SampleItem - Queries by biosafety level, ethics approval,
 * MTA reference, PI - Approved sample type listing - Biosafety statistics
 */
@RestController
@RequestMapping(value = "/rest/biorepository/sample")
public class BioSampleRestController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(BioSampleRestController.class);

    @Autowired
    private BioSampleService bioSampleService;

    @Autowired
    private BioSampleLifecycleService bioSampleLifecycleService;

    @Autowired
    private BiorepositoryApprovedSampleTypeService approvedSampleTypeService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleDAO sampleDAO;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private RetentionPolicyService retentionPolicyService;

    @Autowired
    private LocalizationService localizationService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private DepartmentIsolationService departmentIsolationService;

    @Autowired
    private RbacPermissionService rbacPermissionService;

    @Autowired
    private SampleStorageService sampleStorageService;

    private List<BioSample> filterAccessibleBioSamples(List<BioSample> list, HttpServletRequest request) {
        if (departmentIsolationService.hasUnrestrictedDepartmentAccess(request)) {
            return list;
        }
        return list.stream().filter(bs -> departmentIsolationService.canAccessBioSample(bs, request))
                .collect(Collectors.toList());
    }

    /**
     * Get a BioSample extension by ID.
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BioSample> getBioSample(@PathVariable("id") Integer id, HttpServletRequest request) {
        try {
            BioSample bioSample = bioSampleService.get(id);
            if (bioSample == null) {
                return ResponseEntity.notFound().build();
            }
            if (!departmentIsolationService.canAccessBioSample(bioSample, request)) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.ok(bioSample);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get BioSample extension by SampleItem ID.
     */
    @GetMapping(value = "/by-sample-item/{sampleItemId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BioSample> getBySampleItemId(@PathVariable("sampleItemId") Integer sampleItemId,
            HttpServletRequest request) {
        if (!departmentIsolationService.canAccessSampleItemIdentifier(String.valueOf(sampleItemId), request)) {
            return ResponseEntity.status(403).build();
        }
        BioSample bioSample = bioSampleService.getBySampleItemId(sampleItemId);
        if (bioSample == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(bioSample);
    }

    /**
     * Chronological lifecycle (storage placements, inbound transfer acceptance,
     * retrieval checkout, returns) aggregated from persisted data.
     */
    @GetMapping(value = "/{id}/lifecycle", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getLifecycleByBioSampleId(@PathVariable("id") Integer id,
            HttpServletRequest request) {
        BioSample bioSample = bioSampleService.get(id);
        if (bioSample == null) {
            return ResponseEntity.notFound().build();
        }
        if (!departmentIsolationService.canAccessBioSample(bioSample, request)) {
            return ResponseEntity.status(403).build();
        }
        List<BioSampleLifecycleEventDTO> events = bioSampleLifecycleService.buildLifecycleEvents(id);
        return ResponseEntity.ok(buildLifecycleResponse(bioSample, events));
    }

    /**
     * Same lifecycle trail as {@link #getLifecycleByBioSampleId(Integer)} keyed by
     * SampleItem ID.
     */
    @GetMapping(value = "/by-sample-item/{sampleItemId}/lifecycle", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getLifecycleBySampleItemId(
            @PathVariable("sampleItemId") Integer sampleItemId, HttpServletRequest request) {
        if (!departmentIsolationService.canAccessSampleItemIdentifier(String.valueOf(sampleItemId), request)) {
            return ResponseEntity.status(403).build();
        }
        BioSample bioSample = bioSampleService.getBySampleItemId(sampleItemId);
        if (bioSample == null) {
            return ResponseEntity.notFound().build();
        }
        List<BioSampleLifecycleEventDTO> events = bioSampleLifecycleService.buildLifecycleEvents(bioSample.getId());
        return ResponseEntity.ok(buildLifecycleResponse(bioSample, events));
    }

    private Map<String, Object> buildLifecycleResponse(BioSample bioSample, List<BioSampleLifecycleEventDTO> events) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("bioSampleId", bioSample.getId());
        if (bioSample.getSampleItem() != null && bioSample.getSampleItem().getId() != null) {
            body.put("sampleItemId", Integer.valueOf(bioSample.getSampleItem().getId()));
        } else {
            body.put("sampleItemId", null);
        }
        body.put("events", events != null ? events : List.of());
        return body;
    }

    /**
     * Get BioSample by barcode (SampleItem.externalId). Returns sample details for
     * manual disposal lookup. Only returns samples that are in storage (not already
     * disposed).
     *
     * GET /rest/biorepository/sample/by-barcode/{barcode}
     *
     * @param barcode the sample barcode (external ID)
     * @return sample details as BioSampleListDTO or 404 if not found
     */
    @GetMapping(value = "/by-barcode/{barcode}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getByBarcode(@PathVariable("barcode") String barcode, HttpServletRequest request) {
        if (barcode == null || barcode.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Barcode is required"));
        }

        // Search for SampleItem by external_id (barcode)
        List<SampleItem> sampleItems = sampleItemService.getSampleItemsByExternalID(barcode.trim());

        if (sampleItems == null || sampleItems.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "No sample found with barcode: " + barcode));
        }

        // Get the first matching sample item
        SampleItem sampleItem = sampleItems.get(0);
        if (!departmentIsolationService.canAccessSampleItem(sampleItem, request)) {
            return ResponseEntity.status(403).build();
        }

        // Get the BioSample extension if it exists
        BioSample bioSample = bioSampleService.getBySampleItemId(Integer.valueOf(sampleItem.getId()));

        // Build response DTO
        BioSampleListDTO dto = new BioSampleListDTO();
        dto.setId(bioSample != null ? bioSample.getId() : null);
        dto.setSampleItemId(Integer.valueOf(sampleItem.getId()));
        dto.setBarcode(sampleItem.getExternalId());

        // Get sample type and convert to SampleTypeDTO
        if (sampleItem.getTypeOfSample() != null) {
            BioSampleListDTO.SampleTypeDTO sampleTypeDTO = new BioSampleListDTO.SampleTypeDTO(
                    sampleItem.getTypeOfSample().getId(), sampleItem.getTypeOfSample().getDescription());
            dto.setSampleType(sampleTypeDTO);
        }

        // Get parent sample info
        if (sampleItem.getSample() != null) {
            dto.setAccessionNumber(sampleItem.getSample().getAccessionNumber());
            dto.setSampleId(Integer.valueOf(sampleItem.getSample().getId()));
        }

        // Add BioSample-specific fields if extension exists
        if (bioSample != null) {
            dto.setWorkflowStatus(
                    bioSample.getWorkflowStatus() != null ? bioSample.getWorkflowStatus().name() : "REGISTERED");
            dto.setBiosafetyLevel(bioSample.getBiosafetyLevel() != null ? bioSample.getBiosafetyLevel().name() : null);
            dto.setRetentionExpiryDate(bioSample.getRetentionExpiryDate());
            dto.setOriginLab(bioSample.getOriginLab());
            dto.setProjectId(bioSample.getProjectId());

            // Get retention policy name if set
            if (bioSample.getRetentionPolicyId() != null) {
                RetentionPolicy policy = retentionPolicyService.get(bioSample.getRetentionPolicyId());
                if (policy != null) {
                    dto.setRetentionPolicyName(policy.getPolicyName());
                    dto.setRetentionPolicyId(policy.getId());
                }
            }

            // Get shipment info
            if (bioSample.getShipment() != null) {
                dto.setShipmentId(bioSample.getShipment().getId());
            }
        } else {
            // No BioSample extension - sample might be a regular sample without
            // biorepository metadata
            dto.setWorkflowStatus("REGISTERED");
        }

        return ResponseEntity.ok(dto);
    }

    /**
     * Get all BioSample extensions for a shipment.
     */
    @GetMapping(value = "/by-shipment/{shipmentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<BioSample>> getByShipmentId(@PathVariable("shipmentId") Integer shipmentId,
            HttpServletRequest request) {
        List<BioSample> bioSamples = bioSampleService.getByShipmentId(shipmentId);
        if (!departmentIsolationService.hasUnrestrictedDepartmentAccess(request)) {
            bioSamples = bioSamples.stream().filter(bs -> departmentIsolationService.canAccessBioSample(bs, request))
                    .collect(Collectors.toList());
        }
        return ResponseEntity.ok(bioSamples);
    }

    /**
     * Get BioSample extensions by biosafety level.
     */
    @GetMapping(value = "/by-biosafety-level", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<BioSample>> getByBiosafetyLevel(@RequestParam BiosafetyLevel biosafetyLevel,
            HttpServletRequest request) {
        List<BioSample> bioSamples = filterAccessibleBioSamples(bioSampleService.getByBiosafetyLevel(biosafetyLevel),
                request);
        return ResponseEntity.ok(bioSamples);
    }

    /**
     * Get BioSample extensions by ethics approval reference.
     */
    @GetMapping(value = "/by-ethics-ref", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<BioSample>> getByEthicsApprovalRef(@RequestParam String ethicsApprovalRef,
            HttpServletRequest request) {
        List<BioSample> bioSamples = filterAccessibleBioSamples(
                bioSampleService.getByEthicsApprovalRef(ethicsApprovalRef), request);
        return ResponseEntity.ok(bioSamples);
    }

    /**
     * Get BioSample extensions by MTA reference.
     */
    @GetMapping(value = "/by-mta-ref", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<BioSample>> getByMtaReference(@RequestParam String mtaReference,
            HttpServletRequest request) {
        List<BioSample> bioSamples = filterAccessibleBioSamples(bioSampleService.getByMtaReference(mtaReference),
                request);
        return ResponseEntity.ok(bioSamples);
    }

    /**
     * Get BioSample extensions by principal investigator.
     */
    @GetMapping(value = "/by-pi", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<BioSample>> getByPrincipalInvestigator(@RequestParam String principalInvestigator,
            HttpServletRequest request) {
        List<BioSample> bioSamples = filterAccessibleBioSamples(
                bioSampleService.getByPrincipalInvestigator(principalInvestigator), request);
        return ResponseEntity.ok(bioSamples);
    }

    /**
     * Count BioSample extensions by shipment.
     */
    @GetMapping(value = "/count/by-shipment/{shipmentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Long>> countByShipmentId(@PathVariable("shipmentId") Integer shipmentId,
            HttpServletRequest request) {
        long count = filterAccessibleBioSamples(bioSampleService.getByShipmentId(shipmentId), request).size();
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Count BioSample extensions by biosafety level.
     */
    @GetMapping(value = "/count/by-biosafety-level", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Long>> countByBiosafetyLevel(@RequestParam BiosafetyLevel biosafetyLevel,
            HttpServletRequest request) {
        long count = filterAccessibleBioSamples(bioSampleService.getByBiosafetyLevel(biosafetyLevel), request).size();
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Check if a BioSample extension exists for a SampleItem.
     */
    @GetMapping(value = "/exists/by-sample-item/{sampleItemId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Boolean>> existsBySampleItemId(@PathVariable("sampleItemId") Integer sampleItemId,
            HttpServletRequest request) {
        if (!departmentIsolationService.canAccessSampleItemIdentifier(String.valueOf(sampleItemId), request)) {
            return ResponseEntity.ok(Map.of("exists", false));
        }
        boolean exists = bioSampleService.existsBySampleItemId(sampleItemId);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    /**
     * Get approved sample types for biorepository use. Returns only sample types
     * that are configured as approved per FR-MAN-007.
     */
    @GetMapping(value = "/approved-sample-types", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getApprovedSampleTypes() {
        List<TypeOfSample> approvedTypes = approvedSampleTypeService.getApprovedTypeOfSamples();

        List<Map<String, Object>> result = new ArrayList<>();
        for (TypeOfSample type : approvedTypes) {
            result.add(Map.of("id", type.getId(), "name", type.getDescription() != null ? type.getDescription() : "",
                    "localizedName",
                    type.getLocalizedName() != null ? type.getLocalizedName() : type.getDescription()));
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Get biosafety level statistics.
     */
    @GetMapping(value = "/stats/biosafety", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Long>> getBiosafetyStats(HttpServletRequest request) {
        if (departmentIsolationService.hasUnrestrictedDepartmentAccess(request)) {
            Map<String, Long> stats = Map.of("BSL_1", bioSampleService.countByBiosafetyLevel(BiosafetyLevel.BSL_1),
                    "BSL_2", bioSampleService.countByBiosafetyLevel(BiosafetyLevel.BSL_2), "BSL_3",
                    bioSampleService.countByBiosafetyLevel(BiosafetyLevel.BSL_3), "BSL_4",
                    bioSampleService.countByBiosafetyLevel(BiosafetyLevel.BSL_4));
            return ResponseEntity.ok(stats);
        }
        List<BioSample> scoped = filterAccessibleBioSamples(bioSampleService.getAll(), request);
        Map<String, Long> stats = new java.util.LinkedHashMap<>();
        stats.put("BSL_1", scoped.stream().filter(bs -> BiosafetyLevel.BSL_1.equals(bs.getBiosafetyLevel())).count());
        stats.put("BSL_2", scoped.stream().filter(bs -> BiosafetyLevel.BSL_2.equals(bs.getBiosafetyLevel())).count());
        stats.put("BSL_3", scoped.stream().filter(bs -> BiosafetyLevel.BSL_3.equals(bs.getBiosafetyLevel())).count());
        stats.put("BSL_4", scoped.stream().filter(bs -> BiosafetyLevel.BSL_4.equals(bs.getBiosafetyLevel())).count());
        return ResponseEntity.ok(stats);
    }

    /**
     * Calculate and save retention expiry dates for samples. This is typically
     * called when samples are "advanced to storage".
     *
     * The retention policy is determined by: 1. Project-specific policy (if sample
     * has a project and a matching policy exists) 2. Sample type policy (fallback
     * if no project policy)
     *
     * The expiry date is calculated from the sample's collection date (or receipt
     * date if no collection date).
     *
     * @param request     contains list of sample item IDs
     * @param httpRequest for user session
     * @return result with updated count and any errors
     */
    @PostMapping(value = "/calculate-retention", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> calculateRetention(@RequestBody CalculateRetentionRequest request,
            HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        if (request.getSampleItemIds() == null || request.getSampleItemIds().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Sample item IDs are required"));
        }

        int updatedCount = 0;
        List<Map<String, Object>> results = new ArrayList<>();

        // Fetch all BioSamples with relationships eagerly loaded (prevents
        // LazyInitializationException)
        List<BioSample> bioSamples = bioSampleService.getBySampleItemIds(request.getSampleItemIds());
        Map<Integer, BioSample> bioSampleMap = new java.util.HashMap<>();
        for (BioSample bs : bioSamples) {
            if (bs.getSampleItem() != null) {
                bioSampleMap.put(Integer.valueOf(bs.getSampleItem().getId()), bs);
            }
        }

        for (Integer sampleItemId : request.getSampleItemIds()) {
            Map<String, Object> sampleResult = new java.util.HashMap<>();
            sampleResult.put("sampleItemId", sampleItemId);

            try {
                BioSample bioSample = bioSampleMap.get(sampleItemId);
                if (bioSample == null) {
                    sampleResult.put("status", "skipped");
                    sampleResult.put("reason", "No BioSample extension found");
                    results.add(sampleResult);
                    continue;
                }

                // Get sample type ID from linked SampleItem
                SampleItem sampleItem = bioSample.getSampleItem();
                Integer sampleTypeId = null;
                java.time.LocalDate fromDate = null;

                if (sampleItem != null) {
                    if (sampleItem.getTypeOfSample() != null) {
                        sampleTypeId = Integer.parseInt(sampleItem.getTypeOfSample().getId());
                    }
                    // Use collection date, or fall back to receipt date
                    if (sampleItem.getCollectionDate() != null) {
                        fromDate = sampleItem.getCollectionDate().toLocalDateTime().toLocalDate();
                    } else if (sampleItem.getSample() != null
                            && sampleItem.getSample().getReceivedTimestamp() != null) {
                        fromDate = sampleItem.getSample().getReceivedTimestamp().toLocalDateTime().toLocalDate();
                    }
                }

                if (fromDate == null) {
                    sampleResult.put("status", "skipped");
                    sampleResult.put("reason", "No collection or receipt date available");
                    results.add(sampleResult);
                    continue;
                }

                // Find applicable policy using project name (string) and sample type ID
                String projectName = bioSample.getProjectId(); // This is actually the project code/name
                java.util.Optional<RetentionPolicy> policyOpt = retentionPolicyService
                        .findApplicablePolicyByProjectName(projectName, sampleTypeId);

                if (policyOpt.isEmpty()) {
                    sampleResult.put("status", "skipped");
                    sampleResult.put("reason", "No matching retention policy found");
                    results.add(sampleResult);
                    continue;
                }

                RetentionPolicy policy = policyOpt.get();
                java.time.LocalDate expiryDate = policy.calculateExpiryDate(fromDate);

                // Update BioSample with retention info
                bioSample.setRetentionPolicyId(policy.getId());
                bioSample.setRetentionExpiryDate(java.sql.Date.valueOf(expiryDate));
                bioSample.setSysUserId(sysUserId);
                bioSampleService.update(bioSample);

                sampleResult.put("status", "updated");
                sampleResult.put("policyId", policy.getId());
                sampleResult.put("policyName", policy.getPolicyName());
                sampleResult.put("expiryDate", expiryDate.toString());
                results.add(sampleResult);
                updatedCount++;

            } catch (Exception e) {
                sampleResult.put("status", "error");
                sampleResult.put("reason", e.getMessage());
                results.add(sampleResult);
            }
        }

        return ResponseEntity.ok(Map.of("success", true, "updatedCount", updatedCount, "totalRequested",
                request.getSampleItemIds().size(), "results", results));
    }

    /**
     * Request body for calculate retention endpoint.
     */
    public static class CalculateRetentionRequest {
        private List<Integer> sampleItemIds;

        public List<Integer> getSampleItemIds() {
            return sampleItemIds;
        }

        public void setSampleItemIds(List<Integer> sampleItemIds) {
            this.sampleItemIds = sampleItemIds;
        }
    }

    /**
     * Update workflow status for one or more BioSamples. Used to track sample
     * progression through the biorepository lifecycle.
     *
     * @param request     contains sample item IDs and new workflow status
     * @param httpRequest for user session
     * @return result with updated count
     */
    @PutMapping(value = "/workflow-status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> updateWorkflowStatus(@RequestBody UpdateWorkflowStatusRequest request,
            HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        if (request.getSampleItemIds() == null || request.getSampleItemIds().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Sample item IDs are required"));
        }

        if (request.getWorkflowStatus() == null || request.getWorkflowStatus().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Workflow status is required"));
        }

        BioSample.WorkflowStatus newStatus = BioSample.WorkflowStatus.fromString(request.getWorkflowStatus());
        if (newStatus == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid workflow status: " + request.getWorkflowStatus()));
        }

        int updatedCount = 0;
        List<BioSample> bioSamples = bioSampleService.getBySampleItemIds(request.getSampleItemIds());

        for (BioSample bioSample : bioSamples) {
            if (!departmentIsolationService.canAccessBioSample(bioSample, httpRequest)) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied for one or more samples"));
            }
        }

        for (BioSample bioSample : bioSamples) {
            bioSample.setWorkflowStatus(newStatus);
            bioSample.setSysUserId(sysUserId);
            bioSampleService.update(bioSample);
            updatedCount++;
        }

        return ResponseEntity.ok(Map.of("success", true, "updatedCount", updatedCount, "newStatus", newStatus.name()));
    }

    /**
     * Request body for workflow status update endpoint.
     */
    public static class UpdateWorkflowStatusRequest {
        private List<Integer> sampleItemIds;
        private String workflowStatus;

        public List<Integer> getSampleItemIds() {
            return sampleItemIds;
        }

        public void setSampleItemIds(List<Integer> sampleItemIds) {
            this.sampleItemIds = sampleItemIds;
        }

        public String getWorkflowStatus() {
            return workflowStatus;
        }

        public void setWorkflowStatus(String workflowStatus) {
            this.workflowStatus = workflowStatus;
        }
    }

    /**
     * List all biorepository samples with enriched data. Combines data from
     * BioSample, SampleItem, and Sample entities.
     *
     * Uses eager-loading queries to prevent LazyInitializationException.
     *
     * @param shipmentId    optional filter by shipment ID
     * @param sampleItemIds optional comma-separated list of sample item IDs to
     *                      filter by
     * @param limit         maximum number of samples to return (default 100)
     * @return list of enriched sample DTOs
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<BioSampleListDTO>> listSamples(@RequestParam(required = false) Integer shipmentId,
            @RequestParam(required = false) String sampleItemIds,
            @RequestParam(required = false, defaultValue = "100") Integer limit,
            @RequestParam(required = false) String workflowStatus, HttpServletRequest request) {

        List<BioSample> bioSamples;
        if (sampleItemIds != null && !sampleItemIds.trim().isEmpty()) {
            // Filter by sample item IDs
            List<Integer> ids = new ArrayList<>();
            for (String idStr : sampleItemIds.split(",")) {
                try {
                    ids.add(Integer.parseInt(idStr.trim()));
                } catch (NumberFormatException e) {
                    // Skip invalid IDs
                }
            }
            bioSamples = bioSampleService.getBySampleItemIds(ids);
        } else if (shipmentId != null) {
            // Use eager-loading method to prevent LazyInitializationException
            bioSamples = bioSampleService.getByShipmentIdWithRelationships(shipmentId);
        } else {
            // Use eager-loading method with limit
            bioSamples = bioSampleService.getAllWithRelationships(limit);
        }

        // Filter by workflow status if specified
        if (workflowStatus != null && !workflowStatus.trim().isEmpty()) {
            BioSample.WorkflowStatus filterStatus = BioSample.WorkflowStatus.fromString(workflowStatus);
            if (filterStatus != null) {
                bioSamples = bioSamples.stream().filter(bs -> filterStatus.equals(bs.getWorkflowStatus())
                        || (bs.getWorkflowStatus() == null && filterStatus == BioSample.WorkflowStatus.REGISTERED))
                        .collect(java.util.stream.Collectors.toList());
            }
        }

        bioSamples = filterAccessibleBioSamples(bioSamples, request);

        List<BioSampleListDTO> result = new ArrayList<>();
        for (BioSample bioSample : bioSamples) {
            result.add(convertToListDTO(bioSample));
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Discovery search for biorepository-held samples available for retrieval.
     * AND-combines partial filters; at least one filter or browse=true required.
     *
     * GET /rest/biorepository/sample/search?sampleType=Plasma&status=STORED
     * GET /rest/biorepository/sample/search?browse=true&status=STORED
     */
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<BioSampleListDTO>> searchSamples(@RequestParam(required = false) String barcode,
            @RequestParam(required = false) String originLab, @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String accessionNumber, @RequestParam(required = false) String sampleType,
            @RequestParam(required = false) String collectionDateFrom,
            @RequestParam(required = false) String collectionDateTo,
            @RequestParam(required = false) String context,
            @RequestParam(required = false, defaultValue = "false") Boolean browse,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "50") Integer limit, HttpServletRequest request) {

        boolean hasBrowse = Boolean.TRUE.equals(browse);
        boolean hasBarcode = barcode != null && !barcode.trim().isEmpty();
        boolean hasOriginLab = originLab != null && !originLab.trim().isEmpty();
        boolean hasProjectId = projectId != null && !projectId.trim().isEmpty();
        boolean hasAccessionNumber = accessionNumber != null && !accessionNumber.trim().isEmpty();
        boolean hasSampleType = sampleType != null && !sampleType.trim().isEmpty();
        boolean hasCollectionDateFrom = collectionDateFrom != null && !collectionDateFrom.trim().isEmpty();
        boolean hasCollectionDateTo = collectionDateTo != null && !collectionDateTo.trim().isEmpty();

        boolean hasAnyFilter = hasBarcode || hasOriginLab || hasProjectId || hasAccessionNumber || hasSampleType
                || hasCollectionDateFrom || hasCollectionDateTo;

        if (!hasBrowse && !hasAnyFilter) {
            return ResponseEntity.ok(new ArrayList<>());
        }

        BioSample.WorkflowStatus filterStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            filterStatus = BioSample.WorkflowStatus.fromString(status);
        }
        if (hasBrowse && filterStatus == null) {
            filterStatus = BioSample.WorkflowStatus.STORED;
        }

        Set<String> matchingTypeIds = null;
        if (hasSampleType) {
            matchingTypeIds = resolveSampleTypeIdsForSearch(sampleType.trim());
            if (matchingTypeIds.isEmpty()) {
                return ResponseEntity.ok(new ArrayList<>());
            }
        }

        BioSampleRetrievalSearchCriteria criteria = new BioSampleRetrievalSearchCriteria();
        criteria.setWorkflowStatus(filterStatus);
        criteria.setBarcodePattern(wrapLikePattern(barcode));
        criteria.setAccessionPattern(wrapLikePattern(accessionNumber));
        criteria.setSampleTypeIds(matchingTypeIds);
        criteria.setOriginLabPattern(wrapLikePattern(originLab));
        criteria.setProjectIdPattern(wrapLikePattern(projectId));
        criteria.setCollectionDateFrom(parseCollectionDateStart(collectionDateFrom));
        criteria.setCollectionDateTo(parseCollectionDateEnd(collectionDateTo));
        criteria.setLimit(limit != null && limit > 0 ? limit : 50);

        List<BioSample> bioSamples = bioSampleService.searchForRetrieval(criteria);

        List<BioSampleListDTO> result = new ArrayList<>();
        for (BioSample bioSample : bioSamples) {
            if (bioSample.getSampleItem() == null) {
                continue;
            }
            BioSampleListDTO dto = buildSearchResultDTOFromBioSample(bioSample, filterStatus);
            if (dto != null) {
                result.add(dto);
            }
        }

        if (!departmentIsolationService.hasUnrestrictedDepartmentAccess(request)) {
            result = result.stream()
                    .filter(d -> d.getSampleItemId() != null && departmentIsolationService
                            .canAccessSampleItemIdentifier(String.valueOf(d.getSampleItemId()), request))
                    .collect(Collectors.toList());
        }

        sortSearchResults(result, barcode, accessionNumber, sampleType, originLab, projectId, context);
        if (result.size() > criteria.getLimit()) {
            result = new ArrayList<>(result.subList(0, criteria.getLimit()));
        }

        return ResponseEntity.ok(result);
    }

    private String wrapLikePattern(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return "%" + value.trim() + "%";
    }

    private Timestamp parseCollectionDateStart(String value) {
        LocalDate date = parseCollectionDate(value);
        return date != null ? Timestamp.valueOf(date.atStartOfDay()) : null;
    }

    private Timestamp parseCollectionDateEnd(String value) {
        LocalDate date = parseCollectionDate(value);
        return date != null ? Timestamp.valueOf(date.atTime(23, 59, 59)) : null;
    }

    private LocalDate parseCollectionDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private void sortSearchResults(List<BioSampleListDTO> results, String barcode, String accessionNumber,
            String sampleType, String originLab, String projectId, String context) {
        String barcodeTerm = barcode != null ? barcode.trim().toLowerCase(Locale.ROOT) : null;
        String accessionTerm = accessionNumber != null ? accessionNumber.trim().toLowerCase(Locale.ROOT) : null;
        String sampleTypeTerm = sampleType != null ? sampleType.trim().toLowerCase(Locale.ROOT) : null;
        String originLabTerm = originLab != null ? originLab.trim().toLowerCase(Locale.ROOT) : null;
        String projectIdTerm = projectId != null ? projectId.trim().toLowerCase(Locale.ROOT) : null;
        boolean fulfillmentContext = "fulfillment".equalsIgnoreCase(context);

        results.forEach(dto -> dto.setMatchReason(determineMatchReason(
                dto, barcodeTerm, accessionTerm, sampleTypeTerm, originLabTerm, projectIdTerm, fulfillmentContext)));

        Comparator<BioSampleListDTO> comparator = Comparator
                .comparingInt((BioSampleListDTO dto) -> fulfillmentMatchScore(
                        dto, barcodeTerm, accessionTerm, sampleTypeTerm, originLabTerm, projectIdTerm,
                        fulfillmentContext))
                .reversed()
                .thenComparing(BioSampleListDTO::getCollectionDate,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(BioSampleListDTO::getId, Comparator.nullsLast(Comparator.reverseOrder()));

        results.sort(comparator);
    }

    private int fulfillmentMatchScore(BioSampleListDTO dto, String barcodeTerm, String accessionTerm,
            String sampleTypeTerm, String originLabTerm, String projectIdTerm, boolean fulfillmentContext) {
        int score = 0;
        if (barcodeTerm != null && dto.getBarcode() != null
                && dto.getBarcode().equalsIgnoreCase(barcodeTerm)) {
            score += 200;
        } else if (barcodeTerm != null && dto.getBarcode() != null
                && dto.getBarcode().toLowerCase(Locale.ROOT).contains(barcodeTerm)) {
            score += 120;
        }
        if (accessionTerm != null && dto.getAccessionNumber() != null
                && dto.getAccessionNumber().equalsIgnoreCase(accessionTerm)) {
            score += 220;
        } else if (accessionTerm != null && dto.getAccessionNumber() != null
                && dto.getAccessionNumber().toLowerCase(Locale.ROOT).contains(accessionTerm)) {
            score += 140;
        }

        if (fulfillmentContext) {
            if (matchesTerm(dto.getSampleType() != null ? dto.getSampleType().getDescription() : null, sampleTypeTerm)) {
                score += 40;
            }
            if (matchesTerm(dto.getOriginLab(), originLabTerm)) {
                score += 35;
            }
            if (matchesTerm(dto.getProjectId(), projectIdTerm)) {
                score += 20;
            }
            if (sampleTypeTerm != null && originLabTerm != null
                    && matchesTerm(dto.getSampleType() != null ? dto.getSampleType().getDescription() : null,
                            sampleTypeTerm)
                    && matchesTerm(dto.getOriginLab(), originLabTerm)) {
                score += 25;
            }
        }
        return score;
    }

    private String determineMatchReason(BioSampleListDTO dto, String barcodeTerm, String accessionTerm,
            String sampleTypeTerm, String originLabTerm, String projectIdTerm, boolean fulfillmentContext) {
        if (accessionTerm != null && dto.getAccessionNumber() != null
                && dto.getAccessionNumber().equalsIgnoreCase(accessionTerm)) {
            return "EXACT_ACCESSION";
        }
        if (barcodeTerm != null && dto.getBarcode() != null
                && dto.getBarcode().equalsIgnoreCase(barcodeTerm)) {
            return "EXACT_BARCODE";
        }
        if (!fulfillmentContext) {
            return null;
        }

        boolean sameType = matchesTerm(dto.getSampleType() != null ? dto.getSampleType().getDescription() : null,
                sampleTypeTerm);
        boolean sameOrigin = matchesTerm(dto.getOriginLab(), originLabTerm);
        boolean sameProject = matchesTerm(dto.getProjectId(), projectIdTerm);

        if (sameType && sameOrigin) {
            return "SAME_TYPE_ORIGIN";
        }
        if (sameProject) {
            return "RELATED_PROJECT";
        }
        if (sameType) {
            return "SAME_TYPE";
        }
        if (sameOrigin) {
            return "SAME_ORIGIN";
        }
        return null;
    }

    private boolean matchesTerm(String value, String term) {
        return term != null && value != null && value.toLowerCase(Locale.ROOT).contains(term);
    }

    /**
     * Build search result DTO from SampleItem, applying workflow status filter.
     */
    private BioSampleListDTO buildSearchResultDTO(SampleItem sampleItem, BioSample.WorkflowStatus filterStatus) {
        if (sampleItem == null) {
            return null;
        }

        // Get BioSample extension if exists
        BioSample bioSample = bioSampleService.getBySampleItemId(Integer.valueOf(sampleItem.getId()));

        // Apply workflow status filter
        if (filterStatus != null) {
            BioSample.WorkflowStatus sampleStatus = bioSample != null ? bioSample.getWorkflowStatus()
                    : BioSample.WorkflowStatus.REGISTERED;
            if (sampleStatus == null) {
                sampleStatus = BioSample.WorkflowStatus.REGISTERED;
            }
            if (!filterStatus.equals(sampleStatus)) {
                return null;
            }
        }

        // Build DTO
        BioSampleListDTO dto = new BioSampleListDTO();
        dto.setSampleItemId(Integer.valueOf(sampleItem.getId()));
        dto.setBarcode(sampleItem.getExternalId());

        // Get sample type
        if (sampleItem.getTypeOfSample() != null) {
            BioSampleListDTO.SampleTypeDTO sampleTypeDTO = new BioSampleListDTO.SampleTypeDTO(
                    sampleItem.getTypeOfSample().getId(), sampleItem.getTypeOfSample().getDescription());
            dto.setSampleType(sampleTypeDTO);
        }

        // Get parent sample info
        if (sampleItem.getSample() != null) {
            dto.setAccessionNumber(sampleItem.getSample().getAccessionNumber());
            dto.setSampleId(Integer.valueOf(sampleItem.getSample().getId()));
        }

        if (sampleItem.getCollectionDate() != null) {
            dto.setCollectionDate(sampleItem.getCollectionDate());
        }

        populateQuantityFields(dto, sampleItem);

        // Add BioSample-specific fields if extension exists
        if (bioSample != null) {
            dto.setId(bioSample.getId());
            dto.setWorkflowStatus(
                    bioSample.getWorkflowStatus() != null ? bioSample.getWorkflowStatus().name() : "REGISTERED");
            dto.setBiosafetyLevel(bioSample.getBiosafetyLevel() != null ? bioSample.getBiosafetyLevel().name() : null);
            dto.setRetentionExpiryDate(bioSample.getRetentionExpiryDate());
            dto.setOriginLab(bioSample.getOriginLab());
            dto.setProjectId(bioSample.getProjectId());

            if (bioSample.getRetentionPolicyId() != null) {
                RetentionPolicy policy = retentionPolicyService.get(bioSample.getRetentionPolicyId());
                if (policy != null) {
                    dto.setRetentionPolicyName(policy.getPolicyName());
                    dto.setRetentionPolicyId(policy.getId());
                }
            }
            if (bioSample.getShipment() != null) {
                dto.setShipmentId(bioSample.getShipment().getId());
            }
        } else {
            dto.setWorkflowStatus("REGISTERED");
        }

        populateStorageLocation(dto, sampleItem);

        return dto;
    }

    private void populateStorageLocation(BioSampleListDTO dto, SampleItem sampleItem) {
        if (dto == null || sampleItem == null || sampleItem.getId() == null) {
            return;
        }

        Map<String, Object> location = sampleStorageService.getSampleItemLocation(sampleItem.getId());
        if (location == null || location.isEmpty()) {
            return;
        }

        dto.setRoomName(asTrimmedString(location.get("roomName")));
        dto.setDeviceName(asTrimmedString(location.get("deviceName")));
        dto.setShelfLabel(asTrimmedString(location.get("shelfLabel")));
        dto.setRackLabel(asTrimmedString(location.get("rackLabel")));
        dto.setBoxLabel(asTrimmedString(location.get("boxLabel")));
        dto.setPositionCoordinate(asTrimmedString(location.get("positionCoordinate")));

        String hierarchicalPath = asTrimmedString(location.get("hierarchicalPath"));
        if (hierarchicalPath == null) {
            hierarchicalPath = asTrimmedString(location.get("location"));
        }
        dto.setHierarchicalPath(hierarchicalPath);
        dto.setSamplePath(Brf02SamplePathFormatter.format(location));
    }

    private String asTrimmedString(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    /**
     * Build search result DTO from BioSample, applying workflow status filter.
     */
    private BioSampleListDTO buildSearchResultDTOFromBioSample(BioSample bioSample,
            BioSample.WorkflowStatus filterStatus) {
        if (bioSample == null || bioSample.getSampleItem() == null) {
            return null;
        }

        // Apply workflow status filter
        if (filterStatus != null) {
            BioSample.WorkflowStatus sampleStatus = bioSample.getWorkflowStatus();
            if (sampleStatus == null) {
                sampleStatus = BioSample.WorkflowStatus.REGISTERED;
            }
            if (!filterStatus.equals(sampleStatus)) {
                return null;
            }
        }

        SampleItem sampleItem = bioSample.getSampleItem();

        // Build DTO
        BioSampleListDTO dto = new BioSampleListDTO();
        dto.setId(bioSample.getId());
        dto.setSampleItemId(Integer.valueOf(sampleItem.getId()));
        dto.setBarcode(sampleItem.getExternalId());
        dto.setWorkflowStatus(
                bioSample.getWorkflowStatus() != null ? bioSample.getWorkflowStatus().name() : "REGISTERED");
        dto.setBiosafetyLevel(bioSample.getBiosafetyLevel() != null ? bioSample.getBiosafetyLevel().name() : null);
        dto.setRetentionExpiryDate(bioSample.getRetentionExpiryDate());
        dto.setOriginLab(bioSample.getOriginLab());
        dto.setProjectId(bioSample.getProjectId());

        // Get sample type
        if (sampleItem.getTypeOfSample() != null) {
            BioSampleListDTO.SampleTypeDTO sampleTypeDTO = new BioSampleListDTO.SampleTypeDTO(
                    sampleItem.getTypeOfSample().getId(), sampleItem.getTypeOfSample().getDescription());
            dto.setSampleType(sampleTypeDTO);
        }

        // Get parent sample info
        if (sampleItem.getSample() != null) {
            dto.setAccessionNumber(sampleItem.getSample().getAccessionNumber());
            dto.setSampleId(Integer.valueOf(sampleItem.getSample().getId()));
        }

        if (sampleItem.getCollectionDate() != null) {
            dto.setCollectionDate(sampleItem.getCollectionDate());
        }

        populateQuantityFields(dto, sampleItem);

        if (bioSample.getRetentionPolicyId() != null) {
            RetentionPolicy policy = retentionPolicyService.get(bioSample.getRetentionPolicyId());
            if (policy != null) {
                dto.setRetentionPolicyName(policy.getPolicyName());
                dto.setRetentionPolicyId(policy.getId());
            }
        }
        if (bioSample.getShipment() != null) {
            dto.setShipmentId(bioSample.getShipment().getId());
        }

        populateStorageLocation(dto, sampleItem);

        return dto;
    }

    /**
     * Convert BioSample to enriched DTO by fetching related data.
     */
    private BioSampleListDTO convertToListDTO(BioSample bioSample) {
        BioSampleListDTO dto = new BioSampleListDTO();
        dto.setId(bioSample.getId());
        dto.setBiosafetyLevel(bioSample.getBiosafetyLevel() != null ? bioSample.getBiosafetyLevel().name() : null);
        dto.setOriginLab(bioSample.getOriginLab());
        dto.setProjectId(bioSample.getProjectId());
        dto.setPrincipalInvestigator(bioSample.getPrincipalInvestigator());
        dto.setEthicsApprovalRef(bioSample.getEthicsApprovalRef());
        dto.setMtaReference(bioSample.getMtaReference());
        dto.setPreservationMedium(bioSample.getPreservationMedium());
        dto.setArrivalCondition(bioSample.getArrivalCondition());
        dto.setRequiredTempMin(bioSample.getRequiredTempMin());
        dto.setRequiredTempMax(bioSample.getRequiredTempMax());

        // Get shipment ID if linked
        if (bioSample.getShipment() != null) {
            dto.setShipmentId(bioSample.getShipment().getId());
            // Get documentation status from shipment
            dto.setDocumentationStatus(bioSample.getShipment().getDocumentationStatus() != null
                    ? bioSample.getShipment().getDocumentationStatus().name()
                    : "PENDING");
        } else {
            dto.setDocumentationStatus("PENDING");
        }

        // Get data from linked SampleItem
        SampleItem sampleItem = bioSample.getSampleItem();
        if (sampleItem != null) {
            dto.setBarcode(sampleItem.getExternalId());
            dto.setSampleItemId(Integer.valueOf(sampleItem.getId()));
            dto.setCollectionDate(sampleItem.getCollectionDate());

            // Get sample type
            if (sampleItem.getTypeOfSample() != null) {
                BioSampleListDTO.SampleTypeDTO sampleTypeDTO = new BioSampleListDTO.SampleTypeDTO(
                        sampleItem.getTypeOfSample().getId(), sampleItem.getTypeOfSample().getDescription());
                dto.setSampleType(sampleTypeDTO);
            }

            // Get data from parent Sample
            Sample sample = sampleItem.getSample();
            if (sample != null) {
                dto.setSampleId(Integer.valueOf(sample.getId()));
                dto.setReceiptDate(sample.getReceivedTimestamp());
                dto.setAccessionNumber(sample.getAccessionNumber());

                // Use sample status - for now default to REGISTERED
                dto.setStatus("REGISTERED");
            }

            populateQuantityFields(dto, sampleItem);
        }

        // Set retention policy fields
        dto.setRetentionPolicyId(bioSample.getRetentionPolicyId());
        dto.setRetentionExpiryDate(bioSample.getRetentionExpiryDate());

        // Fetch policy name if retention policy ID is set
        if (bioSample.getRetentionPolicyId() != null) {
            RetentionPolicy policy = retentionPolicyService.get(bioSample.getRetentionPolicyId());
            if (policy != null) {
                dto.setRetentionPolicyName(policy.getPolicyName());
            }
        }

        // Set workflow status
        dto.setWorkflowStatus(
                bioSample.getWorkflowStatus() != null ? bioSample.getWorkflowStatus().name() : "REGISTERED");

        return dto;
    }

    private void populateQuantityFields(BioSampleListDTO dto, SampleItem sampleItem) {
        if (sampleItem == null || dto == null) {
            return;
        }
        if (sampleItem.getQuantity() != null) {
            dto.setQuantity(sampleItem.getQuantity());
        }
        if (sampleItem.getEffectiveRemainingQuantity() != null) {
            dto.setRemainingQuantity(sampleItem.getEffectiveRemainingQuantity().doubleValue());
        }
        if (sampleItem.getUnitOfMeasureName() != null && !sampleItem.getUnitOfMeasureName().isBlank()) {
            dto.setUnitOfMeasure(sampleItem.getUnitOfMeasureName());
        }
    }

    /**
     * Generate a unique barcode for a new sample. Format: BIO-YYYYMMDD-XXXXX where
     * XXXXX is a random alphanumeric sequence.
     */
    @GetMapping(value = "/generate-barcode", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> generateBarcode() {
        String datePart = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        String barcode = "BIO-" + datePart + "-" + randomPart;

        return ResponseEntity.ok(Map.of("barcode", barcode));
    }

    /**
     * Register a new sample. Creates Sample, SampleItem, and BioSample extension
     * records.
     */
    @PostMapping(value = "/register", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<?> registerSample(@RequestBody SampleRegistrationRequest request,
            HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found. Please log in again."));
        }

        RegistrationDepartmentResult departmentResult = resolveRegistrationDepartment(httpRequest,
                request.getDepartmentTestSectionId(), request.getProjectId());
        if (departmentResult.errorResponse != null) {
            return departmentResult.errorResponse;
        }
        if (!rbacPermissionService.hasPermission(httpRequest, RbacAction.REGISTER_SAMPLES)) {
            return ResponseEntity.status(403).body(Map.of("error", "Insufficient permission to register samples"));
        }

        try {
            // Validate required fields
            if (request.getBarcode() == null || request.getBarcode().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Barcode is required"));
            }
            if (request.getSampleTypeId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Sample type is required"));
            }

            // Get sample type
            TypeOfSample sampleType = typeOfSampleService.get(request.getSampleTypeId());
            if (sampleType == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid sample type ID"));
            }

            // Create Sample (accession-level record)
            Timestamp receiptTimestamp = request.getReceiptDate() != null ? Timestamp.valueOf(request.getReceiptDate())
                    : new Timestamp(System.currentTimeMillis());
            Sample savedSample = createSampleWithGeneratedAccession(receiptTimestamp, sysUserId);

            // Create SampleItem (aliquot-level record)
            SampleItem sampleItem = new SampleItem();
            sampleItem.setSample(savedSample);
            sampleItem.setExternalId(request.getBarcode().trim());
            sampleItem.setTypeOfSample(sampleType);
            sampleItem.setSortOrder("1");
            sampleItem.setStatusId("1");
            if (request.getCollectionDate() != null) {
                sampleItem.setCollectionDate(Timestamp.valueOf(request.getCollectionDate()));
            }
            sampleItem.setSysUserId(sysUserId);
            SampleItem savedSampleItem = sampleItemService.save(sampleItem);

            // Create BioSample extension
            BioSample bioSample = new BioSample();
            bioSample.setBiosafetyLevel(
                    request.getBiosafetyLevel() != null ? BiosafetyLevel.valueOf(request.getBiosafetyLevel())
                            : BiosafetyLevel.BSL_1);
            bioSample.setEthicsApprovalRef(request.getEthicsApprovalRef());
            bioSample.setMtaReference(request.getMtaReference());
            bioSample.setSpecialHandling(mergeExternalIdIntoSpecialHandling(request.getSpecialHandling(),
                    request.getExternalId(), request.getBarcode()));
            bioSample.setOriginLab(request.getOriginLab());
            bioSample.setProjectId(request.getProjectId());
            bioSample.setDepartmentTestSectionId(departmentResult.departmentId);
            if (request.getRequiredTempMin() != null) {
                bioSample.setRequiredTempMin(BigDecimal.valueOf(request.getRequiredTempMin()));
            }
            if (request.getRequiredTempMax() != null) {
                bioSample.setRequiredTempMax(BigDecimal.valueOf(request.getRequiredTempMax()));
            }

            // Link to shipment if provided
            if (request.getShipmentId() != null) {
                Shipment shipment = shipmentService.get(request.getShipmentId());
                if (shipment != null) {
                    bioSample.setShipment(shipment);
                }
            }

            bioSample.setSysUserId(sysUserId);
            BioSample savedBioSample = bioSampleService.createForSampleItem(savedSampleItem, bioSample);

            return ResponseEntity.ok(Map.of("id", savedBioSample.getId(), "barcode", request.getBarcode(),
                    "sampleItemId", savedSampleItem.getId(), "sampleId", savedSample.getId()));

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to register sample: " + e.getMessage()));
        }
    }

    private Sample createSampleWithGeneratedAccession(Timestamp receiptTimestamp, String sysUserId) {
        Sample sample = new Sample();
        sample.setReceivedTimestamp(receiptTimestamp);
        sample.setEnteredDate(new java.sql.Date(receiptTimestamp.getTime()));
        sample.setDomain("H");
        sample.setSysUserId(sysUserId);

        AccessionNumberHandler accessionNumberHandler = new AccessionNumberHandler(sampleService, sampleDAO,
                entityManager, BioSampleRestController.class);
        String sampleId = accessionNumberHandler.generateAndInsertWithUniqueAccessionNumber(sample);
        return sampleService.get(sampleId);
    }

    /**
     * Request body for sample registration.
     */
    public static class SampleRegistrationRequest {
        private String barcode;
        private String externalId;
        private String originLab;
        private String sampleTypeId;
        private String receiptDate;
        private String collectionDate;
        private Double requiredTempMin;
        private Double requiredTempMax;
        private String projectId;
        private String biosafetyLevel;
        private String ethicsApprovalRef;
        private String mtaReference;
        private String specialHandling;
        private Integer shipmentId;
        private Integer departmentTestSectionId;

        public String getBarcode() {
            return barcode;
        }

        public void setBarcode(String barcode) {
            this.barcode = barcode;
        }

        public String getExternalId() {
            return externalId;
        }

        public void setExternalId(String externalId) {
            this.externalId = externalId;
        }

        public String getOriginLab() {
            return originLab;
        }

        public void setOriginLab(String originLab) {
            this.originLab = originLab;
        }

        public String getSampleTypeId() {
            return sampleTypeId;
        }

        public void setSampleTypeId(String sampleTypeId) {
            this.sampleTypeId = sampleTypeId;
        }

        public String getReceiptDate() {
            return receiptDate;
        }

        public void setReceiptDate(String receiptDate) {
            this.receiptDate = receiptDate;
        }

        public String getCollectionDate() {
            return collectionDate;
        }

        public void setCollectionDate(String collectionDate) {
            this.collectionDate = collectionDate;
        }

        public Double getRequiredTempMin() {
            return requiredTempMin;
        }

        public void setRequiredTempMin(Double requiredTempMin) {
            this.requiredTempMin = requiredTempMin;
        }

        public Double getRequiredTempMax() {
            return requiredTempMax;
        }

        public void setRequiredTempMax(Double requiredTempMax) {
            this.requiredTempMax = requiredTempMax;
        }

        public String getProjectId() {
            return projectId;
        }

        public void setProjectId(String projectId) {
            this.projectId = projectId;
        }

        public String getBiosafetyLevel() {
            return biosafetyLevel;
        }

        public void setBiosafetyLevel(String biosafetyLevel) {
            this.biosafetyLevel = biosafetyLevel;
        }

        public String getEthicsApprovalRef() {
            return ethicsApprovalRef;
        }

        public void setEthicsApprovalRef(String ethicsApprovalRef) {
            this.ethicsApprovalRef = ethicsApprovalRef;
        }

        public String getMtaReference() {
            return mtaReference;
        }

        public void setMtaReference(String mtaReference) {
            this.mtaReference = mtaReference;
        }

        public String getSpecialHandling() {
            return specialHandling;
        }

        public void setSpecialHandling(String specialHandling) {
            this.specialHandling = specialHandling;
        }

        public Integer getShipmentId() {
            return shipmentId;
        }

        public void setShipmentId(Integer shipmentId) {
            this.shipmentId = shipmentId;
        }

        public Integer getDepartmentTestSectionId() {
            return departmentTestSectionId;
        }

        public void setDepartmentTestSectionId(Integer departmentTestSectionId) {
            this.departmentTestSectionId = departmentTestSectionId;
        }
    }

    /**
     * Validate manifest data before import. Checks for duplicate barcodes, valid
     * sample types, and required fields.
     */
    @PostMapping(value = "/validate-manifest-import", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ManifestValidationResponse> validateManifestImport(@RequestBody ManifestImportRequest request,
            HttpServletRequest httpRequest) {

        ManifestValidationResponse response = new ManifestValidationResponse();
        response.setValid(true);

        List<SampleRegistrationDTO> samples = request.getSamples();
        if (samples == null || samples.isEmpty()) {
            ManifestValidationResponse.RowValidationResult row = new ManifestValidationResponse.RowValidationResult(0);
            row.addError("No samples provided in manifest");
            response.addRow(row);
            return ResponseEntity.ok(response);
        }

        Map<String, TypeOfSample> sampleTypeLookup = buildManifestSampleTypeLookup();

        List<String> manifestBarcodes = new ArrayList<>();
        for (SampleRegistrationDTO sample : samples) {
            String barcode = firstNonBlank(sample.getBarcode(), sample.getExternalId());
            if (barcode != null && !barcode.trim().isEmpty()) {
                manifestBarcodes.add(barcode.trim());
            }
        }
        Set<String> existingBarcodes = bioSampleService.findExistingBarcodes(manifestBarcodes);

        // Track barcodes within the manifest for duplicate detection
        Set<String> seenBarcodes = new HashSet<>();

        for (int i = 0; i < samples.size(); i++) {
            SampleRegistrationDTO sample = samples.get(i);
            ManifestValidationResponse.RowValidationResult rowResult = new ManifestValidationResponse.RowValidationResult(
                    i);

            // Validate required fields
            String barcode = firstNonBlank(sample.getBarcode(), sample.getExternalId());
            if (barcode == null || barcode.trim().isEmpty()) {
                rowResult.addError("Barcode is required");
            } else {
                barcode = barcode.trim();
                // Check for duplicates within the manifest
                if (seenBarcodes.contains(barcode)) {
                    rowResult.addError("Duplicate sample ID in manifest: " + barcode);
                } else {
                    seenBarcodes.add(barcode);
                }
                // Check for existing barcode in database
                if (existingBarcodes.contains(barcode)) {
                    rowResult.addError("Sample ID already exists: " + barcode);
                }
            }

            // Validate sample type
            String sampleTypeId = sample.getSampleTypeId();
            if (sampleTypeId == null || sampleTypeId.trim().isEmpty()) {
                rowResult.addError("Sample type is required");
            } else {
                TypeOfSample sampleType = findSampleTypeInLookup(sampleTypeLookup, sampleTypeId.trim());
                if (sampleType == null) {
                    if (isManifestSampleTypeAutoCreatable(sampleTypeId)) {
                        rowResult.addWarning("New biorepository sample type will be created: "
                                + normalizeSampleTypeLabel(sampleTypeId));
                    } else {
                        rowResult.addError("Invalid sample type: " + sampleTypeId);
                    }
                }
            }

            // Validate biosafety level
            String bsl = sample.getBiosafetyLevel();
            if (bsl != null && !bsl.trim().isEmpty()) {
                try {
                    BiosafetyLevel.valueOf(bsl.trim());
                } catch (IllegalArgumentException e) {
                    rowResult.addError("Invalid biosafety level: " + bsl + ". Must be BSL_1, BSL_2, BSL_3, or BSL_4");
                }
            }

            // Validate origin lab
            if (sample.getOriginLab() == null || sample.getOriginLab().trim().isEmpty()) {
                rowResult.addError("Origin lab is required");
            }

            if (sample.getReceiptDate() == null || sample.getReceiptDate().trim().isEmpty()) {
                rowResult.addError("Receipt date is required");
            } else if (!isValidDateFormat(sample.getReceiptDate().trim())) {
                rowResult.addError("Invalid receipt date format: " + sample.getReceiptDate());
            }

            if (sample.getRequiredTempMin() == null) {
                rowResult.addError("Minimum storage temperature is required");
            }

            if (sample.getRequiredTempMax() == null) {
                rowResult.addError("Maximum storage temperature is required");
            }

            if (sample.getRequiredTempMin() != null && sample.getRequiredTempMax() != null
                    && sample.getRequiredTempMin().compareTo(sample.getRequiredTempMax()) > 0) {
                rowResult.addError(
                        "Maximum storage temperature must be greater than or equal to minimum storage temperature");
            }

            // Validate collection date format (if provided)
            String collectionDate = sample.getCollectionDate();
            if (collectionDate != null && !collectionDate.trim().isEmpty()) {
                if (!isValidDateFormat(collectionDate.trim())) {
                    rowResult.addError("Invalid collection date format: " + collectionDate);
                }
            }

            response.addRow(rowResult);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Bulk register samples from a validated manifest.
     */
    @PostMapping(value = "/register-bulk", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BulkRegistrationResponse> registerBulk(@RequestBody ManifestImportRequest request,
            HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            BulkRegistrationResponse errorResponse = new BulkRegistrationResponse();
            errorResponse.setSuccess(false);
            errorResponse.setError("User session not found. Please log in again.");
            return ResponseEntity.status(401).body(errorResponse);
        }
        RegistrationDepartmentResult departmentResult = resolveRegistrationDepartment(httpRequest,
                request.getDepartmentTestSectionId(), null);
        if (departmentResult.errorResponse != null) {
            BulkRegistrationResponse errorResponse = new BulkRegistrationResponse();
            errorResponse.setSuccess(false);
            if (departmentResult.errorResponse.getStatusCode() == HttpStatus.FORBIDDEN) {
                errorResponse.setError("Select a department first.");
                return ResponseEntity.status(403).body(errorResponse);
            }
            Object body = departmentResult.errorResponse.getBody();
            errorResponse.setError(body instanceof Map<?, ?> map && map.get("error") != null
                    ? String.valueOf(map.get("error"))
                    : "Select a department first.");
            return ResponseEntity.status(departmentResult.errorResponse.getStatusCode()).body(errorResponse);
        }
        if (!rbacPermissionService.hasPermission(httpRequest, RbacAction.REGISTER_SAMPLES)) {
            BulkRegistrationResponse errorResponse = new BulkRegistrationResponse();
            errorResponse.setSuccess(false);
            errorResponse.setError("Insufficient permission to register samples");
            return ResponseEntity.status(403).body(errorResponse);
        }

        BulkRegistrationResponse response = new BulkRegistrationResponse();
        response.setSuccess(true);

        List<SampleRegistrationDTO> samples = request.getSamples();
        if (samples == null || samples.isEmpty()) {
            response.setSuccess(false);
            response.setError("No samples provided");
            return ResponseEntity.badRequest().body(response);
        }

        TransactionTemplate rowTransaction = new TransactionTemplate(transactionManager);
        rowTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        for (SampleRegistrationDTO dto : samples) {
            try {
                if (dto.getProjectId() != null && !dto.getProjectId().isBlank()
                        && !departmentIsolationService.isInventoryProjectConsistent(departmentResult.departmentId,
                                dto.getProjectId())) {
                    String sampleRef = firstNonBlank(dto.getBarcode(), dto.getExternalId());
                    String prefix = (sampleRef == null || sampleRef.isBlank()) ? "Sample"
                            : "Sample '" + sampleRef + "'";
                    response.addRowError(prefix + ": Selected project belongs to a different department.");
                    continue;
                }
                BulkRegistrationResponse.RegisteredSample registered = rowTransaction.execute(status -> registerSingleSample(
                        dto, request.getShipmentId(), sysUserId, departmentResult.departmentId));

                if (registered != null) {
                    response.addSample(registered);
                } else {
                    response.addRowError("Sample registration returned no result");
                }
            } catch (Exception e) {
                String sampleRef = firstNonBlank(dto.getBarcode(), dto.getExternalId());
                String prefix = (sampleRef == null || sampleRef.isBlank()) ? "Sample" : "Sample '" + sampleRef + "'";
                String detailedError = getRootCauseMessage(e);
                logger.error("Bulk manifest import failed for {}", prefix, e);
                response.addRowError(prefix + ": " + detailedError);
            }
        }

        if (response.getRegisteredCount() == 0) {
            response.setSuccess(false);
            String errorSummary = response.getRowErrors().isEmpty() ? "Failed to register samples"
                    : response.getRowErrors().stream().limit(3).collect(Collectors.joining("; "));
            response.setError("Failed to register samples: " + errorSummary);
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Register a single sample from the manifest DTO.
     */
    private BulkRegistrationResponse.RegisteredSample registerSingleSample(SampleRegistrationDTO dto,
            Integer shipmentId, String sysUserId, Integer departmentTestSectionId) {
        String barcode = firstNonBlank(dto.getBarcode(), dto.getExternalId());
        if (barcode == null || barcode.isBlank()) {
            barcode = generateBarcode().getBody().get("barcode");
        }

        // Find or create sample type by name or ID for biorepository intake.
        TypeOfSample sampleType = resolveManifestSampleType(dto.getSampleTypeId(), sysUserId, true);
        if (sampleType == null) {
            throw new IllegalArgumentException("Invalid sample type: " + dto.getSampleTypeId());
        }

        // Create Sample (accession-level record)
        Timestamp receiptTimestamp = dto.getReceiptDate() != null && !dto.getReceiptDate().trim().isEmpty()
                ? parseDate(dto.getReceiptDate().trim())
                : null;
        if (receiptTimestamp == null) {
            receiptTimestamp = new Timestamp(System.currentTimeMillis());
        }
        Sample savedSample = createSampleWithGeneratedAccession(receiptTimestamp, sysUserId);

        // Create SampleItem (aliquot-level record)
        SampleItem sampleItem = new SampleItem();
        sampleItem.setSample(savedSample);
        sampleItem.setExternalId(barcode);
        sampleItem.setTypeOfSample(sampleType);
        sampleItem.setSortOrder("1");
        sampleItem.setStatusId("1");
        if (dto.getCollectionDate() != null && !dto.getCollectionDate().trim().isEmpty()) {
            Timestamp collectionTs = parseDate(dto.getCollectionDate().trim());
            if (collectionTs != null) {
                sampleItem.setCollectionDate(collectionTs);
            }
        }
        sampleItem.setSysUserId(sysUserId);
        SampleItem savedSampleItem = sampleItemService.save(sampleItem);

        // Create BioSample extension
        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(dto.getBiosafetyLevel() != null ? BiosafetyLevel.valueOf(dto.getBiosafetyLevel())
                : BiosafetyLevel.BSL_1);
        bioSample.setEthicsApprovalRef(dto.getEthicsApprovalRef());
        bioSample.setMtaReference(dto.getMtaReference());
        bioSample.setConsentId(dto.getConsentId());
        bioSample.setPrincipalInvestigator(dto.getPrincipalInvestigator());
        bioSample.setPreservationMedium(dto.getPreservationMedium());
        bioSample.setArrivalCondition(dto.getArrivalCondition());
        bioSample.setSpecialHandling(
                mergeExternalIdIntoSpecialHandling(dto.getSpecialHandling(), dto.getExternalId(), barcode));
        bioSample.setOriginLab(dto.getOriginLab());
        bioSample.setProjectId(dto.getProjectId());
        bioSample.setDepartmentTestSectionId(departmentTestSectionId);
        if (dto.getRequiredTempMin() != null) {
            bioSample.setRequiredTempMin(dto.getRequiredTempMin());
        }
        if (dto.getRequiredTempMax() != null) {
            bioSample.setRequiredTempMax(dto.getRequiredTempMax());
        }
        if (shipmentId != null) {
            Shipment shipment = shipmentService.get(shipmentId);
            if (shipment != null) {
                bioSample.setShipment(shipment);
            }
        }
        bioSample.setSysUserId(sysUserId);
        BioSample savedBioSample = bioSampleService.createForSampleItem(savedSampleItem, bioSample);

        // Force SQL execution in this row transaction so DB constraint failures surface
        // with a specific cause.
        entityManager.flush();

        // Build response
        BulkRegistrationResponse.RegisteredSample registered = new BulkRegistrationResponse.RegisteredSample();
        registered.setId(savedBioSample.getId());
        registered.setBarcode(barcode);
        registered.setSampleItemId(Integer.valueOf(savedSampleItem.getId()));
        registered.setSampleId(Integer.valueOf(savedSample.getId()));
        return registered;
    }

    private String getRootCauseMessage(Throwable throwable) {
        if (throwable == null) {
            return "Unknown error";
        }

        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }

        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            message = throwable.getMessage();
        }

        return (message == null || message.isBlank()) ? "Unknown error" : message;
    }

    private Map<String, TypeOfSample> buildManifestSampleTypeLookup() {
        Map<String, TypeOfSample> lookup = new HashMap<>();
        List<TypeOfSample> allTypes = typeOfSampleService.getAllTypeOfSamples();
        for (TypeOfSample type : allTypes) {
            if (type.getId() != null) {
                lookup.putIfAbsent(type.getId(), type);
            }
            String description = normalizeSampleTypeLabel(type.getDescription());
            if (!description.isBlank()) {
                lookup.putIfAbsent(description.toLowerCase(Locale.ROOT), type);
            }
            String localizedName = normalizeSampleTypeLabel(type.getLocalizedName());
            if (!localizedName.isBlank()) {
                lookup.putIfAbsent(localizedName.toLowerCase(Locale.ROOT), type);
            }
            if (type.getLocalAbbreviation() != null && !type.getLocalAbbreviation().isBlank()) {
                lookup.putIfAbsent(type.getLocalAbbreviation().toLowerCase(Locale.ROOT), type);
            }
        }
        return lookup;
    }

    private TypeOfSample findSampleTypeInLookup(Map<String, TypeOfSample> lookup, String nameOrId) {
        if (nameOrId == null || nameOrId.trim().isEmpty()) {
            return null;
        }
        nameOrId = nameOrId.trim();
        String normalizedInput = normalizeSampleTypeLabel(nameOrId);

        if (nameOrId.matches("^\\d+$")) {
            try {
                TypeOfSample byId = typeOfSampleService.get(nameOrId);
                if (byId != null) {
                    return byId;
                }
            } catch (Exception e) {
                // Fall through to map lookup.
            }
            TypeOfSample fromLookup = lookup.get(nameOrId);
            if (fromLookup != null) {
                return fromLookup;
            }
        }

        TypeOfSample match = lookup.get(normalizedInput.toLowerCase(Locale.ROOT));
        if (match != null) {
            return match;
        }
        return lookup.get(nameOrId.toLowerCase(Locale.ROOT));
    }

    /**
     * Find sample type by name or ID.
     */
    private TypeOfSample findSampleTypeByNameOrId(String nameOrId) {
        if (nameOrId == null || nameOrId.trim().isEmpty()) {
            return null;
        }
        nameOrId = nameOrId.trim();
        String normalizedInput = normalizeSampleTypeLabel(nameOrId);

        // Only query by ID when input is numeric.
        // Passing labels (e.g. "Plasma") to ID lookup can poison the transaction
        // on some Hibernate/user-type paths.
        if (nameOrId.matches("^\\d+$")) {
            try {
                TypeOfSample byId = typeOfSampleService.get(nameOrId);
                if (byId != null) {
                    return byId;
                }
            } catch (Exception e) {
                // Fall through to name-based lookup.
            }
        }

        // Try to find by description (name)
        List<TypeOfSample> allTypes = typeOfSampleService.getAllTypeOfSamples();
        for (TypeOfSample type : allTypes) {
            if (matchesSampleTypeIdentifier(nameOrId, normalizedInput, type)) {
                return type;
            }
        }

        return null;
    }

    private boolean isNumericIdentifier(String value) {
        return value != null && value.matches("\\d+");
    }

    private TypeOfSample resolveManifestSampleType(String nameOrId, String sysUserId, boolean createIfMissing) {
        TypeOfSample existingType = findSampleTypeByNameOrId(nameOrId);
        if (existingType != null) {
            ensureBiorepositoryApprovedSampleType(existingType, sysUserId);
            return existingType;
        }

        if (!createIfMissing || !isManifestSampleTypeAutoCreatable(nameOrId)) {
            return null;
        }

        return createManifestSampleType(nameOrId, sysUserId);
    }

    private TypeOfSample createManifestSampleType(String requestedType, String sysUserId) {
        String normalizedType = normalizeSampleTypeLabel(requestedType);
        if (normalizedType == null || normalizedType.isBlank()) {
            return null;
        }

        TypeOfSample existing = findSampleTypeByNameOrId(normalizedType);
        if (existing != null) {
            ensureBiorepositoryApprovedSampleType(existing, sysUserId);
            return existing;
        }

        TypeOfSample typeOfSample = new TypeOfSample();
        typeOfSample.setDescription(normalizedType);
        typeOfSample.setDomain("H");
        typeOfSample.setLocalAbbreviation(generateUniqueSampleTypeAbbreviation(normalizedType));
        typeOfSample.setIsActive(true);
        typeOfSample.setSortOrder(Integer.MAX_VALUE);
        typeOfSample.setSysUserId(sysUserId);
        typeOfSample.setNameKey("Sample.type." + normalizedType.replaceAll("[^A-Za-z0-9]+", "_"));
        typeOfSample.setLocalization(createSampleTypeLocalization(normalizedType, sysUserId));

        TypeOfSample saved = typeOfSampleService.save(typeOfSample);
        typeOfSampleService.clearCache();
        refreshSampleTypeLists();
        ensureBiorepositoryApprovedSampleType(saved, sysUserId);
        return saved;
    }

    private void ensureBiorepositoryApprovedSampleType(TypeOfSample sampleType, String sysUserId) {
        if (sampleType == null || sampleType.getId() == null || sysUserId == null) {
            return;
        }

        if (approvedSampleTypeService.getByTypeOfSampleId(sampleType.getId()) != null) {
            return;
        }

        BiorepositoryApprovedSampleType approvedType = new BiorepositoryApprovedSampleType();
        approvedType.setTypeOfSample(sampleType);
        approvedType.setCategory(determineBiorepositorySampleCategory(sampleType.getDescription()));
        approvedType.setIsActive(true);
        approvedType.setDisplayOrder((int) approvedSampleTypeService.countActive() + 1);
        approvedType.setSysUserId(sysUserId);
        approvedSampleTypeService.save(approvedType);
    }

    private SampleCategory determineBiorepositorySampleCategory(String sampleTypeName) {
        String normalized = normalizeSampleTypeLabel(sampleTypeName).toLowerCase(Locale.ROOT);

        if (normalized.contains("serum") || normalized.contains("plasma") || normalized.contains("blood")
                || normalized.contains("buffy")) {
            return SampleCategory.BLOOD_DERIVED;
        }
        if (normalized.contains("dna") || normalized.contains("rna") || normalized.contains("cdna")
                || normalized.contains("nucleic")) {
            return SampleCategory.NUCLEIC_ACIDS;
        }
        if (normalized.contains("tissue") || normalized.contains("biopsy") || normalized.contains("ffpe")
                || normalized.contains("block")) {
            return SampleCategory.TISSUE;
        }
        if (normalized.contains("cell") || normalized.contains("pbmc")) {
            return SampleCategory.CELLULAR;
        }
        if (normalized.contains("isolate") || normalized.contains("culture") || normalized.contains("bacteria")
                || normalized.contains("bacterial") || normalized.contains("viral") || normalized.contains("fungal")) {
            return SampleCategory.MICROBIOLOGICAL;
        }

        return SampleCategory.OTHER;
    }

    private boolean matchesSampleTypeIdentifier(String rawInput, String normalizedInput, TypeOfSample type) {
        return normalizedInput.equalsIgnoreCase(normalizeSampleTypeLabel(type.getDescription()))
                || normalizedInput.equalsIgnoreCase(normalizeSampleTypeLabel(type.getLocalizedName()))
                || rawInput.equalsIgnoreCase(type.getLocalAbbreviation());
    }

    private Set<String> resolveSampleTypeIdsForSearch(String sampleTypeQuery) {
        if (sampleTypeQuery == null || sampleTypeQuery.trim().isEmpty()) {
            return Set.of();
        }
        String trimmed = sampleTypeQuery.trim();
        String normalizedQuery = normalizeSampleTypeLabel(trimmed).toLowerCase(Locale.ROOT);
        Set<String> matchingIds = new HashSet<>();

        TypeOfSample exactMatch = findSampleTypeByNameOrId(trimmed);
        if (exactMatch != null && exactMatch.getId() != null) {
            matchingIds.add(exactMatch.getId());
        }

        for (TypeOfSample type : typeOfSampleService.getAllTypeOfSamples()) {
            if (type.getId() == null) {
                continue;
            }
            String description = normalizeSampleTypeLabel(type.getDescription()).toLowerCase(Locale.ROOT);
            String localizedName = normalizeSampleTypeLabel(type.getLocalizedName()).toLowerCase(Locale.ROOT);
            if (description.contains(normalizedQuery) || localizedName.contains(normalizedQuery)
                    || matchesSampleTypeIdentifier(trimmed, normalizedQuery, type)) {
                matchingIds.add(type.getId());
            }
        }
        return matchingIds;
    }

    private boolean isManifestSampleTypeAutoCreatable(String sampleType) {
        if (sampleType == null) {
            return false;
        }

        String normalized = normalizeSampleTypeLabel(sampleType);
        if (normalized == null || normalized.isBlank()) {
            return false;
        }

        return !normalized.matches("^\\d+$");
    }

    private String normalizeSampleTypeLabel(String sampleType) {
        if (sampleType == null) {
            return "";
        }

        return sampleType.trim().replaceAll("\\s+", " ");
    }

    private String generateUniqueSampleTypeAbbreviation(String sampleTypeName) {
        String cleaned = sampleTypeName.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "");
        if (cleaned.isBlank()) {
            cleaned = "BIOSAMPLE";
        }

        String base = cleaned.substring(0, Math.min(10, cleaned.length()));
        String candidate = base;
        int suffix = 1;

        while (typeOfSampleService.getTypeOfSampleByLocalAbbrevAndDomain(candidate, "H") != null) {
            String suffixText = Integer.toString(suffix++);
            int maxBaseLength = Math.max(1, 10 - suffixText.length());
            String baseCandidate = cleaned.substring(0, Math.min(maxBaseLength, cleaned.length()));
            candidate = baseCandidate + suffixText;
        }

        return candidate;
    }

    private Localization createSampleTypeLocalization(String sampleTypeName, String sysUserId) {
        Localization localization = new Localization();
        localization.setEnglish(sampleTypeName);
        localization.setFrench(sampleTypeName);
        localization.setDescription("type of sample name");
        localization.setSysUserId(sysUserId);
        return localizationService.save(localization);
    }

    private void refreshSampleTypeLists() {
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.SAMPLE_TYPE);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.SAMPLE_TYPE_ACTIVE);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.SAMPLE_TYPE_INACTIVE);
    }

    // ========== RETENTION & DISPOSAL ENDPOINTS ==========

    /**
     * Get samples approaching or past retention expiry for the disposal dashboard.
     *
     * GET /rest/biorepository/sample/expiring
     *
     * Query params: - status: "expired" (already past expiry) or "expiring"
     * (approaching expiry) - days: number of days window (default 30, only for
     * status=expiring)
     *
     * Returns list of BioSampleListDTO with retention policy details.
     */
    @GetMapping(value = "/expiring", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<BioSampleListDTO>> getExpiringSamples(
            @RequestParam(required = false, defaultValue = "expiring") String status,
            @RequestParam(required = false, defaultValue = "30") Integer days, HttpServletRequest request) {

        List<BioSample> samples;

        if ("expired".equalsIgnoreCase(status)) {
            samples = bioSampleService.getExpiredSamples();
        } else if ("all".equalsIgnoreCase(status)) {
            samples = bioSampleService.getSamplesForDisposalDashboard();
        } else {
            // Default to expiring within N days
            samples = bioSampleService.getExpiringSamples(days);
        }

        samples = filterAccessibleBioSamples(samples, request);

        List<BioSampleListDTO> dtos = samples.stream().map(this::convertToListDTO).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * Dispose a biorepository sample. Updates both SampleItem status and BioSample
     * workflowStatus to DISPOSED.
     *
     * POST /rest/biorepository/sample/dispose
     *
     * @param request contains sampleItemId, reason, method, and optional notes
     * @return disposal result including audit trail ID
     */
    @PostMapping(value = "/dispose", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> disposeSample(@RequestBody DisposalRequest request,
            HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        // Validate required fields
        if (request.getSampleItemId() == null || request.getSampleItemId().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Sample item ID is required"));
        }
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Disposal reason is required"));
        }
        if (request.getMethod() == null || request.getMethod().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Disposal method is required"));
        }

        try {
            if (!departmentIsolationService.canAccessSampleItemIdentifier(request.getSampleItemId(), httpRequest)) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }
            if (!rbacPermissionService.hasPermission(httpRequest, RbacAction.UPDATE_SAMPLES)) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "Insufficient permission to update samples"));
            }
            Map<String, Object> result = bioSampleService.disposeBioSample(request.getSampleItemId(),
                    request.getReason(), request.getMethod(), request.getNotes());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Request body for sample disposal.
     */
    public static class DisposalRequest {
        private String sampleItemId;
        private String reason;
        private String method;
        private String notes;

        public String getSampleItemId() {
            return sampleItemId;
        }

        public void setSampleItemId(String sampleItemId) {
            this.sampleItemId = sampleItemId;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    /**
     * Check if date string is in valid format.
     */
    private boolean isValidDateFormat(String dateStr) {
        return parseDate(dateStr) != null;
    }

    /**
     * Parse date string to Timestamp.
     */
    private Timestamp parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        String normalizedDate = dateStr.trim().replace("T", " ");
        String[] dateTimePatterns = { "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "dd/MM/yyyy HH:mm:ss",
                "dd/MM/yyyy HH:mm", "dd-MM-yyyy HH:mm:ss", "dd-MM-yyyy HH:mm", "dd.MM.yyyy HH:mm:ss",
                "dd.MM.yyyy HH:mm" };

        for (String pattern : dateTimePatterns) {
            try {
                LocalDateTime parsedDate = LocalDateTime.parse(normalizedDate, DateTimeFormatter.ofPattern(pattern));
                return Timestamp.valueOf(parsedDate);
            } catch (DateTimeParseException e) {
                // Try the next pattern
            }
        }

        String[] datePatterns = { "yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy", "dd.MM.yyyy" };
        for (String pattern : datePatterns) {
            try {
                LocalDate parsedDate = LocalDate.parse(normalizedDate, DateTimeFormatter.ofPattern(pattern));
                return Timestamp.valueOf(parsedDate.atStartOfDay());
            } catch (DateTimeParseException e) {
                // Try the next pattern
            }
        }

        return null;
    }

    private RegistrationDepartmentResult resolveRegistrationDepartment(HttpServletRequest request,
            Integer explicitDepartmentId, String projectRef) {
        Integer departmentId = departmentIsolationService.resolveDepartmentForScopedCreate(request,
                explicitDepartmentId);
        if (departmentId == null) {
            if (departmentIsolationService.hasUnrestrictedDepartmentAccess(request)) {
                return RegistrationDepartmentResult.error(
                        ResponseEntity.badRequest().body(Map.of("error", "Select a department (departmentTestSectionId).")));
            }
            if (departmentIsolationService.getRestrictedUserTestSectionIds(request).isEmpty()) {
                return RegistrationDepartmentResult.error(
                        ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Select a department first.")));
            }
            return RegistrationDepartmentResult
                    .error(ResponseEntity.badRequest().body(Map.of("error", "Select a department first.")));
        }
        if (!departmentIsolationService.canAccessDepartmentScopedLocation(departmentId, request)) {
            return RegistrationDepartmentResult
                    .error(ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied")));
        }
        if (projectRef != null && !projectRef.isBlank()
                && !departmentIsolationService.isInventoryProjectConsistent(departmentId, projectRef)) {
            return RegistrationDepartmentResult.error(ResponseEntity.badRequest().body(Map.of("error",
                    "Selected linked notebook / project belongs to a different department.")));
        }
        return RegistrationDepartmentResult.ok(departmentId);
    }

    private static final class RegistrationDepartmentResult {
        private final Integer departmentId;
        private final ResponseEntity<?> errorResponse;

        private RegistrationDepartmentResult(Integer departmentId, ResponseEntity<?> errorResponse) {
            this.departmentId = departmentId;
            this.errorResponse = errorResponse;
        }

        private static RegistrationDepartmentResult ok(Integer departmentId) {
            return new RegistrationDepartmentResult(departmentId, null);
        }

        private static RegistrationDepartmentResult error(ResponseEntity<?> errorResponse) {
            return new RegistrationDepartmentResult(null, errorResponse);
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }

        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }

        return null;
    }

    private String mergeExternalIdIntoSpecialHandling(String specialHandling, String externalId, String barcode) {
        String normalizedExternalId = firstNonBlank(externalId);
        if (normalizedExternalId == null || normalizedExternalId.equals(firstNonBlank(barcode))) {
            return specialHandling;
        }

        String externalIdNote = "External ID: " + normalizedExternalId;
        if (specialHandling == null || specialHandling.trim().isEmpty()) {
            return externalIdNote;
        }

        if (specialHandling.contains(externalIdNote)) {
            return specialHandling;
        }

        return specialHandling + " | " + externalIdNote;
    }
}
