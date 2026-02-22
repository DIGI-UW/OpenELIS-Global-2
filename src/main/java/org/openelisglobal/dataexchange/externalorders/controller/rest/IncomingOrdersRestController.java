package org.openelisglobal.dataexchange.externalorders.controller.rest;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.dataexchange.externalorders.dto.ExternalOrderRequest;
import org.openelisglobal.dataexchange.externalorders.service.IncomingOrderService;
import org.openelisglobal.dataexchange.externalorders.valueholder.IncomingOrder;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/incoming-orders")
public class IncomingOrdersRestController {

    @Autowired
    private IncomingOrderService incomingOrderService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<IncomingOrderListItem> list() {
        List<IncomingOrder> orders = incomingOrderService.getOrders();

        List<IncomingOrderListItem> response = new ArrayList<>();
        for (IncomingOrder order : orders) {
            IncomingOrderListItem item = new IncomingOrderListItem();
            item.setExternalOrderNumber(order.getExternalOrderNumber());
            item.setPatientGuid(order.getPatientGuid());
            item.setReceivedTimestamp(order.getReceivedTimestamp());
            response.add(item);
        }
        return response;
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
    public ResponseEntity<?> getSamplePatientEntryForm(@PathVariable("externalOrderNumber") String externalOrderNumber) {
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
        private String externalOrderNumber;
        private String patientGuid;
        private Timestamp receivedTimestamp;

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
    }
}
