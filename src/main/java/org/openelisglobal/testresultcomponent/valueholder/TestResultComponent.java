package org.openelisglobal.testresultcomponent.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * OGC-949 M5 / OGC-937 — a labeled result field of a test (e.g. systolic vs.
 * diastolic). Every test has at least a PRIMARY component (auto-created by the
 * M1 backfill). Result options ({@code test_result.component_id}), ranges
 * ({@code result_limits.component_id}), and interpretations
 * ({@code test_result_interpretation.component_id}) all hang off a component.
 *
 * <p>
 * Numeric FK columns ({@code test_id}, {@code uom_id}) map to String via
 * {@code LIMSStringNumberUserType}, the established OpenELIS idiom (see
 * {@code TestMethod}). The audit {@code @Version} column ({@code last_updated})
 * comes from {@link BaseObject}; the table's separate {@code lastupdated}
 * (DEFAULT now()) is filled by the DB and not mapped here.
 */
@Entity
@Table(name = "test_result_component", schema = "clinlims")
public class TestResultComponent extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "test_id", nullable = false, precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String testId;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "label", nullable = false, length = 255)
    private String label;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "result_type", length = 1)
    private String resultType;

    @Column(name = "uom_id", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String uomId;

    @Column(name = "significant_digits", precision = 10, scale = 0)
    private Integer significantDigits;

    @Column(name = "default_result", length = 80)
    private String defaultResult;

    @Column(name = "allow_multiple_readings", nullable = false)
    private boolean allowMultipleReadings = false;

    @Column(name = "is_active", nullable = false, length = 2)
    private String isActive = "Y";

    public TestResultComponent() {
        super();
        this.id = UUID.randomUUID().toString();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getResultType() {
        return resultType;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    public String getUomId() {
        return uomId;
    }

    public void setUomId(String uomId) {
        this.uomId = uomId;
    }

    public Integer getSignificantDigits() {
        return significantDigits;
    }

    public void setSignificantDigits(Integer significantDigits) {
        this.significantDigits = significantDigits;
    }

    public String getDefaultResult() {
        return defaultResult;
    }

    public void setDefaultResult(String defaultResult) {
        this.defaultResult = defaultResult;
    }

    public boolean getAllowMultipleReadings() {
        return allowMultipleReadings;
    }

    public void setAllowMultipleReadings(boolean allowMultipleReadings) {
        this.allowMultipleReadings = allowMultipleReadings;
    }

    public String getIsActive() {
        return isActive;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }
}
