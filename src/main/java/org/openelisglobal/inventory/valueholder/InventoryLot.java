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
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 */
package org.openelisglobal.inventory.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.sql.Date;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * InventoryLot entity - Represents a specific lot/batch of an inventory item
 * Maps to inventory_location table (historical naming)
 *
 * Tracks lot number, expiration, quantities, QC status, and physical storage
 * location Implements FEFO (First Expired, First Out) inventory management
 */
@Getter
@Setter
@Entity
@Table(name = "inventory_location", schema = "clinlims")
@DynamicUpdate
@org.hibernate.annotations.OptimisticLocking(type = org.hibernate.annotations.OptimisticLockType.VERSION)
public class InventoryLot extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "fhir_uuid", nullable = false, unique = true)
    private UUID fhirUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    @Column(name = "lot_number", length = 100, nullable = false)
    private String lotNumber;

    @Column(name = "expiration_date", nullable = false)
    private Date expirationDate;

    @Column(name = "initial_quantity", nullable = false)
    private Integer initialQuantity;

    @Column(name = "current_quantity", nullable = false)
    private Integer currentQuantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_location_id")
    private StorageLocation storageLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "qc_status", length = 20, nullable = false)
    private QCStatus qcStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private LotStatus status;

    @Column(name = "barcode", length = 100, unique = true)
    private String barcode;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @PrePersist
    private void onPrePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (fhirUuid == null) {
            fhirUuid = UUID.randomUUID();
        }
        if (qcStatus == null) {
            qcStatus = QCStatus.PENDING;
        }
        if (status == null) {
            status = LotStatus.ACTIVE;
        }
        if (version == null) {
            version = 0;
        }
    }

    /**
     * Check if lot is available for consumption (QC passed, active, has quantity)
     */
    public boolean isAvailableForConsumption() {
        return status == LotStatus.ACTIVE && qcStatus == QCStatus.PASSED && currentQuantity > 0;
    }

    /**
     * Check if lot is expired based on expiration date
     */
    public boolean isExpired() {
        return expirationDate != null && expirationDate.before(new Date(System.currentTimeMillis()));
    }
}
