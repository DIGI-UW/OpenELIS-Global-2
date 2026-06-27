package org.openelisglobal.microbiology.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.microbiology.controller.rest.MicroCaseRestController;
import org.openelisglobal.microbiology.form.MicroCaseLookupForm;
import org.openelisglobal.microbiology.service.MicroCaseService;
import org.openelisglobal.microbiology.service.MicroCaseStateService;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.springframework.http.ResponseEntity;

public class MicroCaseLookupRestControllerTest {

    @Test
    public void getCasesForSampleItemReturnsSiblingCaseLookupRows() {
        MicroCaseService service = org.mockito.Mockito.mock(MicroCaseService.class);
        MicroCase bacteriology = caseRow("case-1", MicroWorkflowType.BACTERIOLOGY);
        MicroCase tb = caseRow("case-2", MicroWorkflowType.MYCOBACTERIOLOGY_TB);
        when(service.getSiblingCases("1001")).thenReturn(List.of(bacteriology, tb));

        ResponseEntity<List<MicroCaseLookupForm>> response = new MicroCaseRestController(service,
                org.mockito.Mockito.mock(MicroCaseStateService.class)).getCasesForSampleItem("1001");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(2, response.getBody().size());
        assertEquals("case-1", response.getBody().get(0).id);
        assertEquals(MicroWorkflowType.BACTERIOLOGY.name(), response.getBody().get(0).workflowType);
    }

    private MicroCase caseRow(String caseId, MicroWorkflowType workflowType) {
        MicroCase microCase = new MicroCase();
        microCase.setId(caseId);
        microCase.setSampleItemId("1001");
        microCase.setWorkflowType(workflowType.name());
        return microCase;
    }
}
