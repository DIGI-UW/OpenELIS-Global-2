package org.openelisglobal.shipment.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.shipment.dto.SampleItemDTO;
import org.openelisglobal.shipment.fhir.ShipmentFhirImportService;
import org.openelisglobal.shipment.form.ShippingBoxForm;
import org.openelisglobal.shipment.service.BoxLabelPDFService;
import org.openelisglobal.shipment.service.BoxSampleItemService;
import org.openelisglobal.shipment.service.ManifestPDFService;
import org.openelisglobal.shipment.service.ShippingBoxService;
import org.openelisglobal.shipment.valueholder.BoxState;
import org.openelisglobal.shipment.valueholder.ShippingBox;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for shipping box management operations
 */
@RestController
@RequestMapping("/rest/shipping-box")
public class ShippingBoxRestController extends BaseRestController {

    @Autowired
    private ShippingBoxService shippingBoxService;

    @Autowired
    private BoxSampleItemService boxSampleItemService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private BoxLabelPDFService boxLabelPDFService;

    @Autowired
    private ManifestPDFService manifestPDFService;

    @Autowired
    private SiteInformationService siteInformationService;

    @Autowired
    private ShipmentFhirImportService shipmentFhirImportService;

    /**
     * Get all active shipping boxes
     */
    @GetMapping
    public ResponseEntity<List<ShippingBoxForm>> getAllBoxes() {
        try {
            List<ShippingBox> boxes = shippingBoxService.getAllActiveBoxes();
            List<ShippingBoxForm> forms = new ArrayList<>();

            for (ShippingBox box : boxes) {
                forms.add(convertToForm(box));
            }

            return ResponseEntity.ok(forms);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get shipping box by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ShippingBoxForm> getBoxById(@PathVariable Integer id) {
        try {
            ShippingBox box = shippingBoxService.getBoxById(id);
            if (box == null) {
                return ResponseEntity.notFound().build();
            }

            ShippingBoxForm form = convertToForm(box);

            // Load sample items for this box (NEW: using BoxSampleItemService)
            List<SampleItemDTO> sampleItems = boxSampleItemService.getBoxSampleItemDTOsByShippingBoxId(id);
            List<ShippingBoxForm.BoxSampleInfo> sampleInfos = new ArrayList<>();

            for (SampleItemDTO sampleItem : sampleItems) {
                ShippingBoxForm.BoxSampleInfo info = new ShippingBoxForm.BoxSampleInfo();
                // Convert sampleItemId (String) to Integer for backward compatibility
                info.setSampleId(parseSampleItemId(sampleItem.getSampleItemId()));
                info.setAccessionNumber(sampleItem.getAccessionNumber());
                // Note: SampleItemDTO doesn't have receptionStatus, addedDate
                // These fields may need to be added or we need to fetch from BoxSampleItem
                // entity
                sampleInfos.add(info);
            }
            form.setSamples(sampleInfos);

            return ResponseEntity.ok(form);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get shipping boxes by state
     */
    @GetMapping("/by-state/{state}")
    public ResponseEntity<List<ShippingBoxForm>> getBoxesByState(@PathVariable String state) {
        try {
            BoxState boxState = BoxState.valueOf(state.toUpperCase());
            List<ShippingBox> boxes = shippingBoxService.getBoxesByState(boxState);
            List<ShippingBoxForm> forms = new ArrayList<>();

            for (ShippingBox box : boxes) {
                forms.add(convertToForm(box));
            }

            return ResponseEntity.ok(forms);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get shipping boxes by destination facility
     */
    @GetMapping("/by-facility/{facilityId}")
    public ResponseEntity<List<ShippingBoxForm>> getBoxesByFacility(@PathVariable Integer facilityId) {
        try {
            List<ShippingBox> boxes = shippingBoxService.getBoxesByDestinationFacility(facilityId);
            List<ShippingBoxForm> forms = new ArrayList<>();

            for (ShippingBox box : boxes) {
                forms.add(convertToForm(box));
            }

            return ResponseEntity.ok(forms);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generate a unique box number
     */
    @GetMapping("/generate-box-number")
    public ResponseEntity<String> generateBoxNumber() {
        try {
            String boxNumber = generateUniqueBoxNumber();
            return ResponseEntity.ok(boxNumber);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get the configured box label prefix
     */
    @GetMapping("/box-label-prefix")
    public ResponseEntity<String> getBoxLabelPrefix() {
        try {
            String prefix = getConfiguredPrefix();
            return ResponseEntity.ok(prefix);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update the box label prefix
     */
    @PutMapping("/box-label-prefix")
    public ResponseEntity<String> updateBoxLabelPrefix(@RequestBody String newPrefix, HttpServletRequest request) {
        try {
            if (newPrefix == null || newPrefix.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Prefix cannot be empty");
            }
            String trimmedPrefix = newPrefix.trim().toUpperCase();

            SiteInformation siteInfo = siteInformationService.getSiteInformationByName("boxLabelPrefix");
            if (siteInfo != null) {
                siteInfo.setValue(trimmedPrefix);
                String userId = getSysUserId(request);
                if (userId != null) {
                    siteInfo.setSysUserId(userId);
                }
                siteInformationService.persistData(siteInfo, false);
            }

            return ResponseEntity.ok(trimmedPrefix);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get the FHIR UUID and org ID of the Organization representing this site.
     */
    @GetMapping("/site-organization-uuid")
    public ResponseEntity<?> getSiteOrganizationUuid() {
        SiteInformation siteInfo = siteInformationService.getSiteInformationByName("siteOrganizationFhirUuid");
        String fhirUuid = (siteInfo != null && siteInfo.getValue() != null) ? siteInfo.getValue() : "";

        // Resolve fhirUuid back to org database ID for dropdown pre-selection
        String orgId = "";
        if (!fhirUuid.isBlank()) {
            try {
                java.util.UUID uuid = java.util.UUID.fromString(fhirUuid);
                List<Organization> orgs = organizationService.getAll();
                for (Organization org : orgs) {
                    if (org.getFhirUuid() != null && org.getFhirUuid().equals(uuid)) {
                        orgId = org.getId();
                        break;
                    }
                }
            } catch (Exception e) {
                // Invalid UUID stored — ignore
            }
        }

        var result = new java.util.HashMap<String, String>();
        result.put("fhirUuid", fhirUuid);
        result.put("orgId", orgId);
        return ResponseEntity.ok(result);
    }

    /**
     * Set the site organization by its database ID. The backend resolves the FHIR
     * UUID and stores it. Used to filter incoming FHIR shipments.
     */
    @PutMapping("/site-organization-uuid")
    public ResponseEntity<?> setSiteOrganizationUuid(@RequestBody String orgId, HttpServletRequest request) {
        try {
            String trimmed = orgId != null ? orgId.trim() : "";
            String fhirUuid = "";

            // Resolve database ID to FHIR UUID
            if (!trimmed.isEmpty()) {
                Organization org = organizationService.get(trimmed);
                if (org == null) {
                    return ResponseEntity.badRequest().body("Organization not found: " + trimmed);
                }
                if (org.getFhirUuid() != null) {
                    fhirUuid = org.getFhirUuid().toString();
                } else {
                    return ResponseEntity.badRequest()
                            .body("Organization '" + org.getOrganizationName() + "' has no FHIR UUID");
                }
            }

            SiteInformation siteInfo = siteInformationService.getSiteInformationByName("siteOrganizationFhirUuid");
            if (siteInfo != null) {
                siteInfo.setValue(fhirUuid);
                String userId = getSysUserId(request);
                if (userId != null) {
                    siteInfo.setSysUserId(userId);
                }
                siteInformationService.update(siteInfo);
            }

            // Return both the FHIR UUID and the org name for display
            var result = new java.util.HashMap<String, String>();
            result.put("fhirUuid", fhirUuid);
            result.put("orgId", trimmed);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all FHIR mapping configuration values.
     */
    @GetMapping("/fhir-mapping-config")
    public ResponseEntity<?> getFhirMappingConfig() {
        var config = new java.util.HashMap<String, String>();
        config.put("containerTypeCode", getSiteInfoValue("fhirContainerTypeCode", "434711009"));
        config.put("containerTypeDisplay", getSiteInfoValue("fhirContainerTypeDisplay", "Specimen container"));
        config.put("nonConformityCodes",
                getSiteInfoValue("fhirNonConformityCodes",
                        "{\"RECEIVED_DAMAGED\":\"281411007\",\"RECEIVED_LEAKED\":\"281412000\","
                                + "\"MISSING\":\"281264009\",\"REJECTED\":\"123840003\"}"));
        return ResponseEntity.ok(config);
    }

    /**
     * Update FHIR mapping configuration values.
     */
    @PutMapping("/fhir-mapping-config")
    public ResponseEntity<?> setFhirMappingConfig(@RequestBody java.util.Map<String, String> configMap,
            HttpServletRequest request) {
        try {
            String userId = getSysUserId(request);

            if (configMap.containsKey("containerTypeCode")) {
                saveSiteInfo("fhirContainerTypeCode", configMap.get("containerTypeCode"), userId);
            }
            if (configMap.containsKey("containerTypeDisplay")) {
                saveSiteInfo("fhirContainerTypeDisplay", configMap.get("containerTypeDisplay"), userId);
            }
            if (configMap.containsKey("nonConformityCodes")) {
                saveSiteInfo("fhirNonConformityCodes", configMap.get("nonConformityCodes"), userId);
            }

            return ResponseEntity.ok(configMap);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String getSiteInfoValue(String name, String defaultValue) {
        SiteInformation si = siteInformationService.getSiteInformationByName(name);
        if (si != null && si.getValue() != null && !si.getValue().isBlank()) {
            return si.getValue();
        }
        return defaultValue;
    }

    private void saveSiteInfo(String name, String value, String userId) {
        SiteInformation si = siteInformationService.getSiteInformationByName(name);
        if (si != null) {
            si.setValue(value != null ? value.trim() : "");
            if (userId != null) {
                si.setSysUserId(userId);
            }
            siteInformationService.update(si);
        }
    }

    private String getConfiguredPrefix() {
        SiteInformation siteInfo = siteInformationService.getSiteInformationByName("boxLabelPrefix");
        if (siteInfo != null && siteInfo.getValue() != null && !siteInfo.getValue().isEmpty()) {
            return siteInfo.getValue();
        }
        return "BOX";
    }

    /**
     * Generate unique box number with format PREFIX-YYYY-NNNN Uses sequential
     * numbering within the year to avoid collisions. The prefix is configurable via
     * the boxLabelPrefix site information setting.
     */
    private synchronized String generateUniqueBoxNumber() {
        int year = java.time.Year.now().getValue();
        String labelPrefix = getConfiguredPrefix();
        String prefix = labelPrefix + "-" + year + "-";
        int nextNumber = 1;

        // Try up to 9999 sequential numbers
        while (nextNumber < 10000) {
            String candidateBoxId = String.format("%s%04d", prefix, nextNumber);

            // Check if this box ID already exists
            ShippingBox existingBox = shippingBoxService.getBoxByBoxId(candidateBoxId);
            if (existingBox == null) {
                return candidateBoxId;
            }

            nextNumber++;
        }

        // Fallback: use timestamp if all 9999 numbers are exhausted (very unlikely)
        long timestamp = System.currentTimeMillis() % 100000;
        return String.format("%s%05d", prefix, timestamp);
    }

    /**
     * Get shipping box by box ID (not database ID)
     */
    @GetMapping("/by-box-id/{boxId}")
    public ResponseEntity<ShippingBoxForm> getBoxByBoxId(@PathVariable String boxId) {
        try {
            ShippingBox box = shippingBoxService.getBoxByBoxId(boxId);
            if (box == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(convertToForm(box));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create a new shipping box
     */
    @PostMapping
    public ResponseEntity<?> createBox(@Valid @RequestBody ShippingBoxForm form, BindingResult result,
            HttpServletRequest request) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(result.getAllErrors());
        }

        try {
            ShippingBox box = convertFromForm(form);

            // Generate FHIR UUID
            box.setFhirUuid(UUID.randomUUID());

            // Set state to DRAFT if not specified
            if (box.getState() == null) {
                box.setState(BoxState.DRAFT);
            }

            // Set sys_user_id for audit trail
            String userIdString = getSysUserId(request);
            if (userIdString != null) {
                box.setSystemUserId(Integer.parseInt(userIdString));
            }

            ShippingBox createdBox = shippingBoxService.createBox(box);
            ShippingBoxForm responseForm = convertToForm(createdBox);

            return ResponseEntity.status(HttpStatus.CREATED).body(responseForm);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating shipping box: " + e.getMessage());
        }
    }

    /**
     * Update an existing shipping box
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBox(@PathVariable Integer id, @Valid @RequestBody ShippingBoxForm form,
            BindingResult result, HttpServletRequest request) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(result.getAllErrors());
        }

        try {
            // Verify box exists
            ShippingBox existing = shippingBoxService.getBoxById(id);
            if (existing == null) {
                return ResponseEntity.notFound().build();
            }

            form.setId(id);
            ShippingBox box = convertFromForm(form);

            // Preserve FHIR UUID
            box.setFhirUuid(existing.getFhirUuid());

            // Set sys_user_id for audit trail
            String userIdString = getSysUserId(request);
            if (userIdString != null) {
                box.setSystemUserId(Integer.parseInt(userIdString));
            }

            ShippingBox updatedBox = shippingBoxService.updateBox(box);
            ShippingBoxForm responseForm = convertToForm(updatedBox);

            return ResponseEntity.ok(responseForm);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating shipping box: " + e.getMessage());
        }
    }

    /**
     * Change box state
     */
    @PutMapping("/{id}/state")
    public ResponseEntity<?> changeBoxState(@PathVariable Integer id, @RequestParam String newState,
            HttpServletRequest request) {
        try {
            if (newState == null || newState.isBlank()) {
                return ResponseEntity.badRequest().body("State parameter is required");
            }
            BoxState state = BoxState.valueOf(newState.toUpperCase());
            String userIdString = getSysUserId(request);
            Integer systemUserId = userIdString != null ? Integer.parseInt(userIdString) : null;
            ShippingBox box = shippingBoxService.changeBoxState(id, state, systemUserId);
            ShippingBoxForm form = convertToForm(box);

            return ResponseEntity.ok(form);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid state: " + newState);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error changing box state: " + e.getMessage());
        }
    }

    /**
     * Delete/Archive a box
     */
    @PutMapping("/{id}/archive")
    public ResponseEntity<?> archiveBox(@PathVariable Integer id, HttpServletRequest request) {
        try {
            String userIdString = getSysUserId(request);
            Integer systemUserId = userIdString != null ? Integer.parseInt(userIdString) : null;
            shippingBoxService.deleteBox(id, systemUserId);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error archiving box: " + e.getMessage());
        }
    }

    /**
     * Download box label PDF
     */
    @GetMapping("/{id}/label/pdf")
    public ResponseEntity<byte[]> downloadBoxLabel(@PathVariable Integer id) {
        try {
            ShippingBox box = shippingBoxService.getBoxById(id);
            if (box == null) {
                return ResponseEntity.notFound().build();
            }

            ByteArrayOutputStream pdfStream = boxLabelPDFService.generateBoxLabelPDF(id.toString());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "box-label-" + box.getBoxId() + ".pdf");
            headers.setContentLength(pdfStream.size());

            return new ResponseEntity<>(pdfStream.toByteArray(), headers, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get manifest data as JSON for frontend PDF generation. Now returns SampleItem
     * data including typeOfSample and referralTests.
     */
    @GetMapping("/{id}/manifest-data")
    public ResponseEntity<?> getManifestData(@PathVariable Integer id) {
        try {
            ShippingBox box = shippingBoxService.getBoxById(id);
            if (box == null) {
                return ResponseEntity.notFound().build();
            }

            // Get sample items (NEW: using BoxSampleItemService)
            List<SampleItemDTO> sampleItems = boxSampleItemService.getBoxSampleItemDTOsByShippingBoxId(id);

            // Create response map
            java.util.Map<String, Object> manifestData = new java.util.HashMap<>();
            manifestData.put("boxId", box.getBoxId());
            manifestData.put("destinationFacility",
                    box.getDestinationFacility() != null ? box.getDestinationFacility().getOrganizationName() : "");
            manifestData.put("state", box.getState() != null ? box.getState().toString() : "");
            manifestData.put("temperature",
                    box.getTemperatureRequirement() != null ? box.getTemperatureRequirement() : "AMBIENT");
            manifestData.put("createdDate", box.getCreatedDate());
            manifestData.put("createdBy", box.getCreatedBy() != null ? box.getCreatedBy().getNameForDisplay() : "");
            manifestData.put("notes", box.getNotes() != null ? box.getNotes() : "");

            // Service location (sender facility)
            String serviceLocation = org.openelisglobal.common.util.ConfigurationProperties.getInstance()
                    .getPropertyValue(org.openelisglobal.common.util.ConfigurationProperties.Property.SiteName);
            manifestData.put("serviceLocation", serviceLocation != null ? serviceLocation : "");

            // Add sample items with typeOfSample and referralTests
            java.util.List<java.util.Map<String, Object>> samplesList = new java.util.ArrayList<>();
            for (SampleItemDTO sampleItem : sampleItems) {
                java.util.Map<String, Object> sampleData = new java.util.HashMap<>();
                sampleData.put("accessionNumber",
                        sampleItem.getAccessionNumber() != null ? sampleItem.getAccessionNumber() : "");
                sampleData.put("typeOfSample",
                        sampleItem.getTypeOfSample() != null ? sampleItem.getTypeOfSample() : "");
                sampleData.put("referralTests", sampleItem.getReferralTestsAsString()); // Comma-separated test names
                sampleData.put("collectionDate", sampleItem.getCollectionDate());
                samplesList.add(sampleData);
            }
            manifestData.put("samples", samplesList);

            // Time-based manifest rules (Rule 3): regeneration within 24h, recall within 7d
            final long MILLIS_PER_HOUR = 1000L * 60 * 60;
            final long REGENERATION_LIMIT_HOURS = 24;
            final long RECALL_LIMIT_HOURS = 7 * 24; // 7 days

            manifestData.put("sentDate", box.getSentDate());
            boolean canRegenerate = true;
            boolean canRecall = true;
            if (box.getSentDate() != null) {
                long hoursSinceSent = (System.currentTimeMillis() - box.getSentDate().getTime()) / MILLIS_PER_HOUR;
                canRegenerate = hoursSinceSent <= REGENERATION_LIMIT_HOURS;
                canRecall = hoursSinceSent <= RECALL_LIMIT_HOURS;
            }
            manifestData.put("canRegenerate", canRegenerate);
            manifestData.put("canRecall", canRecall);

            return ResponseEntity.ok(manifestData);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * Download box manifest PDF
     */
    @GetMapping("/{id}/manifest/pdf")
    public ResponseEntity<byte[]> downloadBoxManifest(@PathVariable Integer id) {
        try {
            ShippingBox box = shippingBoxService.getBoxById(id);
            if (box == null) {
                return ResponseEntity.notFound().build();
            }

            ByteArrayOutputStream pdfStream = manifestPDFService.generateManifestPDF(id.toString());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "manifest-" + box.getBoxId() + ".pdf");
            headers.setContentLength(pdfStream.size());

            return new ResponseEntity<>(pdfStream.toByteArray(), headers, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            LogEvent.logError("Box not found", e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            LogEvent.logError("Error generating manifest PDF", e);
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get dashboard statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<java.util.Map<String, Integer>> getStatistics() {
        try {
            List<ShippingBox> allBoxes = shippingBoxService.getAllActiveBoxes();

            int inTransitCount = 0;
            int deliveredCount = 0;
            int reconciledCount = 0;
            int totalSamples = 0;

            for (ShippingBox box : allBoxes) {
                BoxState state = box.getState();
                int sampleCount = boxSampleItemService.countSampleItemsInBox(box.getId());

                if (state == BoxState.SENT) {
                    inTransitCount++;
                } else if (state == BoxState.RECEIVED) {
                    deliveredCount++;
                } else if (state == BoxState.RECONCILED) {
                    reconciledCount++;
                }

                totalSamples += sampleCount;
            }

            // Create response object
            var statistics = new java.util.HashMap<String, Integer>();
            statistics.put("inTransit", inTransitCount);
            statistics.put("delivered", deliveredCount);
            statistics.put("reconciled", reconciledCount);
            statistics.put("totalSamples", totalSamples);

            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Convert ShippingBox entity to form
     */
    private ShippingBoxForm convertToForm(ShippingBox box) {
        ShippingBoxForm form = new ShippingBoxForm();

        form.setId(box.getId());
        form.setBoxId(box.getBoxId());
        form.setState(box.getState() != null ? box.getState().name() : null);
        form.setTemperatureRequirement(box.getTemperatureRequirement());
        form.setCapacity(box.getCapacity());
        form.setActualSampleCount(box.getActualSampleCount());
        form.setNotes(box.getNotes());
        form.setCreatedDate(box.getCreatedDate());
        form.setSentDate(box.getSentDate());
        form.setReceivedDate(box.getReceivedDate());
        form.setReconciledDate(box.getReconciledDate());
        form.setArchived(box.getArchived());
        form.setArchivedDate(box.getArchivedDate());

        if (box.getDestinationFacility() != null && box.getDestinationFacility().getId() != null) {
            try {
                form.setDestinationFacilityId(Integer.parseInt(box.getDestinationFacility().getId()));
            } catch (NumberFormatException e) {
                // Non-numeric facility ID — skip
            }
            form.setDestinationFacilityName(box.getDestinationFacility().getOrganizationName());
        }

        if (box.getCreatedBy() != null && box.getCreatedBy().getId() != null) {
            try {
                form.setCreatedBy(Integer.parseInt(box.getCreatedBy().getId()));
            } catch (NumberFormatException e) {
                // Non-numeric user ID — skip
            }
            form.setCreatedByName(box.getCreatedBy().getNameForDisplay());
        }

        // Get sample items and compute count + contents summary
        List<SampleItemDTO> sampleItems = boxSampleItemService.getBoxSampleItemDTOsByShippingBoxId(box.getId());
        form.setSampleCount(sampleItems.size());

        // Build contents summary: "Serum (3), Plasma (2)"
        java.util.Map<String, Integer> typeCounts = new java.util.LinkedHashMap<>();
        for (SampleItemDTO item : sampleItems) {
            String type = item.getTypeOfSample() != null ? item.getTypeOfSample() : "Unknown";
            typeCounts.put(type, typeCounts.getOrDefault(type, 0) + 1);
        }
        StringBuilder contentsBuilder = new StringBuilder();
        typeCounts.forEach((type, count) -> {
            if (!contentsBuilder.isEmpty()) {
                contentsBuilder.append(", ");
            }
            contentsBuilder.append(type).append(" (").append(count).append(")");
        });
        form.setContents(contentsBuilder.toString());

        return form;
    }

    /**
     * Convert form to ShippingBox entity
     */
    private ShippingBox convertFromForm(ShippingBoxForm form) {
        ShippingBox box = new ShippingBox();

        box.setId(form.getId());
        box.setBoxId(form.getBoxId());

        if (form.getState() != null) {
            box.setState(BoxState.valueOf(form.getState()));
        }

        box.setTemperatureRequirement(form.getTemperatureRequirement());
        box.setCapacity(form.getCapacity());
        box.setActualSampleCount(form.getActualSampleCount());
        box.setNotes(form.getNotes());
        box.setCreatedDate(form.getCreatedDate());
        box.setSentDate(form.getSentDate());
        box.setReceivedDate(form.getReceivedDate());
        box.setReconciledDate(form.getReconciledDate());
        box.setArchived(form.getArchived());
        box.setArchivedDate(form.getArchivedDate());

        // Set destination facility
        if (form.getDestinationFacilityId() != null) {
            Organization facility = organizationService.get(form.getDestinationFacilityId().toString());
            box.setDestinationFacility(facility);
        }

        // Set created by user
        if (form.getCreatedBy() != null) {
            SystemUser user = systemUserService.getUserById(String.valueOf(form.getCreatedBy()));
            box.setCreatedBy(user);
        }

        return box;
    }

    /**
     * Import shipments from remote FHIR servers. Polls for SupplyDelivery resources
     * with status in-progress and creates local ShippingBox entries with state
     * IN_TRANSIT for reception reconciliation.
     */
    @PostMapping("/import-from-fhir")
    public ResponseEntity<?> importShipmentsFromFhir() {
        try {
            int imported = shipmentFhirImportService.pollAndImportShipments();
            return ResponseEntity.ok(java.util.Collections.singletonMap("imported", imported));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Collections.singletonMap("error", "Error importing shipments: " + e.getMessage()));
        }
    }

    /**
     * Helper method to parse sampleItemId (String) to Integer Handles
     * NumberFormatException gracefully
     */
    private Integer parseSampleItemId(String sampleItemId) {
        if (sampleItemId == null) {
            return null;
        }
        try {
            return Integer.parseInt(sampleItemId);
        } catch (NumberFormatException e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "parseSampleItemId",
                    "Could not parse sampleItemId to Integer: " + sampleItemId);
            return null;
        }
    }
}
