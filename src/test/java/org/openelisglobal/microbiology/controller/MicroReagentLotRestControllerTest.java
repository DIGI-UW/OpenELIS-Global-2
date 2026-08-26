package org.openelisglobal.microbiology.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.microbiology.controller.rest.MicroReagentLotRestController;
import org.openelisglobal.microbiology.form.MicroReagentLotOverviewForm;
import org.openelisglobal.microbiology.form.MicroReagentRequirementForm;
import org.openelisglobal.microbiology.form.MicroReagentUsageForm;
import org.openelisglobal.microbiology.service.MicroReagentLotService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

public class MicroReagentLotRestControllerTest {

    @Test
    public void overviewReturnsCurrentRequirementsAndHistoricalUsage() {
        MicroReagentLotService service = org.mockito.Mockito.mock(MicroReagentLotService.class);
        MicroReagentRequirementForm requirement = new MicroReagentRequirementForm();
        requirement.linkId = "link-1";
        MicroReagentUsageForm usage = new MicroReagentUsageForm();
        usage.id = "usage-1";
        when(service.getRequirements("case-1")).thenReturn(List.of(requirement));
        when(service.getUsageHistory("case-1")).thenReturn(List.of(usage));

        ResponseEntity<MicroReagentLotOverviewForm> response = new MicroReagentLotRestController(service)
                .getOverview("case-1");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("link-1", response.getBody().requirements.get(0).linkId);
        assertEquals("usage-1", response.getBody().usages.get(0).id);
    }

    @Test
    public void overviewRequiresBenchRoleBundle() {
        PreAuthorize authorization = MicroReagentLotRestController.class.getAnnotation(PreAuthorize.class);

        assertEquals("hasAnyRole('ADMIN', 'RESULTS', 'VALIDATION')", authorization.value());
    }
}
