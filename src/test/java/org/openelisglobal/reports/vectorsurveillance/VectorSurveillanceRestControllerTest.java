package org.openelisglobal.reports.vectorsurveillance;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.reports.vectorsurveillance.controller.rest.VectorSurveillanceRestController;
import org.openelisglobal.reports.vectorsurveillance.service.VectorSurveillanceService;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceIndicesDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit test for the dashboard endpoint's "blank dates = all data" handling.
 *
 * <p>
 * The locale-date parse and the 400-on-invalid path both go through
 * {@code DateUtil}, whose static initializer needs the runtime config and
 * cannot load in a plain unit test. Those are verified by the assembled
 * dashboard run (a narrow real date range actually scopes the results; an
 * unparseable date returns 400) — the right level for a parse boundary that
 * depends on the configured locale, and the exact behaviour no prior test
 * exercised (the demo always used empty dates, which is why the bug shipped).
 */
public class VectorSurveillanceRestControllerTest {

    private VectorSurveillanceService service;
    private VectorSurveillanceRestController controller;

    @Before
    public void setUp() {
        service = mock(VectorSurveillanceService.class);
        controller = new VectorSurveillanceRestController();
        ReflectionTestUtils.setField(controller, "vectorSurveillanceService", service);
    }

    // Blank/absent dates mean "all data" — the service is called with null bounds.
    @Test
    public void getIndices_blankDates_scopeToAllData() {
        when(service.getIndices(null, null, null)).thenReturn(new SurveillanceIndicesDTO());

        ResponseEntity<SurveillanceIndicesDTO> resp = controller.getIndices(null, null, null);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(service).getIndices(null, null, null);
    }
}
