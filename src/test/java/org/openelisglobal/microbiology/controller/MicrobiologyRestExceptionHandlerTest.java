package org.openelisglobal.microbiology.controller;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import org.junit.Test;
import org.openelisglobal.microbiology.controller.rest.MicrobiologyRestExceptionHandler;
import org.openelisglobal.microbiology.service.MicroCaseLockedException;
import org.springframework.http.ResponseEntity;

public class MicrobiologyRestExceptionHandlerTest {

    @Test
    public void finalCaseMutationReturnsNamedConflict() {
        ResponseEntity<Map<String, Object>> response = new MicrobiologyRestExceptionHandler()
                .handleLockedCase(new MicroCaseLockedException("Final-released microbiology cases cannot be changed"));

        assertEquals(409, response.getStatusCode().value());
        assertEquals("MICROBIOLOGY_CASE_LOCKED", response.getBody().get("error"));
        assertEquals("Final-released microbiology cases cannot be changed", response.getBody().get("message"));
    }
}
