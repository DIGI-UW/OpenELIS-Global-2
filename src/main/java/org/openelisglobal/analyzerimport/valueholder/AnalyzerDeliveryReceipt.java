package org.openelisglobal.analyzerimport.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.hibernate.converter.StringToIntegerConverter;

/**
 * Durable acceptance evidence, retained independently of result staging and
 * review.
 */
@Entity
@Table(name = "analyzer_delivery_receipt", uniqueConstraints = @UniqueConstraint(name = "uq_analyzer_delivery_receipt", columnNames = {
        "connection_id", "message_id" }))
@Getter
@Setter
public class AnalyzerDeliveryReceipt extends BaseObject<String> {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "analyzer_delivery_receipt_uuid")
    @GenericGenerator(name = "analyzer_delivery_receipt_uuid", strategy = "uuid2")
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "connection_id", length = 255, nullable = false)
    private String connectionId;
    @Column(name = "message_id", length = 255, nullable = false)
    private String messageId;
    @Convert(converter = StringToIntegerConverter.class)
    @Column(name = "analyzer_id", nullable = false)
    private String analyzerId;
    @Column(name = "profile_id", length = 128, nullable = false)
    private String profileId;
    @Column(name = "profile_revision", nullable = false)
    private int profileRevision;
    @Column(name = "results_staged", nullable = false)
    private int resultsStaged;
    @Column(name = "results_held", nullable = false)
    private int resultsHeld;
    @Column(name = "controls_processed", nullable = false)
    private int controlsProcessed;
    @Column(name = "accepted_by", length = 36, nullable = false)
    private String acceptedBy;
    @Column(name = "accepted_at", nullable = false)
    private Timestamp acceptedAt;
}
