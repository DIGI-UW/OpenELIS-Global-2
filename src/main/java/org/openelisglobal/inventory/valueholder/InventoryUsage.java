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
 * InventoryUsage entity - Links inventory consumption to specific test results
 *
 * Enables traceability: which reagent lot was used for which test? Critical for
 * quality control and recall management.
 *
 * Example: HIV test on sample #12345 used RDT lot #ABC123
 */
@Getter
@Setter
@Entity
@Table(name = "inventory_usage", schema = "clinlims")
@DynamicUpdate
@org.hibernate.annotations.OptimisticLocking(type = org.hibernate.annotations.OptimisticLockType.VERSION)
public class InventoryUsage extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id", nullable = false)
    private InventoryLot lot;

    @Column(name = "test_result_id", length = 36)
    private String testResultId;

    @Column(name = "analysis_id", length = 36)
    private String analysisId;

    @Column(name = "quantity_used", nullable = false)
    private Integer quantityUsed;

    @Column(name = "usage_date", nullable = false)
    private Timestamp usageDate;

    @Column(name = "performed_by_user", nullable = false)
    private Integer performedByUser;

    @PrePersist
    private void onPrePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (usageDate == null) {
            usageDate = new Timestamp(System.currentTimeMillis());
        }
    }
}
