package org.openelisglobal.qaevent.qiconfig.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.qaevent.qiconfig.dto.QiConfigView;
import org.openelisglobal.qaevent.qiconfig.dto.ResolvedConfig;
import org.openelisglobal.qaevent.qiconfig.service.QiConfigService;
import org.openelisglobal.test.service.TestSectionService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * OGC-709 — QI Configuration API.
 *
 * <p>
 * Management (list + save) is gated on {@code qa.manage.qi}; the resolve read
 * contract is gated on {@code qa.view.qi} (the QI dashboard's own pillar
 * permission, which Reception/Results/Validation hold — {@code qa.view.qms}
 * would 403 the very consumers we are freezing this contract for).
 *
 * <p>
 * All bad input is 400: the service throws {@link IllegalArgumentException} for
 * unknown indicators / range / direction / duplicate sections, and a
 * {@link DataIntegrityViolationException} (e.g. an FK-invalid section forced
 * past the UI) is mapped to 400 by the backstop handler rather than surfacing
 * as a 500.
 */
@RestController
@RequestMapping("/rest/qi-config")
public class QiConfigRestController extends BaseRestController {

    private final QiConfigService qiConfigService;

    private final TestSectionService testSectionService;

    public QiConfigRestController(QiConfigService qiConfigService, TestSectionService testSectionService) {
        this.qiConfigService = qiConfigService;
        this.testSectionService = testSectionService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('qa.manage.qi') or hasRole('GLOBAL_ADMIN')")
    public List<QiConfigView> list() {
        return qiConfigService.getAllConfigs();
    }

    /**
     * Active test sections (id + localized name) for the per-category override
     * picker.
     */
    @GetMapping(value = "/test-sections", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('qa.manage.qi') or hasRole('GLOBAL_ADMIN')")
    public List<IdValuePair> testSections() {
        return testSectionService.getAllActiveTestSections().stream()
                .map(ts -> new IdValuePair(ts.getId(), ts.getLocalizedName())).collect(Collectors.toList());
    }

    @PutMapping(value = "/indicator/{indicatorKey}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('qa.manage.qi') or hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<Void> saveIndicator(@PathVariable String indicatorKey, @RequestBody QiConfigView body,
            HttpServletRequest request) {
        qiConfigService.saveIndicator(indicatorKey, body, ControllerUtills.getSysUserId(request));
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/resolve", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('qa.view.qi') or hasRole('GLOBAL_ADMIN')")
    public ResolvedConfig resolve(@RequestParam("indicator") String indicator,
            @RequestParam(value = "testSectionId", required = false) String testSectionId) {
        return qiConfigService.resolve(indicator, testSectionId);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadInput(IllegalArgumentException e) {
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleConstraint(DataIntegrityViolationException e) {
        LogEvent.logError(e);
        return Map.of("error", "Invalid or duplicate QI configuration");
    }
}
