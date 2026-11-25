package org.openelisglobal.configuration.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.configuration.service.CatalogImportService;
import org.openelisglobal.configuration.service.CatalogImportService.ImportPlan;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * The import endpoints answer a refused batch with a 422 that names the reason,
 * whether the service could not place a file or could not keep it (OGC-1228),
 * so the page never has to read an empty result as a success.
 */
public class CatalogImportRestControllerTest {

    private CatalogImportService importService;
    private CatalogImportRestController controller;
    private MockHttpServletRequest request;

    @Before
    public void setUp() {
        importService = mock(CatalogImportService.class);
        controller = new CatalogImportRestController();
        ReflectionTestUtils.setField(controller, "importService", importService);
        request = new MockHttpServletRequest();
        UserSessionData user = new UserSessionData();
        user.setSytemUserId(7);
        request.setAttribute(IActionConstants.USER_SESSION_DATA, user);
    }

    @Test
    public void apply_answersAFileThatCouldNotBeKeptWith422AndTheReason() {
        when(importService.apply(anyList(), any(), eq("7"))).thenThrow(new IllegalStateException(
                "Could not save test-sections-cphl.csv to /cfg/test-sections: permission denied"));

        ResponseEntity<ImportPlan> response = controller.apply(List.of(csv("test-sections-cphl.csv")), null,
                request);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertEquals(1, response.getBody().files().size());
        assertEquals("Could not save test-sections-cphl.csv to /cfg/test-sections: permission denied",
                response.getBody().files().get(0).error());
        assertNull(response.getBody().importRunId());
    }

    @Test
    public void apply_answersAFileItCannotPlaceWith422() {
        when(importService.apply(anyList(), any(), eq("7"))).thenThrow(
                new IllegalArgumentException("Cannot tell which catalog domain 'unnamed.csv' belongs to"));

        ResponseEntity<ImportPlan> response = controller.apply(List.of(csv("unnamed.csv")), null, request);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertEquals("Cannot tell which catalog domain 'unnamed.csv' belongs to",
                response.getBody().files().get(0).error());
    }

    @Test
    public void apply_returnsTheServicesPlanWhenTheBatchWasTaken() {
        ImportPlan plan = new ImportPlan("run-1", List.of(), 0);
        when(importService.apply(anyList(), any(), eq("7"))).thenReturn(plan);

        ResponseEntity<ImportPlan> response = controller.apply(List.of(csv("tests-a.csv")), null, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(plan, response.getBody());
    }

    @Test
    public void apply_rejectsAnEmptyBatch() {
        ResponseEntity<ImportPlan> response = controller.apply(List.of(), null, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    private static MultipartFile csv(String fileName) {
        return new MockMultipartFile("files", fileName, "text/csv", "name\nrow\n".getBytes(StandardCharsets.UTF_8));
    }
}
