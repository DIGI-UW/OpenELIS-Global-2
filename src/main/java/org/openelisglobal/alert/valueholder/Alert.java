package org.openelisglobal.alert.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.hibernate.type.JsonBinaryType;
import org.openelisglobal.systemuser.valueholder.SystemUser;

/**
 * Generic Alert entity that can be used across all OpenELIS features.
 *
 * <p>
 * This entity uses polymorphic mapping to link alerts to any entity type: -
 * alert_entity_type: Entity class name (e.g., "Freezer", "Equipment", "Sample")
 * - alert_entity_id: ID of the entity - alert_type: Type of alert
 * (FREEZER_TEMPERATURE, EQUIPMENT_FAILURE, etc.) - context_data: JSONB field
 * storing type-specific data
 *
 * <p>
 * Examples: - Freezer temperature alert: alertType=FREEZER_TEMPERATURE,
 * alertEntityType="Freezer", alertEntityId=5, contextData='{"temperature":
 * -15.5, "threshold": -20.0, "thresholdType": "CRITICAL_HIGH"}'
 * <p>
 * - Equipment failure alert: alertType=EQUIPMENT_FAILURE,
 * alertEntityType="Equipment", alertEntityId=12, contextData='{"errorCode":
 * "E-1234", "component": "Temperature Sensor"}'
 *
 * <p>
 * Alert lifecycle: OPEN → ACKNOWLEDGED → RESOLVED
 *
 * <p>
 * Deduplication: Uses last_duplicate_time and duplicate_count to prevent
 * duplicate alerts within a 30-minute window.
 */
@Getter
@Setter
@Entity
@Table(name = "alert", indexes = { @Index(name = "idx_alert_entity", columnList = "alert_entity_type,alert_entity_id"),
        @Index(name = "idx_alert_type", columnList = "alert_type"),
        @Index(name = "idx_alert_status", columnList = "status"),
        @Index(name = "idx_alert_severity", columnList = "severity"),
        @Index(name = "idx_alert_start_time", columnList = "start_time"),
        @Index(name = "idx_alert_status_severity", columnList = "status,severity") })
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class Alert extends BaseObject<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "alert_generator")
    @SequenceGenerator(name = "alert_generator", sequenceName = "alert_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", length = 50, nullable = false)
    private AlertType alertType;

    @Column(name = "alert_entity_type", length = 100, nullable = false)
    private String alertEntityType;

    @Column(name = "alert_entity_id", nullable = false)
    private Long alertEntityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 20, nullable = false)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private AlertStatus status;

    @Column(name = "start_time", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime startTime;

    @Column(name = "end_time", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime endTime;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Type(type = "jsonb")
    @Column(name = "context_data", columnDefinition = "jsonb")
    private String contextData;

    @Column(name = "acknowledged_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime acknowledgedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acknowledged_by")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private SystemUser acknowledgedBy;

    @Column(name = "resolved_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private SystemUser resolvedBy;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "last_duplicate_time", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime lastDuplicateTime;

    @Column(name = "duplicate_count", nullable = false)
    private Integer duplicateCount = 0;
}
