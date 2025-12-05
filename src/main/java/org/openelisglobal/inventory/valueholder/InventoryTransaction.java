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
import java.sql.Timestamp;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Immutable;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * InventoryTransaction entity - Immutable audit trail of all inventory quantity
 * changes
 *
 * Records every change to inventory lot quantities for complete traceability: -
 * RECEIPT: Initial stock received - CONSUMPTION: Used for testing - ADJUSTMENT:
 * Manual corrections - TRANSFER: Moved between locations - DISPOSAL: Discarded
 * (expired, damaged, etc.)
 */
@Getter
@Setter
@Entity
@Table(name = "inventory_transaction", schema = "clinlims")
@Immutable
@DynamicUpdate
public class InventoryTransaction extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id", nullable = false)
    private InventoryLot lot;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", length = 50, nullable = false)
    private TransactionType transactionType;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "transaction_date", nullable = false)
    private Timestamp transactionDate;

    @Column(name = "reference_id", length = 36)
    private String referenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", length = 50)
    private ReferenceType referenceType;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "performed_by_user", nullable = false)
    private Integer performedByUser;

    @PrePersist
    private void onPrePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (transactionDate == null) {
            transactionDate = new Timestamp(System.currentTimeMillis());
        }
    }
}
