package org.openelisglobal.testconfiguration.service;

import java.util.List;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SampleTypeTestAssignServiceImpl implements SampleTypeTestAssignService {

    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private TypeOfSampleTestService typeOfSampleTestService;

    @Override
    @Transactional
    public void update(TypeOfSample typeOfSample, String testId, List<String> typeOfSamplesTestIDs, String sampleTypeId,
            boolean deleteExistingTypeOfSampleTest, boolean updateTypeOfSample, TypeOfSample deActivateTypeOfSample,
            String systemUserId) {
        if (deleteExistingTypeOfSampleTest) {
            for (String typeOfSamplesTestID : typeOfSamplesTestIDs) {
                typeOfSampleTestService.delete(typeOfSamplesTestID, systemUserId);
            }
        }

        if (updateTypeOfSample) {
            typeOfSampleService.update(typeOfSample);
        }

        TypeOfSampleTest typeOfSampleTest = new TypeOfSampleTest();
        typeOfSampleTest.setTestId(testId);
        typeOfSampleTest.setTypeOfSampleId(sampleTypeId);
        typeOfSampleTest.setSysUserId(systemUserId);
        typeOfSampleTest.setLastupdatedFields();

        typeOfSampleTestService.insert(typeOfSampleTest);

        if (deActivateTypeOfSample != null) {
            typeOfSampleService.update(deActivateTypeOfSample);
        }
    }
}
