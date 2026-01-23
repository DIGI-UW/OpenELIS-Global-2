package org.openelisglobal.biorepository.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.valueholder.SystemUser;

/**
 * Immutable audit trail entity for all sample custody changes.
 *
 * Records who, when, and where for every movement of a sample through the
 * biorepository lifecycle. Each entry captures a single custody action and
 * cannot be modified after creation (immutable audit log).
 *
 * Links to: - SampleItem: The physical sample being tracked (required) -
 * SampleTransferRequest: The inbound transfer that brought the sample in
 * (optional) - SampleRetrievalRequest: The outbound retrieval taking the sample
 * out (optional)
 *
 * This enables full traceability: - Track complete custody chain for any sample
 * - Link outbound retrievals to original inbound transfers - Generate
 * chain-of-custody reports for compliance
 */
@Entity
@Table(name = "chain_of_custody_log", schema = "clinlims")
public class ChainOfCustodyLog extends BaseObject<Integer> {

    /**
     * Types of custody actions that can be logged.
     */
    public enum CustodyAction {
        // Inbound/Intake actions
        CHECKOUT_REQUESTED, // Retrieval request submitted
        CHECKOUT_APPROVED, // Supervisor approved request
        CHECKOUT_RETRIEVED, // Physically removed from storage
        CHECKOUT_RELEASED, // Handed to requester

        // External transfer actions
        TRANSFER_INITIATED, // External transfer started
        TRANSFER_IN_TRANSIT, // Being transported
        TRANSFER_RECEIVED, // Received at destination

        // Return actions
        RETURN_INITIATED, // Return process started
        RETURN_RECEIVED, // Back at biorepository
        RETURN_INSPECTED, // Condition verified
        RETURN_STORED // Returned to storage location
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "chain_of_custody_log_generator")
    @SequenceGenerator(name = "chain_of_custody_log_generator", sequenceName = "chain_of_custody_log_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @NotNull(message = "Sample item is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sample_item_id", nullable = false)
    private SampleItem sampleItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_in_request_id")
    private SampleTransferRequest transferInRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retrieval_request_id")
    private SampleRetrievalRequest retrievalRequest;

    @NotNull(message = "Custody action is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "custody_action", nullable = false, length = 30)
    private CustodyAction custodyAction;

    @Size(max = 255)
    @Column(name = "from_location", length = 255)
    private String fromLocation;

    @Size(max = 255)
    @Column(name = "to_location", length = 255)
    private String toLocation;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "from_custodian_user_id")
    private SystemUser fromCustodian;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "to_custodian_user_id")
    private SystemUser toCustodian;

    @NotNull(message = "Action timestamp is required")
    @Column(name = "action_timestamp", nullable = false)
    private Timestamp actionTimestamp;

    @Size(max = 100)
    @Column(name = "storage_coordinates", length = 100)
    private String storageCoordinates;

    @Column(name = "temperature", precision = 5, scale = 2)
    private BigDecimal temperature;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "sys_user_id", nullable = false, length = 36)
    private String sysUserId;

    public ChainOfCustodyLog() {
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public String getSysUserId() {
        return sysUserId;
    }

    @Override
    public void setSysUserId(String sysUserId) {
        this.sysUserId = sysUserId;
    }

    public SampleItem getSampleItem() {
        return sampleItem;
    }

    public void setSampleItem(SampleItem sampleItem) {
        this.sampleItem = sampleItem;
    }

    public SampleTransferRequest getTransferInRequest() {
        return transferInRequest;
    }

    public void setTransferInRequest(SampleTransferRequest transferInRequest) {
        this.transferInRequest = transferInRequest;
    }

    public SampleRetrievalRequest getRetrievalRequest() {
        return retrievalRequest;
    }

    public void setRetrievalRequest(SampleRetrievalRequest retrievalRequest) {
        this.retrievalRequest = retrievalRequest;
    }

    public CustodyAction getCustodyAction() {
        return custodyAction;
    }

    public void setCustodyAction(CustodyAction custodyAction) {
        this.custodyAction = custodyAction;
    }

    public String getFromLocation() {
        return fromLocation;
    }

    public void setFromLocation(String fromLocation) {
        this.fromLocation = fromLocation;
    }

    public String getToLocation() {
        return toLocation;
    }

    public void setToLocation(String toLocation) {
        this.toLocation = toLocation;
    }

    public SystemUser getFromCustodian() {
        return fromCustodian;
    }

    public void setFromCustodian(SystemUser fromCustodian) {
        this.fromCustodian = fromCustodian;
    }

    public SystemUser getToCustodian() {
        return toCustodian;
    }

    public void setToCustodian(SystemUser toCustodian) {
        this.toCustodian = toCustodian;
    }

    public Timestamp getActionTimestamp() {
        return actionTimestamp;
    }

    public void setActionTimestamp(Timestamp actionTimestamp) {
        this.actionTimestamp = actionTimestamp;
    }

    public String getStorageCoordinates() {
        return storageCoordinates;
    }

    public void setStorageCoordinates(String storageCoordinates) {
        this.storageCoordinates = storageCoordinates;
    }

    public BigDecimal getTemperature() {
        return temperature;
    }

    public void setTemperature(BigDecimal temperature) {
        this.temperature = temperature;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * Get the sample item ID for serialization.
     */
    public Integer getSampleItemId() {
        return sampleItem != null ? Integer.valueOf(sampleItem.getId()) : null;
    }

    /**
     * Get the transfer in request ID for serialization.
     */
    public Integer getTransferInRequestId() {
        return transferInRequest != null ? transferInRequest.getId() : null;
    }

    /**
     * Get the retrieval request ID for serialization.
     */
    public Integer getRetrievalRequestId() {
        return retrievalRequest != null ? retrievalRequest.getId() : null;
    }

    /**
     * Check if this is a checkout action.
     */
    public boolean isCheckoutAction() {
        return custodyAction != null && custodyAction.name().startsWith("CHECKOUT_");
    }

    /**
     * Check if this is a transfer action.
     */
    public boolean isTransferAction() {
        return custodyAction != null && custodyAction.name().startsWith("TRANSFER_");
    }

    /**
     * Check if this is a return action.
     */
    public boolean isReturnAction() {
        return custodyAction != null && custodyAction.name().startsWith("RETURN_");
    }

    /**
     * Factory method to create a custody log entry.
     */
    public static ChainOfCustodyLog create(SampleItem sampleItem, CustodyAction action, String sysUserId) {
        ChainOfCustodyLog log = new ChainOfCustodyLog();
        log.setSampleItem(sampleItem);
        log.setCustodyAction(action);
        log.setActionTimestamp(new Timestamp(System.currentTimeMillis()));
        log.setSysUserId(sysUserId);
        return log;
    }

    /**
     * Factory method to create a custody log entry with request linkage.
     */
    public static ChainOfCustodyLog createForRetrieval(SampleItem sampleItem, SampleRetrievalRequest retrievalRequest,
            SampleTransferRequest transferInRequest, CustodyAction action, String sysUserId) {
        ChainOfCustodyLog log = create(sampleItem, action, sysUserId);
        log.setRetrievalRequest(retrievalRequest);
        log.setTransferInRequest(transferInRequest);
        return log;
    }
}
