package org.openelisglobal.shipment.controller.rest;

import jakarta.validation.Valid;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.shipment.form.UnassignedSampleForm;
import org.openelisglobal.shipment.service.UnassignedSampleService;
import org.openelisglobal.shipment.valueholder.UnassignedSample;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for unassigned sample management operations
 */
@RestController
@RequestMapping("/rest/unassigned-sample")
public class UnassignedSampleRestController extends BaseRestController {

    @Autowired
    private UnassignedSampleService unassignedSampleService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private TestService testService;

    @Autowired
    private OrganizationService organizationService;

    /**
     * Get all unassigned samples
     */
    @GetMapping
    public ResponseEntity<List<UnassignedSampleForm>> getAllUnassignedSamples() {
        try {
            List<UnassignedSample> unassignedSamples = unassignedSampleService.getAllUnassignedSamples();
            List<UnassignedSampleForm> forms = new ArrayList<>();

            for (UnassignedSample unassignedSample : unassignedSamples) {
                forms.add(convertToForm(unassignedSample));
            }

            return ResponseEntity.ok(forms);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get unassigned sample by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<UnassignedSampleForm> getUnassignedSampleById(@PathVariable Integer id) {
        try {
            UnassignedSample unassignedSample = unassignedSampleService.getUnassignedSampleById(id);
            if (unassignedSample == null) {
                return ResponseEntity.notFound().build();
            }

            UnassignedSampleForm form = convertToForm(unassignedSample);
            return ResponseEntity.ok(form);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get unassigned sample by sample ID
     */
    @GetMapping("/by-sample/{sampleId}")
    public ResponseEntity<UnassignedSampleForm> getUnassignedSampleBySampleId(@PathVariable Integer sampleId) {
        try {
            UnassignedSample unassignedSample = unassignedSampleService.getUnassignedSampleBySampleId(sampleId);
            if (unassignedSample == null) {
                return ResponseEntity.notFound().build();
            }

            UnassignedSampleForm form = convertToForm(unassignedSample);
            return ResponseEntity.ok(form);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get unassigned samples by destination facility
     */
    @GetMapping("/by-facility/{facilityId}")
    public ResponseEntity<List<UnassignedSampleForm>> getUnassignedSamplesByFacility(@PathVariable Integer facilityId) {
        try {
            List<UnassignedSample> unassignedSamples = unassignedSampleService
                    .getUnassignedSamplesByDestinationFacility(facilityId);
            List<UnassignedSampleForm> forms = new ArrayList<>();

            for (UnassignedSample unassignedSample : unassignedSamples) {
                forms.add(convertToForm(unassignedSample));
            }

            return ResponseEntity.ok(forms);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get unassigned samples by priority
     */
    @GetMapping("/by-priority/{priority}")
    public ResponseEntity<List<UnassignedSampleForm>> getUnassignedSamplesByPriority(@PathVariable String priority) {
        try {
            List<UnassignedSample> unassignedSamples = unassignedSampleService.getUnassignedSamplesByPriority(priority);
            List<UnassignedSampleForm> forms = new ArrayList<>();

            for (UnassignedSample unassignedSample : unassignedSamples) {
                forms.add(convertToForm(unassignedSample));
            }

            return ResponseEntity.ok(forms);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get unassigned samples by referral test
     */
    @GetMapping("/by-test/{testId}")
    public ResponseEntity<List<UnassignedSampleForm>> getUnassignedSamplesByReferralTest(@PathVariable Integer testId) {
        try {
            List<UnassignedSample> unassignedSamples = unassignedSampleService
                    .getUnassignedSamplesByReferralTest(testId);
            List<UnassignedSampleForm> forms = new ArrayList<>();

            for (UnassignedSample unassignedSample : unassignedSamples) {
                forms.add(convertToForm(unassignedSample));
            }

            return ResponseEntity.ok(forms);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Count unassigned samples by facility
     */
    @GetMapping("/count-by-facility/{facilityId}")
    public ResponseEntity<Integer> countUnassignedSamplesByFacility(@PathVariable Integer facilityId) {
        try {
            int count = unassignedSampleService.countUnassignedSamplesByFacility(facilityId);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create a new unassigned sample entry
     */
    @PostMapping
    public ResponseEntity<?> createUnassignedSample(@Valid @RequestBody UnassignedSampleForm form,
            BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(result.getAllErrors());
        }

        try {
            UnassignedSample unassignedSample = convertFromForm(form);

            // Set created date
            if (unassignedSample.getCreatedDate() == null) {
                unassignedSample.setCreatedDate(new Timestamp(System.currentTimeMillis()));
            }

            UnassignedSample created = unassignedSampleService.createUnassignedSample(unassignedSample);
            UnassignedSampleForm responseForm = convertToForm(created);

            return ResponseEntity.status(HttpStatus.CREATED).body(responseForm);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating unassigned sample: " + e.getMessage());
        }
    }

    /**
     * Mark sample as assigned to a box
     */
    @PutMapping("/{id}/assign")
    public ResponseEntity<?> markSampleAsAssigned(@PathVariable Integer id) {
        try {
            unassignedSampleService.markSampleAsAssigned(id);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error marking sample as assigned: " + e.getMessage());
        }
    }

    /**
     * Delete unassigned sample entry
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUnassignedSample(@PathVariable Integer id) {
        try {
            unassignedSampleService.deleteUnassignedSample(id);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting unassigned sample: " + e.getMessage());
        }
    }

    /**
     * Convert UnassignedSample entity to form
     */
    private UnassignedSampleForm convertToForm(UnassignedSample unassignedSample) {
        UnassignedSampleForm form = new UnassignedSampleForm();

        form.setId(unassignedSample.getId());
        form.setPriority(unassignedSample.getPriority());
        form.setCreatedDate(unassignedSample.getCreatedDate());
        form.setAssignedDate(unassignedSample.getAssignedDate());
        form.setIsAssigned(unassignedSample.isAssigned());

        if (unassignedSample.getSample() != null) {
            form.setSampleId(Integer.parseInt(unassignedSample.getSample().getId()));
            form.setAccessionNumber(unassignedSample.getSample().getAccessionNumber());
        }

        if (unassignedSample.getReferralTest() != null) {
            form.setReferralTestId(Integer.parseInt(unassignedSample.getReferralTest().getId()));
            form.setReferralTestName(unassignedSample.getReferralTest().getName());
        }

        if (unassignedSample.getDestinationFacility() != null) {
            form.setDestinationFacilityId(Integer.parseInt(unassignedSample.getDestinationFacility().getId()));
            form.setDestinationFacilityName(unassignedSample.getDestinationFacility().getOrganizationName());
        }

        return form;
    }

    /**
     * Convert form to UnassignedSample entity
     */
    private UnassignedSample convertFromForm(UnassignedSampleForm form) {
        UnassignedSample unassignedSample = new UnassignedSample();

        unassignedSample.setId(form.getId());
        unassignedSample.setPriority(form.getPriority());
        unassignedSample.setCreatedDate(form.getCreatedDate());
        unassignedSample.setAssignedDate(form.getAssignedDate());

        // Set sample
        if (form.getSampleId() != null) {
            Sample sample = sampleService.get(form.getSampleId().toString());
            unassignedSample.setSample(sample);
        }

        // Set referral test
        if (form.getReferralTestId() != null) {
            Test test = testService.get(form.getReferralTestId().toString());
            unassignedSample.setReferralTest(test);
        }

        // Set destination facility
        if (form.getDestinationFacilityId() != null) {
            Organization facility = organizationService.get(form.getDestinationFacilityId().toString());
            unassignedSample.setDestinationFacility(facility);
        }

        return unassignedSample;
    }
}
