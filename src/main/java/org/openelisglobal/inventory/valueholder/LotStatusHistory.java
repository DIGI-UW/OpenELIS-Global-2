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
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * LotStatusHistory entity - Tracks QC status changes for inventory lots
 *
 * Records status transitions: PENDING -> PASSED/FAILED Example: Lot received
 * (PENDING), QC test performed, result recorded, status updated to
 * PASSED/FAILED
 */
@Getter
@Setter
@Entity
@Table(name = "lot_status_history", schema = "clinlims")
@DynamicUpdate
@org.hibernate.annotations.OptimisticLocking(type = org.hibernate.annotations.OptimisticLockType.VERSION)
public class LotStatusHistory extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id", nullable = false)
    private InventoryLot lot;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_from", length = 20)
    private QCStatus statusFrom;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_to", length = 20, nullable = false)
    private QCStatus statusTo;

    @Column(name = "qc_test_id", length = 36)
    private String qcTestId;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    @Column(name = "changed_by_user", nullable = false)
    private Integer changedByUser;

    @Column(name = "changed_date", nullable = false)
    private Timestamp changedDate;

    @PrePersist
    private void onPrePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (changedDate == null) {
            changedDate = new Timestamp(System.currentTimeMillis());
        }
    }
}
