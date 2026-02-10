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

    // Store sampleId directly instead of @OneToOne to avoid cross-mapping issues
    // between JPA annotations and HBM XML mapping (Sample uses
    // LIMSStringNumberUserType)
    @Column(name = "sample_id", nullable = false, unique = true)
    private Long sampleId;

    @Column(name = "is_eqa_sample", nullable = false)
    private Boolean isEqaSample = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eqa_program_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private EQAProgram eqaProgram;

    @Column(name = "eqa_provider_organization_id")
    private Long eqaProviderOrganizationId;

    @Column(name = "eqa_provider_sample_id", length = 100)
    private String eqaProviderSampleId;

    @Column(name = "eqa_participant_id", length = 100)
    private String eqaParticipantId;

    @Column(name = "eqa_deadline")
    private Timestamp eqaDeadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "eqa_priority", length = 20)
    private EQAPriority eqaPriority = EQAPriority.STANDARD;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eqa_distribution_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private EQADistribution eqaDistribution;

    @Column(name = "sys_user_id", length = 36, nullable = false)
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
