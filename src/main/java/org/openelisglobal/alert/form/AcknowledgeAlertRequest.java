package org.openelisglobal.alert.form;

import lombok.Data;

/**
 * Request DTO for acknowledging an alert. Task Reference: T046
 */
@Data
public class AcknowledgeAlertRequest {
    private Integer userId;
    private String notes; // Optional acknowledgment notes
}
