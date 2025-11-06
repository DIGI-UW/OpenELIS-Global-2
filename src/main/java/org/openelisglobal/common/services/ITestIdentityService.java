package org.openelisglobal.common.services;

import org.openelisglobal.test.valueholder.Test;

public interface ITestIdentityService {

    public abstract boolean doesTestExist(String name);

    public abstract boolean doesActiveTestExist(String name);

    public abstract boolean doesPanelExist(String name);

    public abstract boolean doesTestExistForLoinc(String loincCode);

    public abstract boolean doesActiveTestExistForLoinc(String loincCode);

    public abstract boolean isTestNumericViralLoad(String testId);

    public abstract boolean isTestNumericViralLoad(Test test);

    public abstract boolean doesActivePanelExistForLoinc(String loinc);
}
