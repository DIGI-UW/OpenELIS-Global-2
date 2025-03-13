package org.openelisglobal.requester;

import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.requester.service.RequesterTypeService;
import org.openelisglobal.requester.service.SampleRequesterService;
import org.openelisglobal.requester.valueholder.RequesterType;
import org.openelisglobal.requester.valueholder.SampleRequester;
import org.springframework.beans.factory.annotation.Autowired;

public class SampleRequesterServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    SampleRequesterService sampleRequesterService;

    @Autowired
    RequesterTypeService requesterTypeService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/requester.xml");
    }

    @Test
    public void verifyTestData() {
        List<SampleRequester> sampleRequesters = sampleRequesterService.getAll();
        System.out.println("Sample Requesters in DB: " + sampleRequesters.size());
        sampleRequesters.forEach(requester -> System.out
                .println(requester.getId() + " - Sample ID: " + requester.getSampleId() + ", Requester ID: "
                        + requester.getRequesterId() + ", Requester Type ID: " + requester.getRequesterTypeId()));

        List<RequesterType> requesterTypes = requesterTypeService.getAll();
        System.out.println("Requester Types in DB: " + requesterTypes.size());
        requesterTypes.forEach(type -> System.out.println(type.getId() + " - " + type.getRequesterType()));

        Assert.assertFalse("❌ sample_requester table should not be empty!", sampleRequesters.isEmpty());
        Assert.assertFalse("❌ requester_type table should not be empty!", requesterTypes.isEmpty());
    }

    @Test
    public void getRequestersForSampleId_shouldReturnCorrectRequesters() {
        List<SampleRequester> requesters = sampleRequesterService.getRequestersForSampleId("1");
        Assert.assertNotNull("Requesters list should not be null", requesters);
        Assert.assertEquals("Expected exactly 1 requester for sample_id=1", 1, requesters.size());
    }

    @Test
    public void getAll_shouldReturnAllRequesters() {
        List<SampleRequester> requesters = sampleRequesterService.getAll();
        Assert.assertNotNull("Requesters list should not be null", requesters);
        Assert.assertTrue("Requesters list should not be empty", !requesters.isEmpty());
    }

}