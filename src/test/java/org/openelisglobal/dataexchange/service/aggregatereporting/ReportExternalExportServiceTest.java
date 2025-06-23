package org.openelisglobal.dataexchange.service.aggregatereporting;

import static org.junit.Assert.assertEquals;

import java.sql.Timestamp;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

public class ReportExternalExportServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ReportExternalExportService reportExternalExportService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/report-external-export.xml");
    }

    @Test
    public void testGetLastCollectedTimestamp() {
        Timestamp lastTimestamp = reportExternalExportService.getLastCollectedTimestamp();
        assertEquals(Timestamp.valueOf("2023-01-11 00:00:00"), lastTimestamp);
    }

}
