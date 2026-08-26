package org.openelisglobal.eqa.controller.rest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.eqa.service.EQAAnalystCompetencyService;
import org.openelisglobal.eqa.service.EQAProgramService;
import org.openelisglobal.eqa.valueholder.EQASchemeAnalyst;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Analyst Competency (OGC-611, FR-V2.3-06): the per-analyst ISO 15189 §6.2.3
 * evidence, banded Competent / Under review / Not competent.
 *
 * <p>
 * Read-only by construction — competency events are service-written
 * (AC-V2.1-21), so there is deliberately no create handler here.
 */
@RestController
@RequestMapping("/rest/eqa")
@PreAuthorize(EQAGuards.READ)
public class EQAAnalystCompetencyRestController extends BaseRestController {

    private final EQAAnalystCompetencyService competencyService;
    private final EQAProgramService programService;
    private final SystemUserService systemUserService;

    public EQAAnalystCompetencyRestController(EQAAnalystCompetencyService competencyService,
            EQAProgramService programService, SystemUserService systemUserService) {
        this.competencyService = competencyService;
        this.programService = programService;
        this.systemUserService = systemUserService;
    }

    @GetMapping(value = "/analyst-competency", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> analystCompetency() {
        return competencyService.getCompetencyRollup();
    }

    /**
     * Who result entry may record as the analyst on this scheme's samples
     * (FR-V2.3-04). The scheme's opt-in list when it has one, every active user
     * when it does not — FR-V2.1-08 makes an empty list permissive, and answering
     * that here keeps the rule out of the grid.
     */
    @GetMapping(value = "/schemes/{schemeId}/eligible-analysts", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Map<String, Object>> eligibleAnalysts(@PathVariable Long schemeId) {
        List<Map<String, Object>> eligible = new ArrayList<>();
        for (EQASchemeAnalyst analyst : programService.getAnalysts(schemeId)) {
            SystemUser user = systemUserService.get(String.valueOf(analyst.getSystemUserId()));
            eligible.add(toAnalystDto(String.valueOf(analyst.getSystemUserId()), user));
        }
        if (!eligible.isEmpty()) {
            return eligible;
        }
        for (SystemUser user : systemUserService.getAllSystemUsers()) {
            if ("Y".equals(user.getIsActive())) {
                eligible.add(toAnalystDto(user.getId(), user));
            }
        }
        return eligible;
    }

    private Map<String, Object> toAnalystDto(String id, SystemUser user) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", id);
        String name = user == null ? null
                : ((user.getFirstName() == null ? "" : user.getFirstName() + " ")
                        + (user.getLastName() == null ? "" : user.getLastName())).trim();
        dto.put("displayName", name == null || name.isEmpty() ? id : name);
        return dto;
    }
}
