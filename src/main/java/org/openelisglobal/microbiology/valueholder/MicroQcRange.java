package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "micro_qc_range", schema = "clinlims")
public class MicroQcRange extends BaseObject<String> {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String value) {
        id = value;
    }

    @Column(name = "qc_strain", nullable = false, length = 200)
    private String qcStrain;

    public String getQcStrain() {
        return qcStrain;
    }

    public void setQcStrain(String value) {
        qcStrain = value;
    }

    @Column(name = "antibiotic_id", nullable = false, length = 36)
    private String antibioticId;

    public String getAntibioticId() {
        return antibioticId;
    }

    public void setAntibioticId(String value) {
        antibioticId = value;
    }

    @Column(name = "method", nullable = false, length = 10)
    private String method;

    public String getMethod() {
        return method;
    }

    public void setMethod(String value) {
        method = value;
    }

    @Column(name = "disk_content", length = 40)
    private String diskContent;

    public String getDiskContent() {
        return diskContent;
    }

    public void setDiskContent(String value) {
        diskContent = value;
    }

    @Column(name = "standard_id", nullable = false, length = 36)
    private String standardId;

    public String getStandardId() {
        return standardId;
    }

    public void setStandardId(String value) {
        standardId = value;
    }

    @Column(name = "low", precision = 12, scale = 4, nullable = false)
    private BigDecimal low;

    public BigDecimal getLow() {
        return low;
    }

    public void setLow(BigDecimal value) {
        low = value;
    }

    @Column(name = "high", precision = 12, scale = 4, nullable = false)
    private BigDecimal high;

    public BigDecimal getHigh() {
        return high;
    }

    public void setHigh(BigDecimal value) {
        high = value;
    }
}
