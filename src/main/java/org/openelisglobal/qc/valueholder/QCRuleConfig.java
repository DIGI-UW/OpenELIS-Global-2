package org.openelisglobal.qc.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;

@Getter
@Setter
@Entity
@Table(name = "qc_rule_config", uniqueConstraints = @UniqueConstraint(name = "uk_qc_rule_config_rule_test", columnNames = {
        "rule_code", "test_type_id" }))
public class QCRuleConfig extends BaseObject<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "qc_rule_config_generator")
    @SequenceGenerator(name = "qc_rule_config_generator", sequenceName = "qc_rule_config_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "rule_code", nullable = false, length = 50)
    private String ruleCode;

    @Column(name = "test_type_id", nullable = false)
    private Long testTypeId;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;
}
