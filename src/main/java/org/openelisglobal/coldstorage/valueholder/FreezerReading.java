package org.openelisglobal.coldstorage.valueholder;

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
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;

@Getter
@Setter
@Entity
@Table(name = "freezer_reading", indexes = {
        @Index(name = "idx_freezer_reading_name_time", columnList = "freezer_id, recorded_at"),
        @Index(name = "idx_freezer_reading_status", columnList = "status") })
public class FreezerReading extends BaseObject<Long> {

    public enum Status {
        NORMAL, WARNING, CRITICAL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "freezer_reading_generator")
    @SequenceGenerator(name = "freezer_reading_generator", sequenceName = "freezer_reading_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freezer_id", nullable = false)
    private Freezer freezer;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    @Column(name = "temperature_celsius")
    private BigDecimal temperatureCelsius;

    @Column(name = "humidity_percentage")
    private BigDecimal humidityPercentage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false)
    private Status status = Status.NORMAL;

    @Column(name = "transmission_ok", nullable = false)
    private Boolean transmissionOk = Boolean.TRUE;

    @Column(name = "error_message")
    private String errorMessage;
}
