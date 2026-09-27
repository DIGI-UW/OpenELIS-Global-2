package org.openelisglobal.analyzer.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.service.AnalyzerUpgradePreparationService.ProfileSelection;
import org.openelisglobal.analyzer.service.AnalyzerUpgradeService;
import org.openelisglobal.common.rest.BaseRestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/analyzer/upgrade")
@PreAuthorize("hasAnyRole('ANALYSER_IMPORT', 'ADMIN')")
public class AnalyzerUpgradeRestController extends BaseRestController {
    private final AnalyzerUpgradeService migration;

    public AnalyzerUpgradeRestController(AnalyzerUpgradeService migration) {
        this.migration = migration;
    }

    @GetMapping
    public List<AnalyzerUpgradeService.Outcome> pending() {
        return migration.pending();
    }

    @PostMapping
    public List<AnalyzerUpgradeService.Outcome> migrate(
            @RequestBody(required = false) Map<String, ProfileSelection> selections, HttpServletRequest request) {
        return migration.migrate(selections == null ? Map.of() : selections, getSysUserId(request));
    }
}
