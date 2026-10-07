package org.openelisglobal.microbiology.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.microbiology.controller.rest.MicrobiologyReferenceRestController;
import org.openelisglobal.microbiology.form.MicroPatientOriginOptionsForm;
import org.openelisglobal.microbiology.form.MicroReferenceOptionForm;
import org.openelisglobal.microbiology.service.MicroBreakpointService;
import org.openelisglobal.microbiology.service.MicroPatientOriginOptions;
import org.openelisglobal.microbiology.service.MicrobiologyReferenceService;
import org.openelisglobal.microbiology.valueholder.MicroAstPanel;
import org.openelisglobal.microbiology.valueholder.MicroPatientOrigin;
import org.springframework.http.ResponseEntity;

public class MicrobiologyReferenceRestControllerTest {

    @Test
    public void panelsExposeActiveOrganismGroupAndPublishedIdentity() {
        MicrobiologyReferenceService referenceService = org.mockito.Mockito.mock(MicrobiologyReferenceService.class);
        MicroAstPanel panel = new MicroAstPanel();
        panel.setId("panel-1");
        panel.setName("Enterobacterales panel");
        panel.setOrganismGroup("Enterobacterales");
        when(referenceService.getActiveAstPanels("Enterobacterales")).thenReturn(List.of(panel));

        ResponseEntity<List<MicroReferenceOptionForm>> response = new MicrobiologyReferenceRestController(
                referenceService, org.mockito.Mockito.mock(MicroBreakpointService.class))
                .getAstPanels("Enterobacterales");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals("panel-1", response.getBody().get(0).id);
        assertEquals("Enterobacterales panel", response.getBody().get(0).label);
        assertEquals("Enterobacterales", response.getBody().get(0).code);
    }

    @Test
    public void patientOriginsExposeStableCodesAndConfiguredDefault() {
        MicrobiologyReferenceService referenceService = org.mockito.Mockito.mock(MicrobiologyReferenceService.class);
        MicroPatientOrigin inpatient = new MicroPatientOrigin();
        inpatient.setId("origin-1");
        inpatient.setCode("INPATIENT");
        inpatient.setDisplayName("Inpatient");
        inpatient.setWhonetCode("INP");
        when(referenceService.getPatientOrigins("27"))
                .thenReturn(new MicroPatientOriginOptions(List.of(inpatient), "INPATIENT"));

        ResponseEntity<MicroPatientOriginOptionsForm> response = new MicrobiologyReferenceRestController(
                referenceService, org.mockito.Mockito.mock(MicroBreakpointService.class)).getPatientOrigins("27");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("INPATIENT", response.getBody().defaultCode);
        assertEquals("INPATIENT", response.getBody().options.get(0).code);
        assertEquals("Inpatient", response.getBody().options.get(0).label);
        assertEquals("INP", response.getBody().options.get(0).whonetCode);
    }
}
