package org.openelisglobal.eqa.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;

@Getter
@Setter
@Entity
@Table(name = "sample_eqa")
public class SampleEQA extends BaseObject<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sample_eqa_generator")
    @SequenceGenerator(name = "sample_eqa_generator", sequenceName = "sample_eqa_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "sample_id", nullable = false, unique = true)
    private Long sampleId;

    @Column(name = "is_eqa_sample", nullable = false)
    private Boolean isEqaSample = false;

    @Column(name = "eqa_enrollment_id")
    private Long eqaEnrollmentId;

    @Column(name = "eqa_provider_sample_id", length = 100)
    private String eqaProviderSampleId;

    @Column(name = "eqa_deadline")
    private Timestamp eqaDeadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "eqa_priority", length = 20)
    private EQAPriority eqaPriority = EQAPriority.STANDARD;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eqa_distribution_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private EQADistribution eqaDistribution;

    @Column(name = "sys_user_id", nullable = false)
    private String sysUserId;

    @Override
    public String getSysUserId() {
        return sysUserId;
    }

    @Override
    public void setSysUserId(String sysUserId) {
        this.sysUserId = sysUserId;
    }
}
