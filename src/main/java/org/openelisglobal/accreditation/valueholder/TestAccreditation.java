package org.openelisglobal.accreditation.valueholder;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.test.valueholder.Test;

@Entity
@Table(name = "test_accreditation", uniqueConstraints = {
        @UniqueConstraint(name = "uq_test_accreditation_unique", columnNames = { "test_id", "accrediting_body_id" }) })
@Access(AccessType.FIELD)
public class TestAccreditation extends BaseObject<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "test_accreditation_generator")
    @SequenceGenerator(name = "test_accreditation_generator", sequenceName = "test_accreditation_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "test_id", nullable = false, foreignKey = @ForeignKey(name = "fk_test_accreditation_test"))
    @NotNull(message = "Test is required")
    private Test test;

    @ManyToOne(optional = false)
    @JoinColumn(name = "accrediting_body_id", nullable = false, foreignKey = @ForeignKey(name = "fk_test_accreditation_body"))
    @NotNull(message = "Accrediting body is required")
    private AccreditingBody accreditingBody;

    @Column(name = "expires_on", nullable = false)
    @NotNull(message = "Expiration date is required")
    private LocalDate expiresOn;

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

    public Test getTest() {
        return test;
    }

    public void setTest(Test test) {
        this.test = test;
    }

    public AccreditingBody getAccreditingBody() {
        return accreditingBody;
    }

    public void setAccreditingBody(AccreditingBody accreditingBody) {
        this.accreditingBody = accreditingBody;
    }

    public LocalDate getExpiresOn() {
        return expiresOn;
    }

    public void setExpiresOn(LocalDate expiresOn) {
        this.expiresOn = expiresOn;
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

    public boolean isActive() {
        LocalDate today = LocalDate.now();
        return expiresOn != null && !expiresOn.isBefore(today) && accreditingBody != null
                && Boolean.TRUE.equals(accreditingBody.getActive());
    }

    public boolean isExpired() {
        LocalDate today = LocalDate.now();
        return expiresOn != null && expiresOn.isBefore(today);
    }

    public boolean isExpiringWithin(int daysFromNow) {
        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusDays(daysFromNow);
        return expiresOn != null && !expiresOn.isBefore(today) && !expiresOn.isAfter(futureDate);
    }
}
