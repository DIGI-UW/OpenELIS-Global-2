package org.openelisglobal.testconfiguration.service;

import java.util.List;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;

public interface TestActivationService {

    void updateAll(List<Test> deactivateTests, List<Test> activateTests, List<TypeOfSample> deactivateSampleTypes,
            List<TypeOfSample> activateSampleTypes);
}
