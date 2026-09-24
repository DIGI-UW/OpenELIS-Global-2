package org.openelisglobal.accreditation.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Objects;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * OGC-686 — one test's membership in an accrediting body's scope.
 *
 * <p>
 * A test is "accredited by body X" if and only if a row exists here. Expiry is
 * deliberately absent: it lives on {@link AccreditingBody#getExpiresOn()},
 * because a certificate is renewed as a whole and a scope extension inherits
 * the certificate's date. {@code effectiveFrom} records when a test entered
 * scope and is provenance only — nothing computes status from it.
 *
 * <p>
 * {@code testId} is a {@code numeric(10)} FK to {@code clinlims.test}, mapped
 * to String via {@code LIMSStringNumberUserType} — the same idiom
 * {@code QiConfig} uses for {@code test_category_id}. It is <em>not</em> a
 * varchar/UUID: the nearest-looking precedent ({@code test_method}) is varchar
 * with no FK at all, and copying it would make a real foreign key impossible.
 *
 * <p>
 * Uniqueness on (test, body) is enforced by
 * {@code uq_test_accreditation_test_body} in {@code liquibase/qa/013}.
 */
@Entity
@Table(name = "test_accreditation", schema = "clinlims")
public class TestAccreditation extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "test_accreditation_seq_gen")
    @SequenceGenerator(name = "test_accreditation_seq_gen", sequenceName = "test_accreditation_id_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "test_id", nullable = false, precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String testId;

    @Column(name = "accrediting_body_id", nullable = false)
    private Long accreditingBodyId;

    /** When this test entered the body's scope. Provenance only. */
    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    public TestAccreditation() {
        super();
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public Long getAccreditingBodyId() {
        return accreditingBodyId;
    }

    public void setAccreditingBodyId(Long accreditingBodyId) {
        this.accreditingBodyId = accreditingBodyId;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TestAccreditation that = (TestAccreditation) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
