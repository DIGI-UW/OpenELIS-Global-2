package org.openelisglobal.containertype.controller.rest;

import java.util.List;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.containertype.form.ContainerProposalForm;
import org.openelisglobal.containertype.form.ContainerProposalView;
import org.openelisglobal.containertype.service.ContainerProposalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * OGC-1611 FR-B24 and B25: the containers Prepare Samples proposes for an
 * order's samples, from their tests' expected containers. Reads the catalog
 * only; nothing is saved.
 */
@RestController
public class ContainerProposalRestController extends BaseRestController {

    @Autowired
    private ContainerProposalService containerProposalService;

    @PostMapping(value = "/rest/container-proposal", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ContainerProposalView> propose(@RequestBody ContainerProposalForm form) {
        return containerProposalService.propose(form);
    }
}
