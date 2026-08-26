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
import org.openelisglobal.microbiology.valueholder.MicroCultureSetup;
import org.openelisglobal.microbiology.valueholder.MicroPatientOrigin;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.springframework.http.ResponseEntity;

public class MicrobiologyReferenceRestControllerTest {

    @Test
    public void cultureMethodsExposeActiveCompatibleMethodIdentity() {
        MicrobiologyReferenceService referenceService = org.mockito.Mockito.mock(MicrobiologyReferenceService.class);
        MicroCultureSetup setup = new MicroCultureSetup();
        setup.setMethodId("method-1");
        setup.setName("Routine blood culture");
        setup.setWorkflowType(MicroWorkflowType.BACTERIOLOGY.name());
        when(referenceService.getActiveCultureSetups(MicroWorkflowType.BACTERIOLOGY)).thenReturn(List.of(setup));

        ResponseEntity<List<MicroReferenceOptionForm>> response = new MicrobiologyReferenceRestController(
                referenceService, org.mockito.Mockito.mock(MicroBreakpointService.class))
                .getCultureMethods(MicroWorkflowType.BACTERIOLOGY.name());

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals("method-1", response.getBody().get(0).id);
        assertEquals("Routine blood culture", response.getBody().get(0).label);
        assertEquals(MicroWorkflowType.BACTERIOLOGY.name(), response.getBody().get(0).code);
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
