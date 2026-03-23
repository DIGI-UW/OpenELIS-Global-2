package org.openelisglobal.alert.form;

import lombok.Data;

/**
 * Request DTO for resolving an alert. Task Reference: T047
 */
@Data
public class ResolveAlertRequest {
    private Integer userId;
    private String resolutionNotes;
}
