package org.openelisglobal.dataexchange.service.orderresult;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

public class HL7MessageOutServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private HL7MessageOutService hL7MessageOutService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/HL7Message-out.xml");
    }

    @Test
    public void getByData_ShouldReturnHL7MessageOutUsingTheDataPassedAsParameter() {
        // TODO: Method not yet implemented.
        // HL7MessageOut message = hL7MessageOutService.getByData("You are requested
        // submit the tests");
        //
        // assertNotNull(message);
        // assertEquals("2", message.getId());
        // assertEquals("SENT", message.getStatus());
    }
}
