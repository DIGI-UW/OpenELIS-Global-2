package org.openelisglobal.eqa.valueholder;

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

@Getter
@Setter
@Entity
@Table(name = "eqa_program_test", uniqueConstraints = @UniqueConstraint(name = "uk_eqa_program_test", columnNames = {
        "eqa_program_id", "test_id" }))
public class EQAProgramTest extends BaseObject<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "eqa_program_test_generator")
    @SequenceGenerator(name = "eqa_program_test_generator", sequenceName = "eqa_program_test_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eqa_program_id", nullable = false)
    private EQAProgram eqaProgram;

    @Column(name = "test_id", nullable = false)
    private Long testId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

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
