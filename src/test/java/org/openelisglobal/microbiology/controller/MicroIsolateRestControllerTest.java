package org.openelisglobal.microbiology.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.microbiology.controller.rest.MicroIsolateRestController;
import org.openelisglobal.microbiology.form.MicroIsolateForm;
import org.openelisglobal.microbiology.form.MicroIsolateRequestForm;
import org.openelisglobal.microbiology.service.MicroIsolateService;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationStatus;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

public class MicroIsolateRestControllerTest {

    @Test
    public void createUsesAuthenticatedActorAndMapsGramStainWorkup() throws Exception {
        MicroIsolateService service = mock(MicroIsolateService.class);
        MicroIsolateRequestForm request = new ObjectMapper()
                .readValue("{\"caseId\":\"case-1\",\"isolateLabel\":\"ISO-1\",\"gramStain\":\"Gram negative rod\","
                        + "\"colonyMorphology\":\"Lactose fermenting\",\"significance\":\"CLINICALLY_SIGNIFICANT\","
                        + "\"performedBy\":\"999\"}", MicroIsolateRequestForm.class);
        MicroIsolate isolate = isolate("iso-1");
        isolate.setGramStain("Gram negative rod");
        isolate.setColonyMorphology("Lactose fermenting");
        when(service.createIsolate("case-1", "ISO-1", "Gram negative rod", "Lactose fermenting",
                MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, "42")).thenReturn(isolate);

        ResponseEntity<MicroIsolateForm> response = new MicroIsolateRestController(service).createIsolate(request,
                requestFor("42"));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Gram negative rod", response.getBody().gramStain);
        verify(service).createIsolate("case-1", "ISO-1", "Gram negative rod", "Lactose fermenting",
                MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, "42");
    }

    @Test
    public void updateMapsConfirmedIdentificationWorkup() {
        MicroIsolateService service = mock(MicroIsolateService.class);
        MicroIsolateRequestForm request = new MicroIsolateRequestForm();
        request.organismId = "organism-1";
        request.preliminaryOrganismText = "E. coli";
        request.identificationMethod = "MALDI-TOF";
        request.identificationConfidence = new BigDecimal("98");
        request.significance = MicroIsolateSignificance.CLINICALLY_SIGNIFICANT.name();
        request.identificationStatus = MicroIsolateIdentificationStatus.CONFIRMED.name();
        MicroIsolate isolate = isolate("iso-1");
        isolate.setOrganismId("organism-1");
        isolate.setIdentificationMethod("MALDI-TOF");
        isolate.setIdentificationConfidence(new BigDecimal("98"));
        isolate.setIdentificationStatus(MicroIsolateIdentificationStatus.CONFIRMED.name());
        when(service.updateIdentification("iso-1", "organism-1", "E. coli",
                MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, MicroIsolateIdentificationStatus.CONFIRMED,
                "MALDI-TOF", new BigDecimal("98"), "42")).thenReturn(isolate);

        ResponseEntity<MicroIsolateForm> response = new MicroIsolateRestController(service)
                .updateIdentification("iso-1", request, requestFor("42"));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("MALDI-TOF", response.getBody().identificationMethod);
        verify(service).updateIdentification("iso-1", "organism-1", "E. coli",
                MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, MicroIsolateIdentificationStatus.CONFIRMED,
                "MALDI-TOF", new BigDecimal("98"), "42");
    }

    private MicroIsolate isolate(String id) {
        MicroIsolate isolate = new MicroIsolate();
        isolate.setId(id);
        isolate.setCaseId("case-1");
        isolate.setIsolateLabel("ISO-1");
        isolate.setSignificance(MicroIsolateSignificance.CLINICALLY_SIGNIFICANT.name());
        isolate.setIdentificationStatus(MicroIsolateIdentificationStatus.PRELIMINARY.name());
        return isolate;
    }

    private MockHttpServletRequest requestFor(String userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(Integer.parseInt(userId));
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return request;
    }
}
