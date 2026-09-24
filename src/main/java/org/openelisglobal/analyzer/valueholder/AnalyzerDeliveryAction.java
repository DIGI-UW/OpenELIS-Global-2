package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.hibernate.converter.StringToIntegerConverter;

/**
 * Immutable attribution for one manual action on an undelivered analyzer
 * result. The Bridge records only OpenELIS's service account, so the acting
 * OpenELIS user is retained here. analyzerId is null when the outbox entry came
 * from a sender that matches no OpenELIS analyzer.
 *
 * <p>
 * The identifier is numeric rather than a UUID because audit history stores
 * reference_id numerically, so an audited row must carry a numeric id.
 */
@Entity
@Table(name = "analyzer_delivery_action", schema = "clinlims")
@Immutable
@Getter
@Setter
public class AnalyzerDeliveryAction extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", precision = 10, scale = 0)
    @GeneratedValue(generator = "analyzer_delivery_action_seq_gen")
    @GenericGenerator(name = "analyzer_delivery_action_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @Parameter(name = "sequence_name", value = "analyzer_delivery_action_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;

    @Column(name = "outbox_entry_id", length = 255, nullable = false, updatable = false)
    private String outboxEntryId;

    @Column(name = "action", length = 16, nullable = false, updatable = false)
    private String action;

    @Convert(converter = StringToIntegerConverter.class)
    @Column(name = "analyzer_id", updatable = false)
    private String analyzerId;

    @Column(name = "actor", length = 36, nullable = false, updatable = false)
    private String actor;

    @Column(name = "acted_at", nullable = false, updatable = false)
    private Timestamp actedAt;

    @PrePersist
    protected void prepareForInsert() {
        if (actedAt == null) {
            actedAt = Timestamp.from(Instant.now());
        }
    }
}
