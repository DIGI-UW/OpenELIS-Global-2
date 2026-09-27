package org.openelisglobal.test.dto;

import java.util.List;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testmethod.service.TestMethodService.TestMethodDto;

/** Complete test metadata needed to restore an order selection. */
public class TestSelectionDTO {

    private final String id;
    private final String name;
    private final String description;
    private final String cultureWorkflowType;
    private final List<TestMethodDto> methods;

    public TestSelectionDTO(Test test, List<TestMethodDto> methods) {
        this.id = test.getId();
        this.name = test.getLocalizedName();
        this.description = test.getDescription();
        this.cultureWorkflowType = test.getCultureWorkflowType();
        this.methods = methods;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCultureWorkflowType() {
        return cultureWorkflowType;
    }

    public List<TestMethodDto> getMethods() {
        return methods;
    }
}
