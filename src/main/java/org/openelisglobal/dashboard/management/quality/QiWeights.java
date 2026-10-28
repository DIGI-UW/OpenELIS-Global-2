package org.openelisglobal.dashboard.management.quality;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "qi_weights", schema = "clinlims")
public class QiWeights extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "qi_weights_seq_gen")
    @SequenceGenerator(name = "qi_weights_seq_gen", sequenceName = "qi_weights_id_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "qc_weight", nullable = false, precision = 3, scale = 2)
    private BigDecimal qcWeight;

    @Column(name = "eqa_weight", nullable = false, precision = 3, scale = 2)
    private BigDecimal eqaWeight;

    @Column(name = "nce_inverse_weight", nullable = false, precision = 3, scale = 2)
    private BigDecimal nceInverseWeight;

    @Column(name = "amendment_inverse_weight", nullable = false, precision = 3, scale = 2)
    private BigDecimal amendmentInverseWeight;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getQcWeight() {
        return qcWeight;
    }

    public void setQcWeight(BigDecimal qcWeight) {
        this.qcWeight = qcWeight;
    }

    public BigDecimal getEqaWeight() {
        return eqaWeight;
    }

    public void setEqaWeight(BigDecimal eqaWeight) {
        this.eqaWeight = eqaWeight;
    }

    public BigDecimal getNceInverseWeight() {
        return nceInverseWeight;
    }

    public void setNceInverseWeight(BigDecimal nceInverseWeight) {
        this.nceInverseWeight = nceInverseWeight;
    }

    public BigDecimal getAmendmentInverseWeight() {
        return amendmentInverseWeight;
    }

    public void setAmendmentInverseWeight(BigDecimal amendmentInverseWeight) {
        this.amendmentInverseWeight = amendmentInverseWeight;
    }
}
