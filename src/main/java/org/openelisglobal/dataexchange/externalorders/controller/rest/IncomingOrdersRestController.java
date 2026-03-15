package org.openelisglobal.dataexchange.externalorders.controller.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.openelisglobal.dataexchange.externalorders.dto.ExternalOrderRequest;
import org.openelisglobal.dataexchange.externalorders.service.IncomingOrderService;
import org.openelisglobal.dataexchange.externalorders.valueholder.IncomingOrder;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/incoming-orders")
public class IncomingOrdersRestController {

    private static final Logger logger = LoggerFactory.getLogger(IncomingOrdersRestController.class);

    @Autowired
    private IncomingOrderService incomingOrderService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private PanelItemService panelItemService;

    @Autowired
    private PanelService panelService;

    @Autowired
    private TestService testService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<IncomingOrderListItem> list() {
        List<IncomingOrder> orders = incomingOrderService.getOrders();

        List<IncomingOrderListItem> response = new ArrayList<>();
        for (IncomingOrder order : orders) {
            IncomingOrderListItem item = new IncomingOrderListItem();
            // Existing fields - MUST remain unchanged for collection flow
            item.setExternalOrderNumber(order.getExternalOrderNumber());
            item.setPatientGuid(order.getPatientGuid());
            item.setReceivedTimestamp(order.getReceivedTimestamp());

            // New display-only fields - null-safe
            item.setPatientName(getPatientName(order.getPatientGuid()));
            item.setTestCount(calculateTotalTestCount(order.getPayload()));
            item.setSource(extractSource(order.getPayload()));

            response.add(item);
        }
        return response;
    }

    /**
     * Get patient name from patientGuid. Returns null if patient not found.
     */
    private String getPatientName(String patientGuid) {
        if (patientGuid == null || patientGuid.trim().isEmpty()) {
            return null;
        }
        try {
            Patient patient = patientService.getPatientForGuid(patientGuid);
            if (patient != null && patient.getPerson() != null) {
                Person person = patient.getPerson();
                String firstName = person.getFirstName();
                String lastName = person.getLastName();
                if (firstName != null && lastName != null) {
                    return lastName + ", " + firstName;
                } else if (firstName != null) {
                    return firstName;
                } else if (lastName != null) {
                    return lastName;
                }
            }
        } catch (Exception e) {
            logger.debug("Could not retrieve patient name for guid: {}", patientGuid, e);
        }
        return null;
    }

    /**
     * Calculate total test count from payload. Includes direct tests + expanded
     * panel tests - removed tests. Uses deduplication to avoid counting same test
     * multiple times.
     */
    private Integer calculateTotalTestCount(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            return null;
        }
        try {
            ExternalOrderRequest request = objectMapper.readValue(payload, ExternalOrderRequest.class);
            if (request.getSamples() == null || request.getSamples().isEmpty()) {
                return 0;
            }

            Set<String> countedTestIds = new HashSet<>();
            int count = 0;

            for (ExternalOrderRequest.ExternalOrderSample sample : request.getSamples()) {
                // Count direct tests
                if (sample.getTests() != null) {
                    for (ExternalOrderRequest.ExternalOrderTestRef testRef : sample.getTests()) {
                        String testId = resolveTestId(testRef);
                        if (testId != null && countedTestIds.add(testId)) {
                            count++;
                        }
                    }
                }

                // Expand panels and count their tests
                if (sample.getPanels() != null) {
                    for (ExternalOrderRequest.ExternalOrderPanelRef panelRef : sample.getPanels()) {
                        String panelId = resolvePanelId(panelRef);
                        if (panelId != null) {
                            List<PanelItem> panelItems = panelItemService.getPanelItemsForPanel(panelId);
                            if (panelItems != null) {
                                for (PanelItem pi : panelItems) {
                                    if (pi.getTest() != null && pi.getTest().getId() != null) {
                                        String testId = pi.getTest().getId();
                                        if (countedTestIds.add(testId)) {
                                            count++;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Subtract removed tests
                if (sample.getRemovedTests() != null) {
                    for (ExternalOrderRequest.ExternalOrderTestRef removedRef : sample.getRemovedTests()) {
                        String testId = resolveTestId(removedRef);
                        if (testId != null && countedTestIds.contains(testId)) {
                            count--;
                        }
                    }
                }
            }

            return count;
        } catch (Exception e) {
            logger.debug("Could not calculate test count from payload", e);
            return null;
        }
    }

    /**
     * Extract source (referringSiteName) from payload. Returns null if not
     * available.
     */
    private String extractSource(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            return null;
        }
        try {
            ExternalOrderRequest request = objectMapper.readValue(payload, ExternalOrderRequest.class);
            String referringSiteName = request.getReferringSiteName();
            if (referringSiteName != null && !referringSiteName.trim().isEmpty()) {
                return referringSiteName.trim();
            }
            return null;
        } catch (Exception e) {
            logger.debug("Could not extract source from payload", e);
            return null;
        }
    }

    /**
     * Resolve test ID from test reference (by GUID or LOINC).
     */
    private String resolveTestId(ExternalOrderRequest.ExternalOrderTestRef testRef) {
        if (testRef == null) {
            return null;
        }
        if (testRef.getTestGuid() != null && !testRef.getTestGuid().trim().isEmpty()) {
            Test test = testService.getTestByGUID(testRef.getTestGuid().trim());
            return test != null ? test.getId() : null;
        }
        if (testRef.getLoinc() != null && !testRef.getLoinc().trim().isEmpty()) {
            List<Test> tests = testService.getActiveTestsByLoinc(testRef.getLoinc().trim());
            if (tests != null && !tests.isEmpty()) {
                return tests.get(0).getId();
            }
        }
        return null;
    }

    /**
     * Resolve panel ID from panel reference (by GUID or LOINC).
     */
    private String resolvePanelId(ExternalOrderRequest.ExternalOrderPanelRef panelRef) {
        if (panelRef == null) {
            return null;
        }
        if (panelRef.getPanelGuid() != null && !panelRef.getPanelGuid().trim().isEmpty()) {
            Panel panel = panelService.getPanelByGUID(panelRef.getPanelGuid().trim());
            return panel != null ? panel.getId() : null;
        }
        if (panelRef.getLoinc() != null && !panelRef.getLoinc().trim().isEmpty()) {
            Panel panel = panelService.getPanelByLoincCode(panelRef.getLoinc().trim());
            return panel != null ? panel.getId() : null;
        }
        return null;
    }

    @GetMapping(value = "/{externalOrderNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> get(@PathVariable("externalOrderNumber") String externalOrderNumber) {
        Optional<IncomingOrder> holdingOpt = incomingOrderService.getOrderByExternalOrderNumber(externalOrderNumber);
        if (holdingOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Unknown externalOrderNumber");
        }
        return ResponseEntity.ok(toDetail(holdingOpt.get()));
    }

    @GetMapping(value = "/{externalOrderNumber}/sample-patient-entry-form", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getSamplePatientEntryForm(
            @PathVariable("externalOrderNumber") String externalOrderNumber) {
        try {
            SamplePatientEntryForm form = incomingOrderService.buildSamplePatientEntryForm(externalOrderNumber);
            return ResponseEntity.ok(form);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping(value = "/{externalOrderNumber}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> update(@PathVariable("externalOrderNumber") String externalOrderNumber,
            @RequestBody ExternalOrderRequest updatedRequest) {
        try {
            IncomingOrder updated = incomingOrderService.updateOrderByExternalOrderNumber(externalOrderNumber,
                    updatedRequest, null, null);
            return ResponseEntity.ok(toDetail(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping(value = "/{externalOrderNumber}/finalize", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> finalizeHolding(@PathVariable("externalOrderNumber") String externalOrderNumber) {
        try {
            incomingOrderService.finalizeHolding(externalOrderNumber);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    private IncomingOrderDetail toDetail(IncomingOrder holding) {
        IncomingOrderDetail detail = new IncomingOrderDetail();
        detail.setExternalOrderNumber(holding.getExternalOrderNumber());
        detail.setPatientGuid(holding.getPatientGuid());
        detail.setPayload(holding.getPayload());
        detail.setReceivedTimestamp(holding.getReceivedTimestamp());
        detail.setReceivedSysUserId(holding.getReceivedSysUserId());
        detail.setCollectedTimestamp(holding.getCollectedTimestamp());
        detail.setCollectedSysUserId(holding.getCollectedSysUserId());
        detail.setLabNo(holding.getLabNo());
        detail.setSampleId(holding.getSampleId());
        detail.setErrorMessage(holding.getErrorMessage());
        return detail;
    }

    public static class IncomingOrderDetail {
        private String externalOrderNumber;
        private String patientGuid;
        private String payload;
        private Timestamp receivedTimestamp;
        private String receivedSysUserId;
        private Timestamp collectedTimestamp;
        private String collectedSysUserId;
        private String labNo;
        private Integer sampleId;
        private String errorMessage;

        public String getExternalOrderNumber() {
            return externalOrderNumber;
        }

        public void setExternalOrderNumber(String externalOrderNumber) {
            this.externalOrderNumber = externalOrderNumber;
        }

        public String getPatientGuid() {
            return patientGuid;
        }

        public void setPatientGuid(String patientGuid) {
            this.patientGuid = patientGuid;
        }

        public String getPayload() {
            return payload;
        }

        public void setPayload(String payload) {
            this.payload = payload;
        }

        public Timestamp getReceivedTimestamp() {
            return receivedTimestamp;
        }

        public void setReceivedTimestamp(Timestamp receivedTimestamp) {
            this.receivedTimestamp = receivedTimestamp;
        }

        public String getReceivedSysUserId() {
            return receivedSysUserId;
        }

        public void setReceivedSysUserId(String receivedSysUserId) {
            this.receivedSysUserId = receivedSysUserId;
        }

        public Timestamp getCollectedTimestamp() {
            return collectedTimestamp;
        }

        public void setCollectedTimestamp(Timestamp collectedTimestamp) {
            this.collectedTimestamp = collectedTimestamp;
        }

        public String getCollectedSysUserId() {
            return collectedSysUserId;
        }

        public void setCollectedSysUserId(String collectedSysUserId) {
            this.collectedSysUserId = collectedSysUserId;
        }

        public String getLabNo() {
            return labNo;
        }

        public void setLabNo(String labNo) {
            this.labNo = labNo;
        }

        public Integer getSampleId() {
            return sampleId;
        }

        public void setSampleId(Integer sampleId) {
            this.sampleId = sampleId;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    public static class IncomingOrderListItem {
        // Existing fields - MUST remain for collection flow
        private String externalOrderNumber;
        private String patientGuid;
        private Timestamp receivedTimestamp;

        // New display-only fields - additive
        private String patientName;
        private Integer testCount;
        private String source;

        public String getExternalOrderNumber() {
            return externalOrderNumber;
        }

        public void setExternalOrderNumber(String externalOrderNumber) {
            this.externalOrderNumber = externalOrderNumber;
        }

        public String getPatientGuid() {
            return patientGuid;
        }

        public void setPatientGuid(String patientGuid) {
            this.patientGuid = patientGuid;
        }

        public Timestamp getReceivedTimestamp() {
            return receivedTimestamp;
        }

        public void setReceivedTimestamp(Timestamp receivedTimestamp) {
            this.receivedTimestamp = receivedTimestamp;
        }

        public String getPatientName() {
            return patientName;
        }

        public void setPatientName(String patientName) {
            this.patientName = patientName;
        }

        public Integer getTestCount() {
            return testCount;
        }

        public void setTestCount(Integer testCount) {
            this.testCount = testCount;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }
    }
}
