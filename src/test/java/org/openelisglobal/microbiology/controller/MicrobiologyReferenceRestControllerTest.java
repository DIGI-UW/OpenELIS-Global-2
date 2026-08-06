package org.openelisglobal.microbiology.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.microbiology.controller.rest.MicrobiologyReferenceRestController;
import org.openelisglobal.microbiology.form.MicroReferenceOptionForm;
import org.openelisglobal.microbiology.service.MicroBreakpointService;
import org.openelisglobal.microbiology.service.MicrobiologyReferenceService;
import org.openelisglobal.microbiology.valueholder.MicroCultureSetup;
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
}
