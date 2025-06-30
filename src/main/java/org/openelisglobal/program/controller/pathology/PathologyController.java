package org.openelisglobal.program.controller.pathology;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.program.bean.PathologyDashBoardCount;
import org.openelisglobal.program.service.PathologyDisplayService;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.valueholder.pathology.PathologyCaseViewDisplayItem;
import org.openelisglobal.program.valueholder.pathology.PathologyDisplayItem;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PathologyController extends BaseRestController {

    @Autowired
    private PathologySampleService pathologySampleService;
    @Autowired
    private PathologyDisplayService pathologyDisplayService;
    @Autowired
    private SystemUserService systemUserService;

    @GetMapping(value = "/rest/pathology/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<PathologyDisplayItem> getFilteredPathologyEntries(@RequestParam(required = false) String searchTerm,
            @RequestParam PathologyStatus... statuses) {
        return pathologySampleService.searchWithStatusAndTerm(Arrays.asList(statuses), searchTerm).stream()
                .map(e -> pathologyDisplayService.convertToDisplayItem(e.getId())).collect(Collectors.toList());
    }

    @GetMapping(value = "/rest/pathology/dashboard/count", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<PathologyDashBoardCount> getFilteredPathologyEntries() {
        PathologyDashBoardCount count = new PathologyDashBoardCount();
        count.setInProgress(pathologySampleService.getCountWithStatus(
                Arrays.asList(PathologyStatus.GROSSING, PathologyStatus.CUTTING, PathologyStatus.GROSSING,
                        PathologyStatus.SLICING, PathologyStatus.STAINING, PathologyStatus.PROCESSING)));
        count.setAwaitingReview(
                pathologySampleService.getCountWithStatus(Arrays.asList(PathologyStatus.READY_PATHOLOGIST)));
        count.setAdditionalRequests(
                pathologySampleService.getCountWithStatus(Arrays.asList(PathologyStatus.ADDITIONAL_REQUEST)));

        Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
        Instant weekAgoInstant = Instant.now().minus(7, ChronoUnit.DAYS);
        Timestamp weekAgoTimestamp = Timestamp.from(weekAgoInstant);

        count.setComplete(pathologySampleService.getCountWithStatusBetweenDates(
                Arrays.asList(PathologyStatus.COMPLETED), weekAgoTimestamp, currentTimestamp));
        return ResponseEntity.ok(count);
    }

    @PostMapping(value = "/rest/pathology/assignTechnician", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> assignTechnician(@RequestParam Integer pathologySampleId,
            HttpServletRequest request) {
        String currentUserId = getSysUserId(request);
        pathologySampleService.assignTechnician(pathologySampleId, systemUserService.get(currentUserId), currentUserId);
        return ResponseEntity.ok("ok");
    }

    @PostMapping(value = "/rest/pathology/assignPathologist", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> assignPathologist(@RequestParam Integer pathologySampleId,
            HttpServletRequest request) {
        String currentUserId = getSysUserId(request);
        pathologySampleService.assignPathologist(pathologySampleId, systemUserService.get(currentUserId),
                currentUserId);
        return ResponseEntity.ok("ok");
    }

    @GetMapping(value = "/rest/pathology/caseView/{pathologySampleId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public PathologyCaseViewDisplayItem getFilteredPathologyEntries(
            @PathVariable("pathologySampleId") Integer pathologySampleId) {
        return pathologyDisplayService.convertToCaseDisplayItem(pathologySampleId);
    }

    @PostMapping(value = "/rest/pathology/caseView/{pathologySampleId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public PathologySampleForm getFilteredPathologyEntries(@PathVariable("pathologySampleId") Integer pathologySampleId,
            @RequestBody PathologySampleForm form, HttpServletRequest request) {
        form.setSystemUserId(this.getSysUserId(request));
        pathologySampleService.updateWithFormValues(pathologySampleId, form);

        return form;
    }
}
