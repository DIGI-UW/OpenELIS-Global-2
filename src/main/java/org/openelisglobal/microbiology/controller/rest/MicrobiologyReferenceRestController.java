package org.openelisglobal.microbiology.controller.rest;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.microbiology.form.MicroReferenceOptionForm;
import org.openelisglobal.microbiology.service.MicrobiologyReferenceService;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstPanel;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/microbiology/reference")
public class MicrobiologyReferenceRestController extends BaseRestController {

    private final MicrobiologyReferenceService referenceService;

    public MicrobiologyReferenceRestController(MicrobiologyReferenceService referenceService) {
        this.referenceService = referenceService;
    }

    @GetMapping("/ast-panels")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MicroReferenceOptionForm>> getAstPanels(@RequestParam String workflowType) {
        List<MicroReferenceOptionForm> forms = new ArrayList<>();
        for (MicroAstPanel panel : referenceService.getActiveAstPanels(MicroWorkflowType.valueOf(workflowType))) {
            forms.add(toPanelForm(panel));
        }
        return ResponseEntity.ok(forms);
    }

    @GetMapping("/antibiotics")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MicroReferenceOptionForm>> getAntibiotics() {
        List<MicroReferenceOptionForm> forms = new ArrayList<>();
        for (MicroAntibiotic antibiotic : referenceService.getActiveAntibiotics()) {
            forms.add(toAntibioticForm(antibiotic));
        }
        return ResponseEntity.ok(forms);
    }

    private MicroReferenceOptionForm toPanelForm(MicroAstPanel panel) {
        MicroReferenceOptionForm form = new MicroReferenceOptionForm();
        form.id = panel.getId();
        form.label = panel.getName();
        form.code = panel.getOrganismGroup();
        return form;
    }

    private MicroReferenceOptionForm toAntibioticForm(MicroAntibiotic antibiotic) {
        MicroReferenceOptionForm form = new MicroReferenceOptionForm();
        form.id = antibiotic.getId();
        form.label = antibiotic.getDisplayName();
        form.code = antibiotic.getWhonetCode();
        return form;
    }
}
