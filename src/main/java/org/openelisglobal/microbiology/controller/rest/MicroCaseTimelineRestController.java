package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.openelisglobal.microbiology.form.MicroCaseActivityForm;
import org.openelisglobal.microbiology.form.MicroCaseNoteRequestForm;
import org.openelisglobal.microbiology.service.MicroCaseTimelineService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/microbiology/cases/{caseId}")
public class MicroCaseTimelineRestController extends MicrobiologyRestControllerSupport {

    private final MicroCaseTimelineService timelineService;

    public MicroCaseTimelineRestController(MicroCaseTimelineService timelineService) {
        this.timelineService = timelineService;
    }

    @GetMapping("/timeline")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MicroCaseActivityForm>> getTimeline(@PathVariable String caseId) {
        return ResponseEntity.ok(timelineService.getTimeline(caseId));
    }

    @PostMapping("/notes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroCaseActivityForm> addNote(@PathVariable String caseId,
            @RequestBody MicroCaseNoteRequestForm request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(timelineService.addNote(caseId, request.text, authenticatedUserId(httpRequest)));
    }
}
