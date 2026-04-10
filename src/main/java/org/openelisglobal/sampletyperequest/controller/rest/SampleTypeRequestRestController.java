package org.openelisglobal.sampletyperequest.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampletyperequest.dto.SampleTypeRequestDTO;
import org.openelisglobal.sampletyperequest.service.SampleTypeRequestService;
import org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
 * REST controller for SampleTypeRequest - manages requested sample types before
 * physical collection in the decoupled order workflow.
 */
@RestController
@RequestMapping("/rest/sample-type-requests")
public class SampleTypeRequestRestController {

    @Autowired
    private SampleTypeRequestService sampleTypeRequestService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private UnitOfMeasureService unitOfMeasureService;

    @Autowired
    private TestService testService;

    @Autowired
    private PanelService panelService;

    /**
     * Get all sample type requests for a sample.
     */
    @GetMapping("/sample/{sampleId}")
    public ResponseEntity<List<SampleTypeRequestDTO>> getRequestsBySample(@PathVariable String sampleId) {
        List<SampleTypeRequest> requests = sampleTypeRequestService.getRequestsBySampleId(sampleId);
        List<SampleTypeRequestDTO> dtos = requests.stream().map(this::convertToDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get pending (not yet collected) requests for a sample.
     */
    @GetMapping("/sample/{sampleId}/pending")
    public ResponseEntity<List<SampleTypeRequestDTO>> getPendingRequests(@PathVariable String sampleId) {
        List<SampleTypeRequest> requests = sampleTypeRequestService.getPendingRequestsBySampleId(sampleId);
        List<SampleTypeRequestDTO> dtos = requests.stream().map(this::convertToDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Create a new sample type request (Step 1: Enter Order).
     */
    @PostMapping
    public ResponseEntity<?> createRequest(@RequestBody SampleTypeRequestDTO dto, HttpServletRequest request) {
        try {
            // Validate required fields
            if (GenericValidator.isBlankOrNull(dto.getSampleId())) {
                return ResponseEntity.badRequest().body("sampleId is required");
            }
            if (GenericValidator.isBlankOrNull(dto.getTypeOfSampleId())) {
                return ResponseEntity.badRequest().body("typeOfSampleId is required");
            }

            // Load sample
            Sample sample = sampleService.get(dto.getSampleId());
            if (sample == null) {
                return ResponseEntity.badRequest().body("Sample not found: " + dto.getSampleId());
            }

            // Load type of sample
            TypeOfSample typeOfSample = typeOfSampleService.get(dto.getTypeOfSampleId());
            if (typeOfSample == null) {
                return ResponseEntity.badRequest().body("TypeOfSample not found: " + dto.getTypeOfSampleId());
            }

            // Create request
            SampleTypeRequest sampleTypeRequest = new SampleTypeRequest();
            sampleTypeRequest.setSample(sample);
            sampleTypeRequest.setTypeOfSample(typeOfSample);
            sampleTypeRequest.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
            sampleTypeRequest
                    .setRequestedQuantity(dto.getRequestedQuantity() != null ? dto.getRequestedQuantity() : 1.0);
            sampleTypeRequest.setRequestedTests(dto.getRequestedTests());
            sampleTypeRequest.setRequestedPanels(dto.getRequestedPanels());
            sampleTypeRequest.setStatus(SampleTypeRequest.Status.REQUESTED);
            sampleTypeRequest.setCreatedDate(new Timestamp(System.currentTimeMillis()));
            sampleTypeRequest.setSysUserId(ControllerUtills.getSysUserId(request));

            // Load unit of measure if provided
            if (!GenericValidator.isBlankOrNull(dto.getUnitOfMeasureId())) {
                UnitOfMeasure uom = unitOfMeasureService.get(dto.getUnitOfMeasureId());
                if (uom != null) {
                    sampleTypeRequest.setUnitOfMeasure(uom);
                }
            }

            Integer requestId = sampleTypeRequestService.insert(sampleTypeRequest);
            sampleTypeRequest.setId(requestId);

            LogEvent.logInfo(this.getClass().getSimpleName(), "createRequest",
                    "Created sample type request: " + requestId + " for sample: " + dto.getSampleId());

            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(sampleTypeRequest));

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "createRequest", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating request: " + e.getMessage());
        }
    }

    /**
     * Fulfill a request by linking it to a collected sample_item (Step 2: Collect
     * Sample).
     */
    @PutMapping("/{requestId}/fulfill")
    public ResponseEntity<?> fulfillRequest(@PathVariable Integer requestId, @RequestParam String sampleItemId) {
        try {
            sampleTypeRequestService.fulfillRequest(requestId, sampleItemId);

            SampleTypeRequest request = sampleTypeRequestService.get(requestId);
            LogEvent.logInfo(this.getClass().getSimpleName(), "fulfillRequest",
                    "Fulfilled request: " + requestId + " with sampleItem: " + sampleItemId);

            return ResponseEntity.ok(convertToDTO(request));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "fulfillRequest", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fulfilling request: " + e.getMessage());
        }
    }

    /**
     * Cancel a pending request.
     */
    @PutMapping("/{requestId}/cancel")
    public ResponseEntity<?> cancelRequest(@PathVariable Integer requestId) {
        try {
            sampleTypeRequestService.cancelRequest(requestId);

            SampleTypeRequest request = sampleTypeRequestService.get(requestId);
            LogEvent.logInfo(this.getClass().getSimpleName(), "cancelRequest", "Cancelled request: " + requestId);

            return ResponseEntity.ok(convertToDTO(request));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "cancelRequest", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error cancelling request: " + e.getMessage());
        }
    }

    /**
     * Convert entity to DTO with test and panel names resolved.
     */
    private SampleTypeRequestDTO convertToDTO(SampleTypeRequest entity) {
        SampleTypeRequestDTO dto = new SampleTypeRequestDTO(entity);

        // Resolve test names
        if (!GenericValidator.isBlankOrNull(entity.getRequestedTests())) {
            String[] testIds = entity.getRequestedTests().split(",");
            StringBuilder testNames = new StringBuilder();
            for (int i = 0; i < testIds.length; i++) {
                String testId = testIds[i].trim();
                if (!testId.isEmpty()) {
                    Test test = testService.getTestById(testId);
                    if (test != null) {
                        if (testNames.length() > 0) {
                            testNames.append(",");
                        }
                        String name = test.getLocalizedName();
                        if (name == null || name.isEmpty()) {
                            name = test.getDescription();
                        }
                        testNames.append(name != null ? name : testId);
                    }
                }
            }
            dto.setRequestedTestNames(testNames.toString());
        }

        // Resolve panel names
        if (!GenericValidator.isBlankOrNull(entity.getRequestedPanels())) {
            String[] panelIds = entity.getRequestedPanels().split(",");
            StringBuilder panelNames = new StringBuilder();
            for (int i = 0; i < panelIds.length; i++) {
                String panelId = panelIds[i].trim();
                if (!panelId.isEmpty()) {
                    Panel panel = panelService.getPanelById(panelId);
                    if (panel != null) {
                        if (panelNames.length() > 0) {
                            panelNames.append(",");
                        }
                        String name = panel.getLocalizedName();
                        if (name == null || name.isEmpty()) {
                            name = panel.getPanelName();
                        }
                        panelNames.append(name != null ? name : panelId);
                    }
                }
            }
            dto.setRequestedPanelNames(panelNames.toString());
        }

        return dto;
    }
}
