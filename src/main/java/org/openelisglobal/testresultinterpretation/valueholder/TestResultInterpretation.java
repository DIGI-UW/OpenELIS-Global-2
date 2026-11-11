package org.openelisglobal.testresultinterpretation.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * OGC-949 M5 / OGC-937 — a result interpretation rule for a result component
 * (e.g. value "POS" → "Detected", severity ABNORMAL, red). Hangs off a
 * {@code test_result_component} via {@code component_id} (a VARCHAR(36) UUID
 * FK, hence a plain String — no LIMSStringNumberUserType). The {@code @Version}
 * column ({@code last_updated}) comes from {@link BaseObject}.
 */
@Entity
@Table(name = "test_result_interpretation", schema = "clinlims")
public class TestResultInterpretation extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "component_id", nullable = false, length = 36)
    private String componentId;

    @Column(name = "value_match", length = 80)
    private String valueMatch;

    @Column(name = "interpretation_text", length = 255)
    private String interpretationText;

    @Column(name = "severity", length = 20)
    private String severity;

    @Column(name = "color", length = 20)
    private String color;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false, length = 2)
    private String isActive = "Y";

    public TestResultInterpretation() {
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

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public String getValueMatch() {
        return valueMatch;
    }

    public void setValueMatch(String valueMatch) {
        this.valueMatch = valueMatch;
    }

    public String getInterpretationText() {
        return interpretationText;
    }

    public void setInterpretationText(String interpretationText) {
        this.interpretationText = interpretationText;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getIsActive() {
        return isActive;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }
}
