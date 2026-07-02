package org.openelisglobal.alert.form;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * Generic Alert DTO for REST API. Task Reference: T044
 *
 * <p>
 * Date fields are forced to ISO-8601 strings ({@link JsonFormat.Shape#STRING})
 * rather than Jackson's default numeric-timestamp shape. {@code OffsetDateTime}
 * serializes as epoch *seconds* by default, while legacy {@code
 * java.sql.Timestamp} fields elsewhere in the app serialize as epoch
 * *milliseconds* — both look like a plain number on the wire, so a consumer has
 * no way to tell which unit it received without guessing. That ambiguity caused
 * alert durations to read as ~56 years and chart axes to show 1970 (issue
 * #3743). An explicit string removes the guesswork entirely.
 */
@Data
public class AlertDTO {
    private Long id;
    private String alertType;
    private String alertEntityType;
    private Long alertEntityId;
    private String severity;
    private String status;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private OffsetDateTime startTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private OffsetDateTime endTime;
    private String message;
    private String contextData;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private OffsetDateTime acknowledgedAt;
    private Integer acknowledgedBy;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private OffsetDateTime resolvedAt;
    private Integer resolvedBy;
    private String resolutionNotes;
    private Integer duplicateCount;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private OffsetDateTime lastDuplicateTime;
    private FreezerDTO freezer;
}
