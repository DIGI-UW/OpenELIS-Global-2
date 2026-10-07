package org.openelisglobal.organization.controller.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.organization.locations.LocationsApi;
import org.openelisglobal.organization.locations.LocationsApi.Query;
import org.openelisglobal.organization.locations.LocationsImportApi;
import org.openelisglobal.organization.locations.LocationsImportApi.Decision;
import org.openelisglobal.organization.locations.LocationsImportApi.Options;
import org.openelisglobal.organization.locations.LocationsImportApi.Plan;
import org.openelisglobal.organization.locations.LocationsImportApi.RecentRun;
import org.openelisglobal.organization.locations.LocationsImportService;
import org.openelisglobal.organization.locations.LocationsNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * OGC-1363 (sections F and G): the Locations import page's preview and apply,
 * the recent runs and their result reports, the templates, and the exports.
 */
@RestController
@RequestMapping("/rest/locations")
@PreAuthorize("hasRole('ADMIN')")
public class LocationsImportRestController extends BaseRestController {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<Map<String, Decision>> DECISIONS = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, String>> RENAMES = new TypeReference<>() {
    };

    @Autowired
    private LocationsImportService importService;

    @PostMapping(value = "/import/preview", produces = MediaType.APPLICATION_JSON_VALUE)
    public Plan preview(@RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "areas", required = false) List<String> areas,
            @RequestParam(value = "mode", defaultValue = LocationsImportApi.MODE_MERGE) String mode,
            @RequestParam(value = "decisions", required = false) String decisions,
            @RequestParam(value = "renames", required = false) String renames, HttpServletRequest request) {
        return importService.preview(files, areas, options(mode, decisions, renames), getSysUserId(request));
    }

    @PostMapping(value = "/import/apply", produces = MediaType.APPLICATION_JSON_VALUE)
    public Plan apply(@RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "areas", required = false) List<String> areas,
            @RequestParam(value = "mode", defaultValue = LocationsImportApi.MODE_MERGE) String mode,
            @RequestParam(value = "decisions", required = false) String decisions,
            @RequestParam(value = "renames", required = false) String renames, HttpServletRequest request) {
        return importService.apply(files, areas, options(mode, decisions, renames), getSysUserId(request));
    }

    private static Options options(String mode, String decisions, String renames) {
        try {
            Map<String, Decision> chosen = decisions == null || decisions.isBlank() ? Map.of()
                    : JSON.readValue(decisions, DECISIONS);
            Map<String, String> renamed = renames == null || renames.isBlank() ? Map.of()
                    : JSON.readValue(renames, RENAMES);
            return new Options(mode, chosen, renamed);
        } catch (Exception e) {
            throw new IllegalArgumentException("The decisions could not be read: " + e.getMessage(), e);
        }
    }

    @GetMapping(value = "/import/recent", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<RecentRun> recent() {
        return importService.recentRuns();
    }

    @GetMapping(value = "/import/runs/{id}/report")
    public ResponseEntity<byte[]> report(@PathVariable String id) {
        return csv(importService.report(id), "import-run-" + id + ".csv");
    }

    @GetMapping(value = "/import/template")
    public ResponseEntity<byte[]> template(
            @RequestParam(defaultValue = LocationsImportApi.AREA_ORGANIZATIONS) String area) {
        String name = LocationsImportApi.AREA_LEVELS.equals(area) ? "address-levels.csv"
                : LocationsImportApi.AREA_VALUES.equals(area) ? "address-values.csv" : "organizations-template.csv";
        return csv(importService.template(area), name);
    }

    @GetMapping(value = "/export")
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue = LocationsApi.VIEW_ORGANIZATIONS) String view,
            @RequestParam(required = false) String q, @RequestParam(required = false) String type,
            @RequestParam(required = false) String location, @RequestParam(required = false) String category,
            @RequestParam(required = false) String ownership,
            @RequestParam(defaultValue = LocationsApi.STATUS_ACTIVE) String status,
            @RequestParam(defaultValue = "false") boolean reviewOverdue, @RequestParam(required = false) String ids) {
        if ("areas".equals(view)) {
            return csv(importService.exportAreas(), "address-values.csv");
        }
        Query query = new Query(view, q, split(type), location, split(category), split(ownership), status,
                reviewOverdue, "name", 1, 100000);
        String name = LocationsApi.VIEW_SITES.equals(view) ? "organizations-sites.csv" : "organizations.csv";
        return csv(importService.export(query, split(ids)), name);
    }

    private static List<String> split(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return List.of(text.split(",")).stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static ResponseEntity<byte[]> csv(String content, String fileName) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(content.getBytes(StandardCharsets.UTF_8));
    }

    @ExceptionHandler(LocationsNotFoundException.class)
    public ResponseEntity<Map<String, Object>> missing(LocationsNotFoundException e) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
    public ResponseEntity<Map<String, Object>> refused(RuntimeException e) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    /**
     * An unexpected failure is logged for the administrator; the user is not shown
     * the exception's text (a parser or database message), only that the request
     * could not be completed, in their language.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> failed(RuntimeException e) {
        LogEvent.logError(e);
        Map<String, Object> body = new HashMap<>();
        body.put("error", "");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
