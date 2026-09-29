package org.openelisglobal.testconfiguration.service;

import java.util.List;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SampleTypeTestAssignService {

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    void update(TypeOfSample typeOfSample, String testId, List<String> typeOfSamplesTestIDs, String sampleTypeId,
            boolean deleteExistingTypeOfSampleTest, boolean updateTypeOfSample, TypeOfSample deActivateTypeOfSample,
            String systemUserId);
}
