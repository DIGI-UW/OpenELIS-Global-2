package org.openelisglobal.sample.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.compliance.valueholder.ComplianceStandard;

@Entity
@Table(name = "sample_compliance_standards", schema = "clinlims")
public class SampleComplianceStandard extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_id", nullable = false)
    private Sample sample;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compliance_standard_id", nullable = false)
    private ComplianceStandard complianceStandard;

    @Column(name = "priority", nullable = false)
    private int priority = 0;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Sample getSample() {
        return sample;
    }

    public void setSample(Sample sample) {
        this.sample = sample;
    }

    public ComplianceStandard getComplianceStandard() {
        return complianceStandard;
    }

    public void setComplianceStandard(ComplianceStandard complianceStandard) {
        this.complianceStandard = complianceStandard;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
}
