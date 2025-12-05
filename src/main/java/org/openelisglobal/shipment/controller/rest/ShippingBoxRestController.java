package org.openelisglobal.shipment.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.shipment.form.ShippingBoxForm;
import org.openelisglobal.shipment.service.BoxSampleService;
import org.openelisglobal.shipment.service.ShippingBoxService;
import org.openelisglobal.shipment.valueholder.BoxSample;
import org.openelisglobal.shipment.valueholder.BoxState;
import org.openelisglobal.shipment.valueholder.ShippingBox;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    private BoxSampleService boxSampleService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private SystemUserService systemUserService;

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

            // Load samples for this box
            List<BoxSample> boxSamples = boxSampleService.getBoxSamplesByShippingBoxId(id);
            List<ShippingBoxForm.BoxSampleInfo> sampleInfos = new ArrayList<>();

            for (BoxSample boxSample : boxSamples) {
                ShippingBoxForm.BoxSampleInfo info = new ShippingBoxForm.BoxSampleInfo();
                info.setId(boxSample.getId());
                if (boxSample.getSample() != null) {
                    info.setSampleId(Integer.parseInt(boxSample.getSample().getId()));
                    info.setAccessionNumber(boxSample.getSample().getAccessionNumber());
                }
                info.setReceptionStatus(
                        boxSample.getReceptionStatus() != null ? boxSample.getReceptionStatus().name() : null);
                info.setReceptionNotes(boxSample.getReceptionNotes());
                info.setAddedDate(boxSample.getAddedDate());
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
            BindingResult result) {
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
    public ResponseEntity<?> changeBoxState(@PathVariable Integer id, @RequestParam String newState) {
        try {
            BoxState state = BoxState.valueOf(newState.toUpperCase());
            ShippingBox box = shippingBoxService.changeBoxState(id, state);
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
    public ResponseEntity<?> archiveBox(@PathVariable Integer id) {
        try {
            shippingBoxService.deleteBox(id);
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
     * Convert ShippingBox entity to form
     */
    private ShippingBoxForm convertToForm(ShippingBox box) {
        ShippingBoxForm form = new ShippingBoxForm();

        form.setId(box.getId());
        form.setBoxId(box.getBoxId());
        form.setState(box.getState() != null ? box.getState().name() : null);
        form.setTemperatureRequirement(box.getTemperatureRequirement());
        form.setNotes(box.getNotes());
        form.setCreatedDate(box.getCreatedDate());
        form.setSentDate(box.getSentDate());
        form.setReceivedDate(box.getReceivedDate());
        form.setReconciledDate(box.getReconciledDate());
        form.setArchived(box.getArchived());
        form.setArchivedDate(box.getArchivedDate());

        if (box.getDestinationFacility() != null) {
            form.setDestinationFacilityId(Integer.parseInt(box.getDestinationFacility().getId()));
            form.setDestinationFacilityName(box.getDestinationFacility().getOrganizationName());
        }

        if (box.getCreatedBy() != null) {
            form.setCreatedBy(Integer.parseInt(box.getCreatedBy().getId()));
            form.setCreatedByName(box.getCreatedBy().getNameForDisplay());
        }

        // Get sample count
        int sampleCount = boxSampleService.countSamplesInBox(box.getId());
        form.setSampleCount(sampleCount);

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
}
