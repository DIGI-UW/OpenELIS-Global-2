package org.openelisglobal.shipment.controller.rest;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.shipment.form.BoxSampleForm;
import org.openelisglobal.shipment.service.BoxSampleService;
import org.openelisglobal.shipment.valueholder.BoxSample;
import org.openelisglobal.shipment.valueholder.ReceptionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
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
 * REST controller for box sample management operations
 */
@RestController
@RequestMapping("/rest/box-sample")
public class BoxSampleRestController extends BaseRestController {

    @Autowired
    private BoxSampleService boxSampleService;

    @Autowired
    private org.openelisglobal.referral.service.ReferralService referralService;

    @Autowired
    private org.openelisglobal.referral.service.ReferralTypeService referralTypeService;

    @Autowired
    private org.openelisglobal.analysis.service.AnalysisService analysisService;

    @Autowired
    private org.openelisglobal.shipment.dao.ShippingBoxDAO shippingBoxDAO;

    /**
     * Get box samples by shipping box ID
     */
    @GetMapping("/by-box/{shippingBoxId}")
    public ResponseEntity<List<BoxSampleForm>> getBoxSamplesByShippingBox(@PathVariable Integer shippingBoxId) {
        try {
            List<BoxSample> boxSamples = boxSampleService.getBoxSamplesByShippingBoxId(shippingBoxId);
            List<BoxSampleForm> forms = new ArrayList<>();

            for (BoxSample boxSample : boxSamples) {
                forms.add(convertToForm(boxSample));
            }

            return ResponseEntity.ok(forms);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get box sample by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<BoxSampleForm> getBoxSampleById(@PathVariable Integer id) {
        try {
            BoxSample boxSample = boxSampleService.getBoxSampleById(id);
            if (boxSample == null) {
                return ResponseEntity.notFound().build();
            }

            BoxSampleForm form = convertToForm(boxSample);
            return ResponseEntity.ok(form);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get box sample by sample ID
     */
    @GetMapping("/by-sample/{sampleId}")
    public ResponseEntity<BoxSampleForm> getBoxSampleBySampleId(@PathVariable Integer sampleId) {
        try {
            BoxSample boxSample = boxSampleService.getBoxSampleBySampleId(sampleId);
            if (boxSample == null) {
                return ResponseEntity.notFound().build();
            }

            BoxSampleForm form = convertToForm(boxSample);
            return ResponseEntity.ok(form);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get box samples by reception status
     */
    @GetMapping("/by-box/{shippingBoxId}/status/{status}")
    public ResponseEntity<List<BoxSampleForm>> getBoxSamplesByReceptionStatus(@PathVariable Integer shippingBoxId,
            @PathVariable String status) {
        try {
            ReceptionStatus receptionStatus = ReceptionStatus.valueOf(status.toUpperCase());
            List<BoxSample> boxSamples = boxSampleService.getBoxSamplesByReceptionStatus(shippingBoxId,
                    receptionStatus);
            List<BoxSampleForm> forms = new ArrayList<>();

            for (BoxSample boxSample : boxSamples) {
                forms.add(convertToForm(boxSample));
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
     * Add sample to box
     */
    @PostMapping
    public ResponseEntity<?> addSampleToBox(@Valid @RequestBody BoxSampleForm form, BindingResult result,
            jakarta.servlet.http.HttpServletRequest request) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(result.getAllErrors());
        }

        try {
            // Get system user ID for audit trail
            String userIdString = getSysUserId(request);
            Integer systemUserId = userIdString != null ? Integer.parseInt(userIdString) : null;

            BoxSample boxSample = boxSampleService.addSampleToBox(form.getShippingBoxId(), form.getSampleId(),
                    systemUserId);
            BoxSampleForm responseForm = convertToForm(boxSample);

            // Create or update Referral entries for ALL analysis IDs
            if (form.getAnalysisIds() != null && !form.getAnalysisIds().isEmpty()) {
                for (Integer analysisId : form.getAnalysisIds()) {
                    createOrUpdateReferral(analysisId, form.getShippingBoxId(), request);
                }
            } else if (form.getAnalysisId() != null) {
                // Fallback for backward compatibility
                createOrUpdateReferral(form.getAnalysisId(), form.getShippingBoxId(), request);
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(responseForm);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error adding sample to box: " + e.getMessage());
        }
    }

    /**
     * Create or update referral entry for analysis and assign to box
     */
    private void createOrUpdateReferral(Integer analysisId, Integer shippingBoxId,
            jakarta.servlet.http.HttpServletRequest request) {
        try {
            // Get existing referral or create new one
            org.openelisglobal.referral.valueholder.Referral referral =
                referralService.getReferralByAnalysisId(String.valueOf(analysisId));

            org.openelisglobal.shipment.valueholder.ShippingBox box =
                shippingBoxDAO.get(shippingBoxId).orElse(null);

            if (referral == null) {
                // Create new referral
                referral = new org.openelisglobal.referral.valueholder.Referral();

                // Get analysis
                org.openelisglobal.analysis.valueholder.Analysis analysis =
                    analysisService.get(String.valueOf(analysisId));

                // Get referral type "Sample Shipment"
                org.openelisglobal.referral.valueholder.ReferralType referralType =
                    referralTypeService.getReferralTypeByName("Sample Shipment");

                if (analysis != null && referralType != null) {
                    referral.setAnalysis(analysis);
                    referral.setRequestDate(new java.sql.Timestamp(System.currentTimeMillis()));
                    referral.setStatus(org.openelisglobal.referral.valueholder.ReferralStatus.CREATED);
                    referral.setFhirUuid(java.util.UUID.randomUUID());
                    referral.setAssignedBox(box);
                    referral.setReferralTypeId(referralType.getId());
                    referral.setSysUserId(getSysUserId(request));

                    // Set destination organization from the box
                    if (box != null && box.getDestinationFacility() != null) {
                        referral.setOrganization(box.getDestinationFacility());
                    }

                    referralService.insert(referral);
                } else {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "createOrUpdateReferral",
                            "Could not create referral - analysis or referralType is null. Analysis ID: " + analysisId);
                }
            } else {
                // Update existing referral
                referral.setAssignedBox(box);
                referral.setSysUserId(getSysUserId(request));
                referralService.update(referral);
            }
        } catch (Exception e) {
            LogEvent.logError("Error creating/updating referral", e);
            // Log but don't fail the box sample creation
        }
    }

    /**
     * Remove sample from box
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> removeSampleFromBox(@PathVariable Integer id) {
        try {
            boxSampleService.removeSampleFromBox(id);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error removing sample from box: " + e.getMessage());
        }
    }

    /**
     * Update reception status
     */
    @PutMapping("/{id}/reception-status")
    public ResponseEntity<?> updateReceptionStatus(@PathVariable Integer id, @RequestParam String status,
            @RequestParam(required = false) String notes) {
        try {
            ReceptionStatus receptionStatus = ReceptionStatus.valueOf(status.toUpperCase());
            BoxSample boxSample = boxSampleService.updateReceptionStatus(id, receptionStatus, notes);
            BoxSampleForm form = convertToForm(boxSample);

            return ResponseEntity.ok(form);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid reception status: " + status);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating reception status: " + e.getMessage());
        }
    }

    /**
     * Check if sample is in a box
     */
    @GetMapping("/check-sample/{sampleId}")
    public ResponseEntity<Boolean> isSampleInBox(@PathVariable Integer sampleId) {
        try {
            boolean inBox = boxSampleService.isSampleInBox(sampleId);
            return ResponseEntity.ok(inBox);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Count samples in box
     */
    @GetMapping("/count-by-box/{shippingBoxId}")
    public ResponseEntity<Integer> countSamplesInBox(@PathVariable Integer shippingBoxId) {
        try {
            int count = boxSampleService.countSamplesInBox(shippingBoxId);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Convert BoxSample entity to form
     */
    private BoxSampleForm convertToForm(BoxSample boxSample) {
        BoxSampleForm form = new BoxSampleForm();

        form.setId(boxSample.getId());
        form.setPositionInBox(boxSample.getPositionInBox());
        form.setReceptionNotes(boxSample.getReceptionNotes());
        form.setAddedDate(boxSample.getAddedDate());

        if (boxSample.getShippingBox() != null) {
            form.setShippingBoxId(boxSample.getShippingBox().getId());
        }

        if (boxSample.getSample() != null) {
            form.setSampleId(Integer.parseInt(boxSample.getSample().getId()));
            form.setAccessionNumber(boxSample.getSample().getAccessionNumber());
        }

        if (boxSample.getReceptionStatus() != null) {
            form.setReceptionStatus(boxSample.getReceptionStatus().name());
        }

        return form;
    }
}
