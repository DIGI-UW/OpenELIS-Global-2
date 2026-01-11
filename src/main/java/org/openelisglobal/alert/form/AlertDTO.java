package org.openelisglobal.alert.form;

import java.time.OffsetDateTime;
import lombok.Data;

/**
 * Generic Alert DTO for REST API. Task Reference: T044
 */
@Data
public class AlertDTO {
    private Long id;
    private String alertType;
    private String alertEntityType;
    private Long alertEntityId;
    private String severity;
    private String status;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private String message;
    private String contextData;
    private OffsetDateTime acknowledgedAt;
    private Integer acknowledgedBy;
    private OffsetDateTime resolvedAt;
    private Integer resolvedBy;
    private String resolutionNotes;
    private Integer duplicateCount;
    private OffsetDateTime lastDuplicateTime;
    private FreezerDTO freezer;
}
