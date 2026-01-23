package org.openelisglobal.biorepository.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openelisglobal.biorepository.controller.rest.dto.BioSampleListDTO;
import org.openelisglobal.biorepository.controller.rest.dto.BulkRegistrationResponse;
import org.openelisglobal.biorepository.controller.rest.dto.ManifestImportRequest;
import org.openelisglobal.biorepository.controller.rest.dto.ManifestValidationResponse;
import org.openelisglobal.biorepository.controller.rest.dto.SampleRegistrationDTO;
import org.openelisglobal.biorepository.service.BioSampleService;
import org.openelisglobal.biorepository.service.BiorepositoryApprovedSampleTypeService;
import org.openelisglobal.biorepository.service.RetentionPolicyService;
import org.openelisglobal.biorepository.service.ShipmentService;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BioSample.BiosafetyLevel;
import org.openelisglobal.biorepository.valueholder.RetentionPolicy;
import org.openelisglobal.biorepository.valueholder.Shipment;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @Autowired
    private BioSampleService bioSampleService;

    @Autowired
    private BiorepositoryApprovedSampleTypeService approvedSampleTypeService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private RetentionPolicyService retentionPolicyService;

    /**
     * Get a BioSample extension by ID.
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BioSample> getBioSample(@PathVariable("id") Integer id) {
        try {
            BioSample bioSample = bioSampleService.get(id);
            if (bioSample == null) {
                return ResponseEntity.notFound().build();
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
    public ResponseEntity<BioSample> getBySampleItemId(@PathVariable("sampleItemId") Integer sampleItemId) {
        BioSample bioSample = bioSampleService.getBySampleItemId(sampleItemId);
        if (bioSample == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(bioSample);
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
    public ResponseEntity<?> getByBarcode(@PathVariable("barcode") String barcode) {
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
    public ResponseEntity<List<BioSample>> getByShipmentId(@PathVariable("shipmentId") Integer shipmentId) {
        List<BioSample> bioSamples = bioSampleService.getByShipmentId(shipmentId);
        return ResponseEntity.ok(bioSamples);
    }

    /**
     * Get BioSample extensions by biosafety level.
     */
    @GetMapping(value = "/by-biosafety-level", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<BioSample>> getByBiosafetyLevel(@RequestParam BiosafetyLevel biosafetyLevel) {
        List<BioSample> bioSamples = bioSampleService.getByBiosafetyLevel(biosafetyLevel);
        return ResponseEntity.ok(bioSamples);
    }

    /**
     * Get BioSample extensions by ethics approval reference.
     */
    @GetMapping(value = "/by-ethics-ref", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<BioSample>> getByEthicsApprovalRef(@RequestParam String ethicsApprovalRef) {
        List<BioSample> bioSamples = bioSampleService.getByEthicsApprovalRef(ethicsApprovalRef);
        return ResponseEntity.ok(bioSamples);
    }

    /**
     * Get BioSample extensions by MTA reference.
     */
    @GetMapping(value = "/by-mta-ref", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<BioSample>> getByMtaReference(@RequestParam String mtaReference) {
        List<BioSample> bioSamples = bioSampleService.getByMtaReference(mtaReference);
        return ResponseEntity.ok(bioSamples);
    }

    /**
     * Get BioSample extensions by principal investigator.
     */
    @GetMapping(value = "/by-pi", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<BioSample>> getByPrincipalInvestigator(@RequestParam String principalInvestigator) {
        List<BioSample> bioSamples = bioSampleService.getByPrincipalInvestigator(principalInvestigator);
        return ResponseEntity.ok(bioSamples);
    }

    /**
     * Count BioSample extensions by shipment.
     */
    @GetMapping(value = "/count/by-shipment/{shipmentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Long>> countByShipmentId(@PathVariable("shipmentId") Integer shipmentId) {
        long count = bioSampleService.countByShipmentId(shipmentId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Count BioSample extensions by biosafety level.
     */
    @GetMapping(value = "/count/by-biosafety-level", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Long>> countByBiosafetyLevel(@RequestParam BiosafetyLevel biosafetyLevel) {
        long count = bioSampleService.countByBiosafetyLevel(biosafetyLevel);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Check if a BioSample extension exists for a SampleItem.
     */
    @GetMapping(value = "/exists/by-sample-item/{sampleItemId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Boolean>> existsBySampleItemId(
            @PathVariable("sampleItemId") Integer sampleItemId) {
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
    public ResponseEntity<Map<String, Long>> getBiosafetyStats() {
        Map<String, Long> stats = Map.of("BSL_1", bioSampleService.countByBiosafetyLevel(BiosafetyLevel.BSL_1), "BSL_2",
                bioSampleService.countByBiosafetyLevel(BiosafetyLevel.BSL_2), "BSL_3",
                bioSampleService.countByBiosafetyLevel(BiosafetyLevel.BSL_3), "BSL_4",
                bioSampleService.countByBiosafetyLevel(BiosafetyLevel.BSL_4));

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
            @RequestParam(required = false) String workflowStatus) {

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

        List<BioSampleListDTO> result = new ArrayList<>();
        for (BioSample bioSample : bioSamples) {
            result.add(convertToListDTO(bioSample));
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Search for biorepository samples using specific query parameters. At least
     * one search parameter (barcode, originLab, or projectId) must be provided.
     *
     * GET /rest/biorepository/sample/search?barcode=BIO-2026-015&status=STORED GET
     * /rest/biorepository/sample/search?originLab=National%20Lab&status=STORED GET
     * /rest/biorepository/sample/search?projectId=COVID-2026&status=STORED
     *
     * @param barcode   exact barcode (external_id) to search for
     * @param originLab exact origin lab name to filter by
     * @param projectId exact project ID to filter by
     * @param status    optional workflow status filter (e.g., STORED for retrieval)
     * @param limit     maximum results (default 50)
     * @return list of matching samples as BioSampleListDTO
     */
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<BioSampleListDTO>> searchSamples(@RequestParam(required = false) String barcode,
            @RequestParam(required = false) String originLab, @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "50") Integer limit) {

        // At least one search parameter is required
        boolean hasBarcode = barcode != null && !barcode.trim().isEmpty();
        boolean hasOriginLab = originLab != null && !originLab.trim().isEmpty();
        boolean hasProjectId = projectId != null && !projectId.trim().isEmpty();

        if (!hasBarcode && !hasOriginLab && !hasProjectId) {
            return ResponseEntity.ok(new ArrayList<>());
        }

        // Parse workflow status filter
        BioSample.WorkflowStatus filterStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            filterStatus = BioSample.WorkflowStatus.fromString(status);
        }

        List<BioSampleListDTO> result = new ArrayList<>();

        // Search by barcode (exact match on external_id)
        if (hasBarcode) {
            List<SampleItem> sampleItems = sampleItemService.getSampleItemsByExternalID(barcode.trim());
            for (SampleItem sampleItem : sampleItems) {
                if (result.size() >= limit) {
                    break;
                }
                BioSampleListDTO dto = buildSearchResultDTO(sampleItem, filterStatus);
                if (dto != null) {
                    result.add(dto);
                }
            }
        }

        // Search by origin lab (exact match)
        if (hasOriginLab && result.size() < limit) {
            List<BioSample> bioSamples = bioSampleService.getByOriginLab(originLab.trim());
            for (BioSample bioSample : bioSamples) {
                if (result.size() >= limit) {
                    break;
                }
                // Skip if already added by barcode search
                if (bioSample.getSampleItem() == null) {
                    continue;
                }
                Integer sampleItemId = Integer.valueOf(bioSample.getSampleItem().getId());
                boolean alreadyAdded = result.stream().anyMatch(d -> sampleItemId.equals(d.getSampleItemId()));
                if (alreadyAdded) {
                    continue;
                }
                BioSampleListDTO dto = buildSearchResultDTOFromBioSample(bioSample, filterStatus);
                if (dto != null) {
                    result.add(dto);
                }
            }
        }

        // Search by project ID (exact match)
        if (hasProjectId && result.size() < limit) {
            List<BioSample> bioSamples = bioSampleService.getByProjectId(projectId.trim());
            for (BioSample bioSample : bioSamples) {
                if (result.size() >= limit) {
                    break;
                }
                // Skip if already added
                if (bioSample.getSampleItem() == null) {
                    continue;
                }
                Integer sampleItemId = Integer.valueOf(bioSample.getSampleItem().getId());
                boolean alreadyAdded = result.stream().anyMatch(d -> sampleItemId.equals(d.getSampleItemId()));
                if (alreadyAdded) {
                    continue;
                }
                BioSampleListDTO dto = buildSearchResultDTOFromBioSample(bioSample, filterStatus);
                if (dto != null) {
                    result.add(dto);
                }
            }
        }

        return ResponseEntity.ok(result);
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

        return dto;
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
    public ResponseEntity<?> registerSample(@RequestBody SampleRegistrationRequest request,
            HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found. Please log in again."));
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
            Sample sample = new Sample();
            sample.setAccessionNumber(generateAccessionNumber());
            sample.setReceivedTimestamp(request.getReceiptDate() != null ? Timestamp.valueOf(request.getReceiptDate())
                    : new Timestamp(System.currentTimeMillis()));
            sample.setEnteredDate(new java.sql.Date(System.currentTimeMillis()));
            sample.setDomain("H"); // Human domain
            sample.setSysUserId(sysUserId);
            Sample savedSample = sampleService.save(sample);

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
            bioSample.setSpecialHandling(request.getSpecialHandling());
            bioSample.setOriginLab(request.getOriginLab());
            bioSample.setProjectId(request.getProjectId());
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

    /**
     * Generate a unique accession number for samples.
     */
    private String generateAccessionNumber() {
        String datePart = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.valueOf(System.currentTimeMillis() % 100000);
        return datePart + randomPart;
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

        // Track barcodes within the manifest for duplicate detection
        Set<String> seenBarcodes = new HashSet<>();

        for (int i = 0; i < samples.size(); i++) {
            SampleRegistrationDTO sample = samples.get(i);
            ManifestValidationResponse.RowValidationResult rowResult = new ManifestValidationResponse.RowValidationResult(
                    i);

            // Validate required fields
            String barcode = sample.getExternalId();
            if (barcode == null || barcode.trim().isEmpty()) {
                rowResult.addError("External ID (barcode) is required");
            } else {
                barcode = barcode.trim();
                // Check for duplicates within the manifest
                if (seenBarcodes.contains(barcode)) {
                    rowResult.addError("Duplicate barcode in manifest: " + barcode);
                } else {
                    seenBarcodes.add(barcode);
                }
                // Check for existing barcode in database
                if (bioSampleService.barcodeExists(barcode)) {
                    rowResult.addError("Barcode already exists in system: " + barcode);
                }
            }

            // Validate sample type
            String sampleTypeId = sample.getSampleTypeId();
            if (sampleTypeId == null || sampleTypeId.trim().isEmpty()) {
                rowResult.addError("Sample type is required");
            } else {
                TypeOfSample sampleType = findSampleTypeByNameOrId(sampleTypeId.trim());
                if (sampleType == null) {
                    rowResult.addError("Invalid sample type: " + sampleTypeId);
                }
            }

            // Validate biosafety level
            String bsl = sample.getBiosafetyLevel();
            if (bsl == null || bsl.trim().isEmpty()) {
                rowResult.addError("Biosafety level is required");
            } else {
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

        BulkRegistrationResponse response = new BulkRegistrationResponse();
        response.setSuccess(true);

        List<SampleRegistrationDTO> samples = request.getSamples();
        if (samples == null || samples.isEmpty()) {
            response.setSuccess(false);
            response.setError("No samples provided");
            return ResponseEntity.badRequest().body(response);
        }

        Shipment shipment = null;
        if (request.getShipmentId() != null) {
            shipment = shipmentService.get(request.getShipmentId());
        }

        try {
            for (SampleRegistrationDTO dto : samples) {
                BulkRegistrationResponse.RegisteredSample registered = registerSingleSample(dto, shipment, sysUserId);
                response.addSample(registered);
            }
        } catch (Exception e) {
            response.setSuccess(false);
            response.setError("Failed to register samples: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Register a single sample from the manifest DTO.
     */
    private BulkRegistrationResponse.RegisteredSample registerSingleSample(SampleRegistrationDTO dto, Shipment shipment,
            String sysUserId) {
        // Get barcode from externalId
        String barcode = dto.getExternalId() != null ? dto.getExternalId().trim()
                : generateBarcode().getBody().get("barcode");

        // Find sample type by name or ID
        TypeOfSample sampleType = findSampleTypeByNameOrId(dto.getSampleTypeId());
        if (sampleType == null) {
            throw new IllegalArgumentException("Invalid sample type: " + dto.getSampleTypeId());
        }

        // Create Sample (accession-level record)
        Sample sample = new Sample();
        sample.setAccessionNumber(generateAccessionNumber());
        sample.setReceivedTimestamp(new Timestamp(System.currentTimeMillis()));
        sample.setEnteredDate(new java.sql.Date(System.currentTimeMillis()));
        sample.setDomain("H"); // Human domain
        sample.setSysUserId(sysUserId);
        Sample savedSample = sampleService.save(sample);

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
        bioSample.setSpecialHandling(dto.getSpecialHandling());
        bioSample.setOriginLab(dto.getOriginLab());
        bioSample.setProjectId(dto.getProjectId());
        if (dto.getRequiredTempMin() != null) {
            bioSample.setRequiredTempMin(dto.getRequiredTempMin());
        }
        if (dto.getRequiredTempMax() != null) {
            bioSample.setRequiredTempMax(dto.getRequiredTempMax());
        }
        if (shipment != null) {
            bioSample.setShipment(shipment);
        }
        bioSample.setSysUserId(sysUserId);
        BioSample savedBioSample = bioSampleService.createForSampleItem(savedSampleItem, bioSample);

        // Build response
        BulkRegistrationResponse.RegisteredSample registered = new BulkRegistrationResponse.RegisteredSample();
        registered.setId(savedBioSample.getId());
        registered.setBarcode(barcode);
        registered.setSampleItemId(Integer.valueOf(savedSampleItem.getId()));
        registered.setSampleId(Integer.valueOf(savedSample.getId()));
        return registered;
    }

    /**
     * Find sample type by name or ID.
     */
    private TypeOfSample findSampleTypeByNameOrId(String nameOrId) {
        if (nameOrId == null || nameOrId.trim().isEmpty()) {
            return null;
        }
        nameOrId = nameOrId.trim();

        // Try to find by ID first
        try {
            TypeOfSample byId = typeOfSampleService.get(nameOrId);
            if (byId != null) {
                return byId;
            }
        } catch (Exception e) {
            // Not a valid ID, try by name
        }

        // Try to find by description (name)
        List<TypeOfSample> allTypes = typeOfSampleService.getAllTypeOfSamples();
        for (TypeOfSample type : allTypes) {
            if (nameOrId.equalsIgnoreCase(type.getDescription())
                    || nameOrId.equalsIgnoreCase(type.getLocalizedName())) {
                return type;
            }
        }

        return null;
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
            @RequestParam(required = false, defaultValue = "30") Integer days) {

        List<BioSample> samples;

        if ("expired".equalsIgnoreCase(status)) {
            samples = bioSampleService.getExpiredSamples();
        } else if ("all".equalsIgnoreCase(status)) {
            samples = bioSampleService.getSamplesForDisposalDashboard();
        } else {
            // Default to expiring within N days
            samples = bioSampleService.getExpiringSamples(days);
        }

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
        // Support multiple formats: yyyy-MM-dd, dd/MM/yyyy, dd-MM-yyyy
        String[] patterns = { "\\d{4}-\\d{2}-\\d{2}", "\\d{2}/\\d{2}/\\d{4}", "\\d{2}-\\d{2}-\\d{4}" };
        for (String pattern : patterns) {
            if (dateStr.matches(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parse date string to Timestamp.
     */
    private Timestamp parseDate(String dateStr) {
        try {
            // Try yyyy-MM-dd format (ISO)
            if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return Timestamp.valueOf(dateStr + " 00:00:00");
            }
            // Try dd/MM/yyyy format
            if (dateStr.matches("\\d{2}/\\d{2}/\\d{4}")) {
                String[] parts = dateStr.split("/");
                String iso = parts[2] + "-" + parts[1] + "-" + parts[0];
                return Timestamp.valueOf(iso + " 00:00:00");
            }
            // Try dd-MM-yyyy format
            if (dateStr.matches("\\d{2}-\\d{2}-\\d{4}")) {
                String[] parts = dateStr.split("-");
                String iso = parts[2] + "-" + parts[1] + "-" + parts[0];
                return Timestamp.valueOf(iso + " 00:00:00");
            }
        } catch (Exception e) {
            // Return null on parse error
        }
        return null;
    }
}
