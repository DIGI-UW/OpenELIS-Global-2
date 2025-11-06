package org.openelisglobal.typeofsample.valueholder;

import org.openelisglobal.common.valueholder.BaseObject;

public class TypeOfSampleTest extends BaseObject<String> {
    private static final long serialVersionUID = 1L;

    private String id;
    private String typeOfSampleId;
    private String testId;

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
}
