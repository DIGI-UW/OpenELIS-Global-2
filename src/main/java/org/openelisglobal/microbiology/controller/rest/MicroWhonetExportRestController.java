package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.microbiology.form.MicroWhonetExportQueryForm;
import org.openelisglobal.microbiology.form.MicroWhonetPreviewForm;
import org.openelisglobal.reports.service.MicroWhonetExportResult;
import org.openelisglobal.reports.service.WHONetReportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/microbiology/whonet")
public class MicroWhonetExportRestController extends MicrobiologyRestControllerSupport {

    private final WHONetReportService reportService;

    public MicroWhonetExportRestController(WHONetReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/preview")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESULTS', 'REPORTS')")
    public ResponseEntity<MicroWhonetPreviewForm> preview(@ModelAttribute MicroWhonetExportQueryForm query) {
        return ResponseEntity.ok(reportService.previewMicrobiologyExport(query));
    }

    @PostMapping("/exports")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESULTS', 'REPORTS')")
    public ResponseEntity<byte[]> generate(@RequestBody MicroWhonetExportQueryForm query, HttpServletRequest request) {
        MicroWhonetExportResult result = reportService.generateMicrobiologyExport(query, authenticatedUserId(request));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv;charset=UTF-8"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(result.getFileName()).build());
        return ResponseEntity.ok().headers(headers).body(result.getContent());
    }
}
