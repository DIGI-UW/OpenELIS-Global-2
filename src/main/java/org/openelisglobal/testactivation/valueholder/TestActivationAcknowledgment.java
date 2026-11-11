package org.openelisglobal.testactivation.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.UUID;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * OGC-949 M7 / OGC-973 — audit row recording that a user acknowledged a test's
 * reference-range coverage gaps in order to activate it (the H-03 safety gate).
 * Insert-only.
 *
 * <p>
 * {@code test_id} / {@code user_id} are numeric FKs (String via
 * LIMSStringNumberUserType). {@code acknowledged_at} is owned by the DB
 * (DEFAULT now()), so it is read-only here. {@code gaps_acknowledged} stores
 * the coverage-gap report as JSONB — written with an explicit {@code ?::jsonb}
 * cast.
 */
@Entity
@Table(name = "test_activation_acknowledgment", schema = "clinlims")
public class TestActivationAcknowledgment extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "test_id", nullable = false, precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String testId;

    @Column(name = "user_id", nullable = false, precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String userId;

    @Column(name = "acknowledged_at", insertable = false, updatable = false)
    private Timestamp acknowledgedAt;

    @Column(name = "gaps_acknowledged")
    @ColumnTransformer(write = "?::jsonb")
    private String gapsAcknowledged;

    public TestActivationAcknowledgment() {
        super();
        this.id = UUID.randomUUID().toString();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Timestamp getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public String getGapsAcknowledged() {
        return gapsAcknowledged;
    }

    public void setGapsAcknowledged(String gapsAcknowledged) {
        this.gapsAcknowledged = gapsAcknowledged;
    }
}
