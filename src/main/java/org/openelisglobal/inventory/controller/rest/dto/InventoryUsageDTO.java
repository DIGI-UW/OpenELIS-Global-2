package org.openelisglobal.inventory.controller.rest.dto;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * InventoryUsageDTO
 *
 * Data Transfer Object for equipment/inventory usage records with enriched
 * information (user names, lab unit names) instead of just IDs.
 *
 * Used for returning usage history to the frontend with human-readable data.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryUsageDTO {

    private Long id;
    private Long inventoryItemId;
    private String inventoryItemName;
    private Long lotId;
    private String lotNumber;
    private Double quantityUsed;
    private Timestamp usageDate;

    // Enriched user data (not just ID)
    private Integer performedByUserId;
    private String performedByUserName;

    // Optional linked records
    private Long testResultId;
    private Long analysisId;
}
