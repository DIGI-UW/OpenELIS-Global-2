package org.openelisglobal.microbiology.controller.rest;

import org.openelisglobal.microbiology.form.MicroReagentLotOverviewForm;
import org.openelisglobal.microbiology.service.MicroReagentLotService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/microbiology/cases/{caseId}/reagent-lots")
@PreAuthorize(MicrobiologyRestControllerSupport.BENCH_ACCESS)
public class MicroReagentLotRestController extends MicrobiologyRestControllerSupport {

    private final MicroReagentLotService reagentLotService;

    public MicroReagentLotRestController(MicroReagentLotService reagentLotService) {
        this.reagentLotService = reagentLotService;
    }

    @GetMapping
    public ResponseEntity<MicroReagentLotOverviewForm> getOverview(@PathVariable String caseId) {
        MicroReagentLotOverviewForm form = new MicroReagentLotOverviewForm();
        form.requirements.addAll(reagentLotService.getRequirements(caseId));
        form.usages.addAll(reagentLotService.getUsageHistory(caseId));
        return ResponseEntity.ok(form);
    }
}
