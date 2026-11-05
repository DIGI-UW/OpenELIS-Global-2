package org.openelisglobal.resultrun.controller.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.resultrun.form.ResultRunHeader;
import org.openelisglobal.resultrun.form.ResultRunListResponse;
import org.openelisglobal.resultrun.form.ResultRunRowsPage;
import org.openelisglobal.resultrun.form.RunViewer;
import org.openelisglobal.resultrun.service.ResultRunService;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.systemuser.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * OGC-1200: the runs a user may work, gated on Results rights per lab unit
 * (D-043, D-044), never on Analyser Import.
 */
@RestController
@RequestMapping("/rest/result-runs")
@PreAuthorize("hasRole('RESULTS')")
public class ResultRunRestController extends BaseRestController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ResultRunService resultRunService;
    private final UserService userService;
    private final RoleService roleService;

    public ResultRunRestController(ResultRunService resultRunService, UserService userService,
            RoleService roleService) {
        this.resultRunService = resultRunService;
        this.userService = userService;
        this.roleService = roleService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResultRunListResponse list(HttpServletRequest request,
            @RequestParam(required = false) String testSectionId) {
        return resultRunService.listActiveRuns(viewer(request), testSectionId);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultRunHeader> header(HttpServletRequest request, @PathVariable Long id) {
        return ResponseEntity.of(resultRunService.getHeader(id, viewer(request)));
    }

    @GetMapping(value = "/{id}/rows", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultRunRowsPage> rows(HttpServletRequest request, @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "50") int pageSize) {
        return ResponseEntity.of(resultRunService.getRows(id, viewer(request), page, pageSize));
    }

    /**
     * The accepted message as a JSON document. Returned as a tree, not a String:
     * the JSON converter would otherwise encode the text a second time.
     */
    @GetMapping(value = "/{id}/raw", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> raw(HttpServletRequest request, @PathVariable Long id)
            throws JsonProcessingException {
        Optional<String> json = resultRunService.getRawMessage(id, viewer(request));
        if (json.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(OBJECT_MAPPER.readTree(json.get()));
    }

    private RunViewer viewer(HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        String roleId = roleService.getRoleByName(Constants.ROLE_RESULTS).getId();
        List<IdValuePair> sections = userService.getUserViewerTestSections(sysUserId, roleId);
        Set<String> labUnitIds = new HashSet<>();
        if (sections != null) {
            sections.forEach(section -> labUnitIds.add(section.getId()));
        }
        return new RunViewer(userService.getTestIdsInUserLabUnits(sysUserId, Constants.ROLE_RESULTS), labUnitIds);
    }
}
