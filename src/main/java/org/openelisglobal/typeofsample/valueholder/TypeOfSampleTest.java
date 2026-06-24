package org.openelisglobal.typeofsample.valueholder;

import org.openelisglobal.common.valueholder.BaseObject;

public class TypeOfSampleTest extends BaseObject<String> {
    private static final long serialVersionUID = 1L;

    private String id;
    private String typeOfSampleId;
    private String testId;
    private Integer displayOrder;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTypeOfSampleId() {
        return typeOfSampleId;
    }

    public void setTypeOfSampleId(String typeOfSampleId) {
        this.typeOfSampleId = typeOfSampleId;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    /** Per-sample-type display position of this test (OGC-949 M12 / OGC-985). */
    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
