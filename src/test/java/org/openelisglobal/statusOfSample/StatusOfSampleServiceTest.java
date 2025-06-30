package org.openelisglobal.statusOfSample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.statusofsample.service.StatusOfSampleService;
import org.openelisglobal.statusofsample.valueholder.StatusOfSample;
import org.springframework.beans.factory.annotation.Autowired;

public class StatusOfSampleServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private StatusOfSampleService statusOfSampleService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/status-of-sample.xml");
    }

    @Test
    public void getData_shouldReturnDataGiveStausOfSample() {
        StatusOfSample statusOfSample = statusOfSampleService.get("2");
        statusOfSampleService.getData(statusOfSample);

        assertEquals("Sample Description 1", statusOfSample.getDescription());
        assertEquals("SampleType1", statusOfSample.getStatusType());
    }

    @Test
    public void getTotalStatusOfSampleCount() {
        int statusOfSamples = statusOfSampleService.getTotalStatusOfSampleCount();
        assertTrue(statusOfSamples >= 0);
    }

    @Test
    public void getAllStatusOfSamples() {
        List<StatusOfSample> statusOfSamples = statusOfSampleService.getAllStatusOfSamples();
        assertTrue(statusOfSamples.size() > 0);
        assertEquals("Status 1", statusOfSamples.get(0).getStatusOfSampleName());
        assertEquals("Status 2", statusOfSamples.get(1).getStatusOfSampleName());

    }

    @Test
    public void getPageOfStatusOfSamples() {
        List<StatusOfSample> statusOfSamples = statusOfSampleService.getPageOfStatusOfSamples(1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(statusOfSamples.size() <= expectedPages);
    }

    @Test
    public void getDataByStatusTypeAndStatusCode() {
        StatusOfSample statusOfSample = statusOfSampleService.get("2");
        StatusOfSample statusOfSampleData = statusOfSampleService.getDataByStatusTypeAndStatusCode(statusOfSample);
        assertEquals("Status 1", statusOfSampleData.getStatusOfSampleName());
    }

}
