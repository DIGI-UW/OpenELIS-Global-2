package org.openelisglobal.esig.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.sql.Timestamp;
import java.util.Objects;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Records a user's one-time certification per 21 CFR Part 11 §11.100(c).
 *
 * <p>
 * Before first use, users must certify that their electronic signature is the
 * legally binding equivalent of their handwritten signature.
 *
 * <p>
 * This table is append-only. Certifications can only be revoked by an admin
 * (which deletes the record), forcing the user to re-certify.
 */
@Entity
@Table(name = "esig_first_use_certification")
public class EsigFirstUseCertification extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "esig_first_use_certification_seq")
    @SequenceGenerator(name = "esig_first_use_certification_seq", sequenceName = "esig_first_use_certification_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    /**
     * Foreign key to system_user. The user who certified.
     */
    @NotNull(message = "User ID is required")
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /**
     * Timestamp when the user completed certification.
     */
    @NotNull(message = "Certified at timestamp is required")
    @Column(name = "certified_at", nullable = false)
    private Timestamp certifiedAt;

    /**
     * The exact legal text the user acknowledged. Stored so that changes to the
     * template don't alter existing certifications.
     */
    @NotBlank(message = "Certification text is required")
    @Column(name = "certification_text", nullable = false, columnDefinition = "TEXT")
    private String certificationText;

    /**
     * IP address of the client at time of certification.
     */
    @Size(max = 45)
    @Column(name = "client_ip", length = 45)
    private String clientIp;

    /**
     * Browser user agent string at time of certification.
     */
    @Size(max = 500)
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    public EsigFirstUseCertification() {
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Timestamp getCertifiedAt() {
        return certifiedAt;
    }

    public void setCertifiedAt(Timestamp certifiedAt) {
        this.certifiedAt = certifiedAt;
    }

    public String getCertificationText() {
        return certificationText;
    }

    public void setCertificationText(String certificationText) {
        this.certificationText = certificationText;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        EsigFirstUseCertification that = (EsigFirstUseCertification) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
