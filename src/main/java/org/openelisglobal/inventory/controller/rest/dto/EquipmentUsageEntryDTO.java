package org.openelisglobal.inventory.controller.rest.dto;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * EquipmentUsageEntryDTO
 *
 * Extended Data Transfer Object for equipment usage entries that includes all
 * form fields from the Usage Log (activities, operator name, login/logout
 * times, etc.) in addition to inventory usage data.
 *
 * Returned by the equipment usage submission endpoint with complete form data.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentUsageEntryDTO {

    // Base inventory usage fields
    private Long id;
    private Long inventoryItemId;
    private String inventoryItemName;
    private Long lotId;
    private String lotNumber;
    private Double quantityUsed;
    private Timestamp usageDate;

    // User information
    private Integer performedByUserId;
    private String performedByUserName;

    // Optional linked records
    private Long testResultId;
    private Long analysisId;

    // Equipment usage form fields
    private String operatorName;
    private String date;
    private String loginTime;
    private String activities;
    private String equipmentStatus;
    private String logoutTime;

    // Approval fields
    private String approvedBy;
    private String approvalDate;
}
