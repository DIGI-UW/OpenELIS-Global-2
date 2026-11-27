package org.openelisglobal.program.controller.pathology;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.openelisglobal.program.controller.pathology.PathologySampleForm.PathologyReportForm;
import org.openelisglobal.program.service.PathologyReportConflictException;
import org.openelisglobal.program.service.PathologyReportService;
import org.openelisglobal.program.service.PathologyReportService.ReportFile;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.service.ProgramSampleLabUnitScope;
import org.openelisglobal.program.service.ReportPaths;
import org.openelisglobal.program.valueholder.pathology.ReportView;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The report versions of a pathology case, each written by its own request
 * rather than by the case save: generate from the stored case, upload a file,
 * withdraw an upload, and read a stored file at its own address. A case outside
 * the user's lab units is not found, before anything is read or written.
 */
@RestController
@RequestMapping("/rest/pathology/caseView/{pathologySampleId}/reports")
public class PathologyReportRestController extends PathologyRestController {

    private final PathologyReportService pathologyReportService;
    private final PathologySampleService pathologySampleService;
    private final ProgramSampleLabUnitScope programSampleLabUnitScope;
    private final ServletContext servletContext;

    public PathologyReportRestController(PathologyReportService pathologyReportService,
            PathologySampleService pathologySampleService, ProgramSampleLabUnitScope programSampleLabUnitScope,
            ServletContext servletContext) {
        this.pathologyReportService = pathologyReportService;
        this.pathologySampleService = pathologySampleService;
        this.programSampleLabUnitScope = programSampleLabUnitScope;
        this.servletContext = servletContext;
    }

    @PostMapping(value = "/generate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReportView> generate(@PathVariable("pathologySampleId") Integer pathologySampleId,
            HttpServletRequest request) {
        String sysUserId = requireVisible(pathologySampleId, request);
        return ResponseEntity
                .ok(pathologyReportService.generate(pathologySampleId, sysUserId, ReportPaths.of(servletContext)));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReportView> upload(@PathVariable("pathologySampleId") Integer pathologySampleId,
            @RequestBody(required = false) PathologyReportForm upload, HttpServletRequest request) {
        String sysUserId = requireVisible(pathologySampleId, request);
        return ResponseEntity.ok(pathologyReportService.upload(pathologySampleId, upload, sysUserId));
    }

    @GetMapping("/{reportId}/file")
    public ResponseEntity<byte[]> file(@PathVariable("pathologySampleId") Integer pathologySampleId,
            @PathVariable("reportId") Integer reportId, HttpServletRequest request) {
        requireVisible(pathologySampleId, request);
        ReportFile file = pathologyReportService.fileOf(pathologySampleId, reportId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.mediaType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(file.fileName()).build().toString())
                .header("X-Content-Type-Options", "nosniff").cacheControl(CacheControl.noStore().cachePrivate())
                .body(file.bytes());
    }

    @PostMapping(value = "/{reportId}/withdraw", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReportView> withdraw(@PathVariable("pathologySampleId") Integer pathologySampleId,
            @PathVariable("reportId") Integer reportId, HttpServletRequest request) {
        String sysUserId = requireVisible(pathologySampleId, request);
        return ResponseEntity.ok(pathologyReportService.withdraw(pathologySampleId, reportId, sysUserId));
    }

    /**
     * Two report writes that met on one row: an upload already withdrawn, or a
     * version number taken at the same moment. The screen reloads the case.
     */
    @ExceptionHandler(PathologyReportConflictException.class)
    public ResponseEntity<Map<String, Object>> reportConflict(PathologyReportConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("conflict", "report", "error", "the report changed at the same moment; reload the case"));
    }

    private String requireVisible(Integer pathologySampleId, HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        programSampleLabUnitScope.requireVisible(sysUserId, pathologySampleService.get(pathologySampleId));
        return sysUserId;
    }
}
