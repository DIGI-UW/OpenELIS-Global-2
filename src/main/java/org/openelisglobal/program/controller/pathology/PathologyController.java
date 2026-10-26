package org.openelisglobal.program.controller.pathology;

import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hibernate.StaleStateException;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.rest.util.DashboardPage;
import org.openelisglobal.common.rest.util.DashboardPaging;
import org.openelisglobal.program.bean.PathologyDashBoardCount;
import org.openelisglobal.program.service.PathologyCaseRuleException;
import org.openelisglobal.program.service.PathologyDisplayService;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.service.ProgramSampleLabUnitScope;
import org.openelisglobal.program.util.PathologyStages;
import org.openelisglobal.program.valueholder.pathology.PathologyBlock;
import org.openelisglobal.program.valueholder.pathology.PathologyCaseViewDisplayItem;
import org.openelisglobal.program.valueholder.pathology.PathologyDisplayItem;
import org.openelisglobal.program.valueholder.pathology.PathologyRequest;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.openelisglobal.program.valueholder.pathology.PathologySlide;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
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
    @Autowired
    private ProgramSampleLabUnitScope programSampleLabUnitScope;

    /**
     * Ids of the cases the last search found, in pages of paging.results.pageSize.
     */
    private final DashboardPaging<Integer> dashboardPaging = new DashboardPaging<>("pathologyDashboard");

    /**
     * One page of the dashboard. A request with {@code page} re-slices the list the
     * session already holds; any other request runs the search again and answers
     * with its first page. Only the page's rows are built.
     */
    @GetMapping(value = "/rest/pathology/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public DashboardPage<PathologyDisplayItem> getFilteredPathologyEntries(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) PathologyStatus[] statuses, @RequestParam(required = false) Integer page,
            HttpServletRequest request) {
        HttpSession session = request.getSession();
        List<Integer> pageIds;
        int pageNumber;
        if (page != null) {
            pageNumber = Math.max(page, 1);
            pageIds = dashboardPaging.page(session, pageNumber);
        } else {
            pageNumber = 1;
            List<PathologyStatus> requested = statuses == null ? List.of() : Arrays.asList(statuses);
            pageIds = dashboardPaging.cache(session,
                    programSampleLabUnitScope
                            .visible(getSysUserId(request),
                                    pathologySampleService.searchWithStatusAndTerm(requested, searchTerm))
                            .stream().map(e -> e.getId()).toList());
        }
        List<PathologyDisplayItem> items = pageIds.stream().map(pathologyDisplayService::convertToDisplayItem)
                .collect(Collectors.toList());
        return new DashboardPage<>(items, dashboardPaging.pagingBean(session, pageNumber),
                dashboardPaging.totalItems(session));
    }

    @GetMapping(value = "/rest/pathology/dashboard/count", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<PathologyDashBoardCount> getFilteredPathologyEntries(HttpServletRequest request) {
        String user = getSysUserId(request);
        if (programSampleLabUnitScope.isRestricted(user)) {
            return ResponseEntity.ok(visibleCount(user));
        }
        PathologyDashBoardCount count = new PathologyDashBoardCount();
        count.setInProgress(pathologySampleService.getCountWithStatus(PathologyStages.inProgress()));
        count.setAwaitingReview(
                pathologySampleService.getCountWithStatus(Arrays.asList(PathologyStatus.READY_PATHOLOGIST)));
        count.setAdditionalRequests(pathologySampleService.getCountWithOpenRequests());

        Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
        Instant weekAgoInstant = Instant.now().minus(7, ChronoUnit.DAYS);
        Timestamp weekAgoTimestamp = Timestamp.from(weekAgoInstant);

        count.setComplete(pathologySampleService.getCountWithStatusBetweenDates(
                Arrays.asList(PathologyStatus.COMPLETED), weekAgoTimestamp, currentTimestamp));
        return ResponseEntity.ok(count);
    }

    /** The dashboard tiles counted over the cases the user may read. */
    private PathologyDashBoardCount visibleCount(String user) {
        PathologyDashBoardCount count = new PathologyDashBoardCount();
        count.setInProgress(programSampleLabUnitScope.count(user,
                () -> pathologySampleService.getWithStatus(PathologyStages.inProgress()), c -> true));
        count.setAwaitingReview(programSampleLabUnitScope.count(user,
                () -> pathologySampleService.getWithStatus(Arrays.asList(PathologyStatus.READY_PATHOLOGIST)),
                c -> true));
        count.setAdditionalRequests(
                programSampleLabUnitScope.count(user, pathologySampleService::getAll, c -> c.getRequests() != null && c
                        .getRequests().stream().anyMatch(r -> r.getStatus() == PathologyRequest.RequestStatus.OPENED)));
        Timestamp weekAgo = Timestamp.from(Instant.now().minus(7, ChronoUnit.DAYS));
        count.setComplete(programSampleLabUnitScope.count(user,
                () -> pathologySampleService.getWithStatus(Arrays.asList(PathologyStatus.COMPLETED)),
                c -> c.getLastupdated() != null && !c.getLastupdated().before(weekAgo)));
        return count;
    }

    @PostMapping(value = "/rest/pathology/assignTechnician", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> assignTechnician(@RequestParam Integer pathologySampleId,
            HttpServletRequest request) {
        String currentUserId = getSysUserId(request);
        programSampleLabUnitScope.requireVisible(currentUserId, pathologySampleService.get(pathologySampleId));
        pathologySampleService.assignTechnician(pathologySampleId, systemUserService.get(currentUserId), currentUserId);
        return ResponseEntity.ok("ok");
    }

    @PostMapping(value = "/rest/pathology/assignPathologist", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> assignPathologist(@RequestParam Integer pathologySampleId,
            HttpServletRequest request) {
        String currentUserId = getSysUserId(request);
        programSampleLabUnitScope.requireVisible(currentUserId, pathologySampleService.get(pathologySampleId));
        pathologySampleService.assignPathologist(pathologySampleId, systemUserService.get(currentUserId),
                currentUserId);
        return ResponseEntity.ok("ok");
    }

    @GetMapping(value = "/rest/pathology/caseView/{pathologySampleId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public PathologyCaseViewDisplayItem getFilteredPathologyEntries(
            @PathVariable("pathologySampleId") Integer pathologySampleId, HttpServletRequest request) {
        programSampleLabUnitScope.requireVisible(getSysUserId(request), pathologySampleService.get(pathologySampleId));
        return pathologyDisplayService.convertToCaseDisplayItem(pathologySampleId);
    }

    /**
     * A post naming a row the case cannot hold is answered with the sentence the
     * service raised, and a designation two saves claimed at once is a conflict.
     */
    @PostMapping(value = "/rest/pathology/caseView/{pathologySampleId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> getFilteredPathologyEntries(@PathVariable("pathologySampleId") Integer pathologySampleId,
            @RequestBody PathologySampleForm form, HttpServletRequest request) {
        programSampleLabUnitScope.requireVisible(getSysUserId(request), pathologySampleService.get(pathologySampleId));
        form.setSystemUserId(this.getSysUserId(request));
        try {
            pathologySampleService.updateWithFormValues(pathologySampleId, form);
        } catch (PathologyCaseRuleException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            // Designations are worked out in memory, so the case's version check
            // and the unique indexes are the last guards and the save that loses
            // the race has to say so.
            if (concurrentSaveConflict(e)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("conflict", "designation", "error",
                        "another save changed this case at the same moment; reload the case and try again"));
            }
            throw e;
        }

        return ResponseEntity.ok(form);
    }

    /**
     * Walks the cause chain for a designation unique index by name, or for the
     * version check a save loses when another save of the same case committed
     * first, which is how that race usually ends; package-private for its test.
     */
    static boolean concurrentSaveConflict(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof OptimisticLockException || cause instanceof StaleStateException
                    || cause instanceof OptimisticLockingFailureException) {
                return true;
            }
            if (cause.getMessage() != null && (cause.getMessage().contains("pathology_block_designation_uk")
                    || cause.getMessage().contains("pathology_slide_designation_uk"))) {
                return true;
            }
        }
        return false;
    }

    @PostMapping(value = "/rest/pathology/block/{blockId}/deactivate", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> deactivateBlock(@PathVariable("blockId") Integer blockId,
            @RequestBody(required = false) DeactivationRequest body, HttpServletRequest request) {
        PathologySample owner = pathologySampleService.getCaseOfBlock(blockId);
        if (owner != null) {
            programSampleLabUnitScope.requireVisible(getSysUserId(request), owner);
        }
        return pathologySampleService.deactivateBlock(blockId, reasonFrom(body), getSysUserId(request))
                .<ResponseEntity<?>>map(block -> ResponseEntity.ok(toMap(block))).orElseGet(() -> ResponseEntity
                        .status(HttpStatus.NOT_FOUND).body(Map.of("error", "No pathology block with id " + blockId)));
    }

    @PostMapping(value = "/rest/pathology/slide/{slideId}/deactivate", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> deactivateSlide(@PathVariable("slideId") Integer slideId,
            @RequestBody(required = false) DeactivationRequest body, HttpServletRequest request) {
        PathologySample owner = pathologySampleService.getCaseOfSlide(slideId);
        if (owner != null) {
            programSampleLabUnitScope.requireVisible(getSysUserId(request), owner);
        }
        return pathologySampleService.deactivateSlide(slideId, reasonFrom(body), getSysUserId(request))
                .<ResponseEntity<?>>map(slide -> ResponseEntity.ok(toMap(slide))).orElseGet(() -> ResponseEntity
                        .status(HttpStatus.NOT_FOUND).body(Map.of("error", "No pathology slide with id " + slideId)));
    }

    private String reasonFrom(DeactivationRequest body) {
        return body == null ? null : body.reason;
    }

    private Map<String, Object> toMap(PathologyBlock block) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", block.getId());
        row.put("designation", block.getDesignation());
        row.put("barcode", block.getBarcode());
        row.put("cassetteState", block.getCassetteState() == null ? null : block.getCassetteState().name());
        row.put("active", block.isActive());
        return row;
    }

    private Map<String, Object> toMap(PathologySlide slide) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", slide.getId());
        row.put("designation", slide.getDesignation());
        row.put("barcode", slide.getBarcode());
        row.put("blockId", slide.getBlockId());
        row.put("active", slide.isActive());
        return row;
    }

    /**
     * Why the block or slide is being taken out of use; the bench may give none.
     */
    public static class DeactivationRequest {
        public String reason;
    }
}
