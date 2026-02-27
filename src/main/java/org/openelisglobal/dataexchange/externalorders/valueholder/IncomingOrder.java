package org.openelisglobal.dataexchange.externalorders.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "incoming_orders")
public class IncomingOrder extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "incoming_orders_generator")
    @SequenceGenerator(name = "incoming_orders_generator", sequenceName = "incoming_orders_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "external_order_number", nullable = false)
    private String externalOrderNumber;

    @Column(name = "patient_guid", nullable = false)
    private String patientGuid;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "received_timestamp")
    private Timestamp receivedTimestamp;

    @Column(name = "received_sys_user_id", length = 50)
    private String receivedSysUserId;

    @Column(name = "collected_timestamp")
    private Timestamp collectedTimestamp;

    @Column(name = "collected_sys_user_id", length = 50)
    private String collectedSysUserId;

    @Column(name = "lab_no", length = 50)
    private String labNo;

    @Column(name = "sample_id")
    private Integer sampleId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

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
