package org.openelisglobal.test.dto;

import java.util.List;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testmethod.service.TestMethodService.TestMethodDto;

/** Complete test metadata needed to restore an order selection. */
public class TestSelectionDTO {

    private final String id;
    private final String name;
    private final String description;
    private final boolean opensMicrobiologyCase;
    private final String microbiologyCaseRole;
    private final boolean collectedInSets;
    private final List<TestMethodDto> methods;

    public TestSelectionDTO(Test test, List<TestMethodDto> methods) {
        this.id = test.getId();
        this.name = test.getLocalizedName();
        this.description = test.getDescription();
        this.opensMicrobiologyCase = test.isOpensMicrobiologyCase();
        this.microbiologyCaseRole = test.getMicrobiologyCaseRole();
        this.collectedInSets = test.isCollectedInSets();
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

    public boolean isOpensMicrobiologyCase() {
        return opensMicrobiologyCase;
    }

    public String getMicrobiologyCaseRole() {
        return microbiologyCaseRole;
    }

    public boolean isCollectedInSets() {
        return collectedInSets;
    }

    public List<TestMethodDto> getMethods() {
        return methods;
    }
}
