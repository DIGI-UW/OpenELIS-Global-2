package org.openelisglobal.sample.override.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * A deliberate decision to proceed past a control on an order.
 *
 * Ordering without a patient and continuing with testing after a failed
 * acceptance check are the same act, so they share one record: without it the
 * user simply proceeded and nothing was kept — no reason, no user, no timestamp
 * — and a patient-less order was indistinguishable from one whose patient had
 * merely been forgotten.
 */
@Getter
@Setter
@Entity
@Table(name = "sample_order_override")
public class SampleOrderOverride extends BaseObject<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sample_order_override_generator")
    @SequenceGenerator(name = "sample_order_override_generator", sequenceName = "sample_order_override_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "sample_id", nullable = false)
    private Long sampleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "override_type", nullable = false, length = 40)
    private OverrideType overrideType;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false, length = 40)
    private OverrideReasonCode reasonCode;

    @Column(name = "reason")
    private String reason;

    @Column(name = "sys_user_id")
    private Long overrideUserId;

    @Column(name = "recorded_date", nullable = false)
    private Timestamp recordedDate;
}
