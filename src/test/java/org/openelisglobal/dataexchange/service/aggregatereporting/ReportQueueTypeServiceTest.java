package org.openelisglobal.dataexchange.service.aggregatereporting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dataexchange.aggregatereporting.valueholder.ReportQueueType;
import org.springframework.beans.factory.annotation.Autowired;

public class ReportQueueTypeServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ReportQueueTypeService reportQueueTypeService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/report-queue-type.xml");
    }

    @Test
    public void getReportQueueTypeByName_ShouldReturnReportQueueType_UsingPassedParameter() {
        ReportQueueType reportQueueType = reportQueueTypeService.getReportQueueTypeByName("CriticalAlerts");
        assertNotNull(reportQueueType);
        assertEquals("3", reportQueueType.getId());
    }

    @Test
    public void getAllReportQueueTypes_ShouldReturnAllReportQueueTypes() {
        List<ReportQueueType> reportQueueTypes = reportQueueTypeService.getAll();
        assertNotNull(reportQueueTypes);
        assertEquals(3, reportQueueTypes.size());
        assertEquals("2", reportQueueTypes.get(1).getId());
    }
}
