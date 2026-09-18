package org.openelisglobal.accreditation.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * OGC-686 — an accrediting body the laboratory holds accreditation from.
 *
 * <p>
 * {@code expiresOn} is the certificate expiry for the body's <em>whole</em>
 * accredited scope. Per-test rows ({@link TestAccreditation}) carry no expiry
 * of their own: a real certificate is renewed as one instrument, and a
 * mid-cycle scope extension inherits the certificate's date. Renewal is
 * therefore one update here, not one per enrolled test.
 *
 * <p>
 * {@code logoImageId} points at {@code clinlims.image} — the DB-backed store
 * that {@code Report.createReportParameters()} already streams into
 * JasperReports. It is a {@code numeric} FK mapped to String via
 * {@code LIMSStringNumberUserType}, the OpenELIS idiom for numeric-keyed legacy
 * tables. A null logo is legitimate: the body still contributes to the report's
 * accreditation notes line, it just has no image to render.
 *
 * <p>
 * The {@code @Version} column {@code last_updated} comes from
 * {@link BaseObject}; the DB-filled {@code lastupdated} is a distinct
 * wall-clock column and is intentionally not mapped.
 */
@Entity
@Table(name = "accrediting_body", schema = "clinlims")
public class AccreditingBody extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "accrediting_body_seq_gen")
    @SequenceGenerator(name = "accrediting_body_seq_gen", sequenceName = "accrediting_body_id_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    /** Short code (e.g. ISO15189). Unique, and immutable once created. */
    @Column(name = "code", nullable = false, length = 16)
    private String code;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "logo_image_id")
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String logoImageId;

    @Column(name = "expires_on", nullable = false)
    private LocalDate expiresOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "logo_visibility_mode", nullable = false, length = 32)
    private LogoVisibilityMode logoVisibilityMode = LogoVisibilityMode.ANY_ACCREDITED_TEST;

    /** Only consulted when the mode is {@code PERCENTAGE}; always stored. */
    @Column(name = "threshold_pct", nullable = false)
    private Short thresholdPct = 80;

    /**
     * Ascending order for side-by-side report logos; ties break on {@link #code}.
     */
    @Column(name = "display_order", nullable = false)
    private Short displayOrder = 0;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    public AccreditingBody() {
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogoImageId() {
        return logoImageId;
    }

    public void setLogoImageId(String logoImageId) {
        this.logoImageId = logoImageId;
    }

    public LocalDate getExpiresOn() {
        return expiresOn;
    }

    public void setExpiresOn(LocalDate expiresOn) {
        this.expiresOn = expiresOn;
    }

    public LogoVisibilityMode getLogoVisibilityMode() {
        return logoVisibilityMode;
    }

    public void setLogoVisibilityMode(LogoVisibilityMode logoVisibilityMode) {
        this.logoVisibilityMode = logoVisibilityMode;
    }

    public Short getThresholdPct() {
        return thresholdPct;
    }

    public void setThresholdPct(Short thresholdPct) {
        this.thresholdPct = thresholdPct;
    }

    public Short getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Short displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AccreditingBody that = (AccreditingBody) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
