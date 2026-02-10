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
@Table(name = "qc_frequency_config", uniqueConstraints = @UniqueConstraint(name = "uk_qc_frequency_config_instrument", columnNames = {
        "instrument_id" }))
public class QCFrequencyConfig extends BaseObject<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "qc_frequency_config_generator")
    @SequenceGenerator(name = "qc_frequency_config_generator", sequenceName = "qc_frequency_config_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "instrument_id", nullable = false)
    private Long instrumentId;

    @Column(name = "frequency_type", nullable = false, length = 30)
    private String frequencyType;

    @Column(name = "frequency_value")
    private Integer frequencyValue;
}
