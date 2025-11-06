package org.openelisglobal.testconfiguration.service;

import java.util.List;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;

public interface SampleTypeTestAssignService {

    void update(TypeOfSample typeOfSample, String testId, List<String> typeOfSamplesTestIDs, String sampleTypeId,
            boolean deleteExistingTypeOfSampleTest, boolean updateTypeOfSample, TypeOfSample deActivateTypeOfSample,
            String systemUserId);
}
