package org.openelisglobal.eqa.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Opt-in list of users who may be recorded as the analyst on a scheme's samples
 * (FR-V2.1-08). An empty list for a scheme means any user may be recorded — so
 * absence of rows is permissive, not restrictive.
 *
 * <p>
 * There is no analyst master table in OpenELIS; analysts are system_user rows.
 */
@Getter
@Setter
@Entity
@Table(name = "eqa_scheme_analyst", uniqueConstraints = @UniqueConstraint(name = "uq_eqa_scheme_analyst_scheme_user", columnNames = {
        "scheme_id", "system_user_id" }))
public class EQASchemeAnalyst extends BaseObject<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "eqa_scheme_analyst_generator")
    @SequenceGenerator(name = "eqa_scheme_analyst_generator", sequenceName = "eqa_scheme_analyst_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheme_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private EQAProgram scheme;

    @Column(name = "system_user_id", nullable = false)
    private Long systemUserId;

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
