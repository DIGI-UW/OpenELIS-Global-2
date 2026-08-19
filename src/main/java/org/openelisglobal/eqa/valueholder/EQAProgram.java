package org.openelisglobal.eqa.valueholder;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.test.valueholder.TestSection;

@Getter
@Setter
@Entity
@Table(name = "eqa_program")
public class EQAProgram extends BaseObject<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "eqa_program_generator")
    @SequenceGenerator(name = "eqa_program_generator", sequenceName = "eqa_program_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "fhir_uuid", nullable = false, unique = true)
    private UUID fhirUuid;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "provider", length = 255)
    private String provider;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "test_section_id")
    private TestSection testSection;

    @Column(name = "frequency", length = 50)
    private String frequency;

    /**
     * Arrangement type (FR-V2.1-06, gate G1 alter-in-place). V1 rows default to
     * INTERNATIONAL_PT. BR-004: provider required unless IN_HOUSE — enforced in
     * EQAProgramServiceImpl.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "scheme_type", nullable = false, length = 30)
    private EQASchemeType schemeType = EQASchemeType.INTERNATIONAL_PT;

    /**
     * FR-V2.1-09. When true the participant cycle stops at ready_to_submit for a QA
     * officer's single confirmation on the Review &amp; Submit panel (FR-V2.2-07)
     * and T-14's auto-submit stands down. Off by default, so schemes that predate
     * the flag keep auto-submitting.
     */
    @Column(name = "requires_cycle_review", nullable = false)
    private Boolean requiresCycleReview = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

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

    @PrePersist
    public void prePersist() {
        if (fhirUuid == null) {
            fhirUuid = UUID.randomUUID();
        }
    }
}
