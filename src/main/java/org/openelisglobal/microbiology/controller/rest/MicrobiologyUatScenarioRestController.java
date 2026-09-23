package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.config.condition.ConditionalOnProperty;
import org.openelisglobal.microbiology.form.MicrobiologyUatScenarioForm;
import org.openelisglobal.microbiology.form.MicrobiologyUatScenarioRequestForm;
import org.openelisglobal.microbiology.service.MicrobiologyUatScenarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/microbiology/uat/scenarios")
@ConditionalOnProperty(property = "org.openelisglobal.uat.scenarios.enabled", havingValue = "true")
public class MicrobiologyUatScenarioRestController extends BaseRestController {

    private final MicrobiologyUatScenarioService scenarioService;

    public MicrobiologyUatScenarioRestController(MicrobiologyUatScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MicrobiologyUatScenarioForm> provision(HttpServletRequest request,
            @RequestBody MicrobiologyUatScenarioRequestForm scenarioRequest) {
        return ResponseEntity.ok(scenarioService.provision(scenarioRequest, getSysUserId(request)));
    }
}
