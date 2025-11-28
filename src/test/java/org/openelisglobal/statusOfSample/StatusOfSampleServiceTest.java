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

        // Find records by name rather than assuming position (order may vary)
        StatusOfSample status1 = statusOfSamples.stream().filter(s -> "Status 1".equals(s.getStatusOfSampleName()))
                .findFirst().orElse(null);
        StatusOfSample status2 = statusOfSamples.stream().filter(s -> "Status 2".equals(s.getStatusOfSampleName()))
                .findFirst().orElse(null);

        assertTrue("Status 1 should be in the list", status1 != null);
        assertTrue("Status 2 should be in the list", status2 != null);
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
