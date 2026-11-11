package org.openelisglobal.dashboard.management.surveillance.valueholder;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "test_surveillance_mapping", schema = "clinlims")
@AttributeOverride(name = "lastupdated", column = @Column(name = "lastupdated"))
public class TestSurveillanceMapping extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    public enum MatchMode {
        SPECIFIC_CODES, ALL_ABNORMAL, NUMERIC_BELOW, NUMERIC_ABOVE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "test_surveillance_mapping_seq_gen")
    @SequenceGenerator(name = "test_surveillance_mapping_seq_gen", sequenceName = "test_surveillance_mapping_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "test_id", nullable = false, precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String testId;

    @Column(name = "program_id", nullable = false)
    private Long programId;

    @Column(name = "metric_label", nullable = false, length = 128)
    private String metricLabel;

    @Column(name = "metric_key", nullable = false, length = 64)
    private String metricKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_mode", nullable = false, length = 20)
    private MatchMode matchMode;

    /** Comma-separated dictionary result values, compared case-insensitively. */
    @Column(name = "positive_result_codes", length = 512)
    private String positiveResultCodes;

    @Column(name = "denominator_result_codes", length = 512)
    private String denominatorResultCodes;

    @Column(name = "numeric_threshold", precision = 15, scale = 5)
    private BigDecimal numericThreshold;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public Long getProgramId() {
        return programId;
    }

    public void setProgramId(Long programId) {
        this.programId = programId;
    }

    public String getMetricLabel() {
        return metricLabel;
    }

    public void setMetricLabel(String metricLabel) {
        this.metricLabel = metricLabel;
    }

    public String getMetricKey() {
        return metricKey;
    }

    public void setMetricKey(String metricKey) {
        this.metricKey = metricKey;
    }

    public MatchMode getMatchMode() {
        return matchMode;
    }

    public void setMatchMode(MatchMode matchMode) {
        this.matchMode = matchMode;
    }

    public String getPositiveResultCodes() {
        return positiveResultCodes;
    }

    public void setPositiveResultCodes(String positiveResultCodes) {
        this.positiveResultCodes = positiveResultCodes;
    }

    public String getDenominatorResultCodes() {
        return denominatorResultCodes;
    }

    public void setDenominatorResultCodes(String denominatorResultCodes) {
        this.denominatorResultCodes = denominatorResultCodes;
    }

    public BigDecimal getNumericThreshold() {
        return numericThreshold;
    }

    public void setNumericThreshold(BigDecimal numericThreshold) {
        this.numericThreshold = numericThreshold;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
