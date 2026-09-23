package org.openelisglobal.testconfiguration.service;

import java.util.List;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.security.access.prepost.PreAuthorize;

public interface TestActivationService {

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    void updateAll(List<Test> deactivateTests, List<Test> activateTests, List<TypeOfSample> deactivateSampleTypes,
            List<TypeOfSample> activateSampleTypes);
}
