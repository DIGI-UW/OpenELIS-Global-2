package org.openelisglobal.accreditation.valueholder;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDateTime;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "accrediting_body")
@Access(AccessType.FIELD)
public class AccreditingBody extends BaseObject<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum LogoVisibilityMode {
        ANY_ACCREDITED_TEST, PERCENTAGE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "accrediting_body_generator")
    @SequenceGenerator(name = "accrediting_body_generator", sequenceName = "accrediting_body_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 16)
    @NotNull(message = "Code is required")
    @Size(min = 2, max = 16, message = "Code must be between 2 and 16 characters")
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "Code must contain only uppercase letters, numbers, and hyphens")
    private String code;

    @Column(name = "name", nullable = false, length = 120)
    @NotNull(message = "Name is required")
    @Size(min = 1, max = 120, message = "Name must be between 1 and 120 characters")
    private String name;

    @Column(name = "logo_path", columnDefinition = "TEXT")
    private String logoPath;

    @Column(name = "logo_visibility_mode", nullable = false, length = 50)
    @NotNull(message = "Logo visibility mode is required")
    @Enumerated(EnumType.STRING)
    private LogoVisibilityMode logoVisibilityMode = LogoVisibilityMode.ANY_ACCREDITED_TEST;

    @Column(name = "threshold_pct", nullable = false)
    @NotNull(message = "Threshold percentage is required")
    @Min(value = 0, message = "Threshold percentage must be at least 0")
    @Max(value = 100, message = "Threshold percentage must not exceed 100")
    private Short thresholdPct = 80;

    @Column(name = "display_order", nullable = false)
    @NotNull(message = "Display order is required")
    private Short displayOrder = 0;

    @Column(name = "active", nullable = false)
    @NotNull(message = "Active flag is required")
    private Boolean active = true;

    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "updated_on", nullable = false)
    private LocalDateTime updatedOn;

    @PrePersist
    protected void onCreate() {
        createdOn = LocalDateTime.now();
        updatedOn = createdOn;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedOn = LocalDateTime.now();
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

    public String getLogoPath() {
        return logoPath;
    }

    public void setLogoPath(String logoPath) {
        this.logoPath = logoPath;
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

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(LocalDateTime createdOn) {
        this.createdOn = createdOn;
    }

    public LocalDateTime getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(LocalDateTime updatedOn) {
        this.updatedOn = updatedOn;
    }
}
