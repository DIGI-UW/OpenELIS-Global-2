package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.*;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "micro_molecular_dst_binding", schema = "clinlims")
public class MicroMolecularDstBinding extends BaseObject<String> {
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

    @Column(name = "test_id", nullable = false, length = 20)
    private String testId;

    public String getTestId() {
        return testId;
    }

    public void setTestId(String value) {
        testId = value;
    }

    @Column(name = "component_id", nullable = false, length = 36)
    private String componentId;

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String value) {
        componentId = value;
    }

    @Column(name = "antibiotic_id", nullable = false, length = 36)
    private String antibioticId;

    public String getAntibioticId() {
        return antibioticId;
    }

    public void setAntibioticId(String value) {
        antibioticId = value;
    }

    @Column(name = "susceptible_value", nullable = false, length = 200)
    private String susceptibleValue;

    public String getSusceptibleValue() {
        return susceptibleValue;
    }

    public void setSusceptibleValue(String value) {
        susceptibleValue = value;
    }

    @Column(name = "resistant_value", nullable = false, length = 200)
    private String resistantValue;

    public String getResistantValue() {
        return resistantValue;
    }

    public void setResistantValue(String value) {
        resistantValue = value;
    }
}
