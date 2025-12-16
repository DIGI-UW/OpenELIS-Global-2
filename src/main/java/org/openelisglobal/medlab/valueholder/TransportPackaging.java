/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.medlab.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * TransportPackaging entity for tracking IATA PI650 compliant packaging.
 *
 * <p>
 * Tracks three-tier packaging system:
 *
 * <ul>
 * <li>Primary: Individual sample containers (vacutainer, cryovial)
 * <li>Secondary: Protective packaging (biohazard bag, canister)
 * <li>Tertiary: Outer shipping container (insulated shipper, rigid box)
 * </ul>
 *
 * <p>
 * Supports compliance with IATA Packing Instruction 650 (PI650) for Category B
 * infectious substances.
 */
@Entity
@Table(name = "transport_packaging")
public class TransportPackaging extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    public enum SealStatus {
        INTACT, LEAKING, DAMAGED
    }

    public enum SecondaryIntegrity {
        INTACT, TORN, WET, LEAKING
    }

    public enum ReceiptCondition {
        ACCEPTABLE, REJECTED
    }

    public enum ArrivalCondition {
        INTACT, DAMAGED
    }

    public enum TransportationStatus {
        ON_TIME, DELAYED
    }

    public enum CollectionTemperature {
        ROOM_TEMP, REFRIGERATED, FROZEN, NA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transport_packaging_seq")
    @SequenceGenerator(name = "transport_packaging_seq", sequenceName = "transport_packaging_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "shipment_id", nullable = false, length = 50)
    private String shipmentId;

    // Primary container (individual sample containers)
    @Column(name = "primary_container_type", nullable = false, length = 50)
    private String primaryContainerType;

    @Column(name = "primary_seal_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SealStatus primarySealStatus;

    @Column(name = "primary_barcode_present", nullable = false)
    private Boolean primaryBarcodePresent;

    @Column(name = "primary_absorbent_present", nullable = false)
    private Boolean primaryAbsorbentPresent;

    // Secondary packaging (protective layer)
    @Column(name = "secondary_packaging_type", nullable = false, length = 50)
    private String secondaryPackagingType;

    @Column(name = "secondary_integrity", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SecondaryIntegrity secondaryIntegrity;

    @Column(name = "secondary_watertight", nullable = false)
    private Boolean secondaryWatertight;

    @Column(name = "secondary_container_count", nullable = false)
    private Integer secondaryContainerCount;

    @Column(name = "secondary_inspector_id", nullable = false)
    private Integer secondaryInspectorId;

    @Column(name = "secondary_inspection_time", nullable = false)
    private Timestamp secondaryInspectionTime;

    @Column(name = "secondary_receipt_condition", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ReceiptCondition secondaryReceiptCondition;

    // Tertiary packaging (outer shipping container)
    @Column(name = "tertiary_box_type", nullable = false, length = 50)
    private String tertiaryBoxType;

    @Column(name = "tertiary_labeling_status", nullable = false, length = 100)
    private String tertiaryLabelingStatus;

    @Column(name = "tertiary_temp_logger_id", length = 50)
    private String tertiaryTempLoggerId;

    @Column(name = "tertiary_courier_company", length = 100)
    private String tertiaryCourierCompany;

    @Column(name = "tertiary_tracking_number", length = 50)
    private String tertiaryTrackingNumber;

    @Column(name = "tertiary_arrival_condition", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ArrivalCondition tertiaryArrivalCondition;

    // Overall compliance
    @Column(name = "iata_pi650_compliant", nullable = false)
    private Boolean iataPi650Compliant;

    @Column(name = "transportation_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TransportationStatus transportationStatus;

    @Column(name = "collection_temperature", length = 20)
    @Enumerated(EnumType.STRING)
    private CollectionTemperature collectionTemperature;

    @Column(name = "received_date", nullable = false)
    private Timestamp receivedDate;

    @Column(name = "fhir_uuid", unique = true)
    private UUID fhirUuid;

    public TransportPackaging() {
        super();
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(String shipmentId) {
        this.shipmentId = shipmentId;
    }

    public String getPrimaryContainerType() {
        return primaryContainerType;
    }

    public void setPrimaryContainerType(String primaryContainerType) {
        this.primaryContainerType = primaryContainerType;
    }

    public SealStatus getPrimarySealStatus() {
        return primarySealStatus;
    }

    public void setPrimarySealStatus(SealStatus primarySealStatus) {
        this.primarySealStatus = primarySealStatus;
    }

    public Boolean getPrimaryBarcodePresent() {
        return primaryBarcodePresent;
    }

    public void setPrimaryBarcodePresent(Boolean primaryBarcodePresent) {
        this.primaryBarcodePresent = primaryBarcodePresent;
    }

    public Boolean getPrimaryAbsorbentPresent() {
        return primaryAbsorbentPresent;
    }

    public void setPrimaryAbsorbentPresent(Boolean primaryAbsorbentPresent) {
        this.primaryAbsorbentPresent = primaryAbsorbentPresent;
    }

    public String getSecondaryPackagingType() {
        return secondaryPackagingType;
    }

    public void setSecondaryPackagingType(String secondaryPackagingType) {
        this.secondaryPackagingType = secondaryPackagingType;
    }

    public SecondaryIntegrity getSecondaryIntegrity() {
        return secondaryIntegrity;
    }

    public void setSecondaryIntegrity(SecondaryIntegrity secondaryIntegrity) {
        this.secondaryIntegrity = secondaryIntegrity;
    }

    public Boolean getSecondaryWatertight() {
        return secondaryWatertight;
    }

    public void setSecondaryWatertight(Boolean secondaryWatertight) {
        this.secondaryWatertight = secondaryWatertight;
    }

    public Integer getSecondaryContainerCount() {
        return secondaryContainerCount;
    }

    public void setSecondaryContainerCount(Integer secondaryContainerCount) {
        this.secondaryContainerCount = secondaryContainerCount;
    }

    public Integer getSecondaryInspectorId() {
        return secondaryInspectorId;
    }

    public void setSecondaryInspectorId(Integer secondaryInspectorId) {
        this.secondaryInspectorId = secondaryInspectorId;
    }

    public Timestamp getSecondaryInspectionTime() {
        return secondaryInspectionTime;
    }

    public void setSecondaryInspectionTime(Timestamp secondaryInspectionTime) {
        this.secondaryInspectionTime = secondaryInspectionTime;
    }

    public ReceiptCondition getSecondaryReceiptCondition() {
        return secondaryReceiptCondition;
    }

    public void setSecondaryReceiptCondition(ReceiptCondition secondaryReceiptCondition) {
        this.secondaryReceiptCondition = secondaryReceiptCondition;
    }

    public String getTertiaryBoxType() {
        return tertiaryBoxType;
    }

    public void setTertiaryBoxType(String tertiaryBoxType) {
        this.tertiaryBoxType = tertiaryBoxType;
    }

    public String getTertiaryLabelingStatus() {
        return tertiaryLabelingStatus;
    }

    public void setTertiaryLabelingStatus(String tertiaryLabelingStatus) {
        this.tertiaryLabelingStatus = tertiaryLabelingStatus;
    }

    public String getTertiaryTempLoggerId() {
        return tertiaryTempLoggerId;
    }

    public void setTertiaryTempLoggerId(String tertiaryTempLoggerId) {
        this.tertiaryTempLoggerId = tertiaryTempLoggerId;
    }

    public String getTertiaryCourierCompany() {
        return tertiaryCourierCompany;
    }

    public void setTertiaryCourierCompany(String tertiaryCourierCompany) {
        this.tertiaryCourierCompany = tertiaryCourierCompany;
    }

    public String getTertiaryTrackingNumber() {
        return tertiaryTrackingNumber;
    }

    public void setTertiaryTrackingNumber(String tertiaryTrackingNumber) {
        this.tertiaryTrackingNumber = tertiaryTrackingNumber;
    }

    public ArrivalCondition getTertiaryArrivalCondition() {
        return tertiaryArrivalCondition;
    }

    public void setTertiaryArrivalCondition(ArrivalCondition tertiaryArrivalCondition) {
        this.tertiaryArrivalCondition = tertiaryArrivalCondition;
    }

    public Boolean getIataPi650Compliant() {
        return iataPi650Compliant;
    }

    public void setIataPi650Compliant(Boolean iataPi650Compliant) {
        this.iataPi650Compliant = iataPi650Compliant;
    }

    public TransportationStatus getTransportationStatus() {
        return transportationStatus;
    }

    public void setTransportationStatus(TransportationStatus transportationStatus) {
        this.transportationStatus = transportationStatus;
    }

    public CollectionTemperature getCollectionTemperature() {
        return collectionTemperature;
    }

    public void setCollectionTemperature(CollectionTemperature collectionTemperature) {
        this.collectionTemperature = collectionTemperature;
    }

    public Timestamp getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(Timestamp receivedDate) {
        this.receivedDate = receivedDate;
    }

    public UUID getFhirUuid() {
        return fhirUuid;
    }

    public void setFhirUuid(UUID fhirUuid) {
        this.fhirUuid = fhirUuid;
    }

    /**
     * Calculates IATA PI650 compliance based on packaging requirements. All
     * conditions must be met for compliance.
     *
     * @return true if fully compliant, false otherwise
     */
    public boolean calculateCompliance() {
        return primarySealStatus == SealStatus.INTACT && Boolean.TRUE.equals(primaryBarcodePresent)
                && Boolean.TRUE.equals(primaryAbsorbentPresent) && secondaryIntegrity == SecondaryIntegrity.INTACT
                && Boolean.TRUE.equals(secondaryWatertight) && secondaryReceiptCondition == ReceiptCondition.ACCEPTABLE
                && tertiaryArrivalCondition == ArrivalCondition.INTACT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TransportPackaging that = (TransportPackaging) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
