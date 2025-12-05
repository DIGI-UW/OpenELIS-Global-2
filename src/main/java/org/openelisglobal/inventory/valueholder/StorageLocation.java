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
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * StorageLocation entity - Physical locations where inventory items are stored
 *
 * Supports hierarchical locations: SITE -> ROOM -> CABINET -> SHELF Example:
 * "Main Building" -> "Supply Room 1" -> "Cabinet A" -> "Shelf 2"
 */
@Getter
@Setter
@Entity
@Table(name = "storage_location", schema = "clinlims")
@DynamicUpdate
@org.hibernate.annotations.OptimisticLocking(type = org.hibernate.annotations.OptimisticLockType.VERSION)
public class StorageLocation extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "location_code", length = 50, nullable = false, unique = true)
    private String locationCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", length = 50, nullable = false)
    private LocationType locationType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_location_id")
    private StorageLocation parentLocation;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "temperature_min", precision = 5, scale = 2)
    private BigDecimal temperatureMin;

    @Column(name = "temperature_max", precision = 5, scale = 2)
    private BigDecimal temperatureMax;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @PrePersist
    private void onPrePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (isActive == null) {
            isActive = true;
        }
    }

    /**
     * Get full hierarchical path (e.g., "Main Building > Supply Room 1 > Cabinet
     * A")
     */
    public String getFullPath() {
        if (parentLocation != null) {
            return parentLocation.getFullPath() + " > " + name;
        }
        return name;
    }
}
