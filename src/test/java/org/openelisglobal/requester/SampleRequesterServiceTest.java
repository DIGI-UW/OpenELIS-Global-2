package org.openelisglobal.requester;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

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
        List<RequesterType> requesterTypes = requesterTypeService.getAll();

        assertNotNull("Sample requester list should not be null", sampleRequesters);
        assertFalse("Sample requester list should not be empty", sampleRequesters.isEmpty());
        assertNotNull("Requester type list should not be null", requesterTypes);
        assertFalse("Requester type list should not be empty", requesterTypes.isEmpty());

        sampleRequesters.forEach(requester -> {
            assertNotNull("SampleRequester ID should not be null", requester.getId());
            assertNotNull("SampleRequester Sample ID should not be null", requester.getSampleId());
            assertNotNull("SampleRequester Requester ID should not be null", requester.getRequesterId());
            assertNotNull("SampleRequester Requester Type ID should not be null", requester.getRequesterTypeId());
        });

        requesterTypes.forEach(type -> {
            assertNotNull("RequesterType ID should not be null", type.getId());
            assertNotNull("RequesterType name should not be null", type.getRequesterType());
        });
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