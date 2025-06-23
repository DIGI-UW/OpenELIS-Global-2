package org.openelisglobal.dataexchange.service.orderresult;

import org.junit.Before;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

public class HL7MessageOutServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private HL7MessageOutService hL7MessageOutService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/HL7Message-out.xml");
    }

//    TODO: Method not yet implemented.
//    @Test
//    public void testGetByData(){
//        HL7MessageOut message = hL7MessageOutService.getByData("You are requested");
//
//        assertNotNull(message);
//        assertEquals("2", message.getId());
//        assertEquals("SENT", message.getStatus());
//    }
}
