package org.openelisglobal.program.controller.immunohistochemistry;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.rest.util.DashboardPage;
import org.openelisglobal.common.rest.util.DashboardPaging;
import org.openelisglobal.program.bean.ImmunohistochemistryDashBoardCount;
import org.openelisglobal.program.service.ImmunohistochemistryDisplayService;
import org.openelisglobal.program.service.ImmunohistochemistrySampleService;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistryCaseViewDisplayItem;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistryDisplayItem;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample.ImmunohistochemistryStatus;
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
public class ImmunohistochemistryController extends BaseRestController {

    @Autowired
    private ImmunohistochemistrySampleService immunohistochemistrySampleService;
    @Autowired
    private ImmunohistochemistryDisplayService immunohistochemistryDisplayService;
    @Autowired
    private SystemUserService systemUserService;

    /**
     * Ids of the cases the last search found, in pages of paging.results.pageSize.
     */
    private final DashboardPaging<Integer> dashboardPaging = new DashboardPaging<>("immunohistochemistryDashboard");

    /**
     * One page of the dashboard. A request with {@code page} re-slices the list the
     * session already holds; any other request runs the search again and answers
     * with its first page. Only the page's rows are built.
     */
    @GetMapping(value = "/rest/immunohistochemistry/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public DashboardPage<ImmunohistochemistryDisplayItem> getFilteredImmunohistochemistryEntries(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) ImmunohistochemistryStatus[] statuses,
            @RequestParam(required = false) Integer page, HttpServletRequest request) {
        HttpSession session = request.getSession();
        List<Integer> pageIds;
        int pageNumber;
        if (page != null) {
            pageNumber = Math.max(page, 1);
            pageIds = dashboardPaging.page(session, pageNumber);
        } else {
            pageNumber = 1;
            List<ImmunohistochemistryStatus> requested = statuses == null ? List.of() : Arrays.asList(statuses);
            pageIds = dashboardPaging.cache(session, immunohistochemistrySampleService
                    .searchWithStatusAndTerm(requested, searchTerm).stream().map(e -> e.getId()).toList());
        }
        List<ImmunohistochemistryDisplayItem> items = pageIds.stream()
                .map(immunohistochemistryDisplayService::convertToDisplayItem).collect(Collectors.toList());
        return new DashboardPage<>(items, dashboardPaging.pagingBean(session, pageNumber),
                dashboardPaging.totalItems(session));
    }

    @GetMapping(value = "/rest/immunohistochemistry/dashboard/count", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ImmunohistochemistryDashBoardCount> getFilteredImmunohistochemistryEntries() {
        ImmunohistochemistryDashBoardCount count = new ImmunohistochemistryDashBoardCount();
        count.setInProgress(immunohistochemistrySampleService
                .getCountWithStatus(Arrays.asList(ImmunohistochemistryStatus.IN_PROGRESS)));
        count.setAwaitingReview(immunohistochemistrySampleService
                .getCountWithStatus(Arrays.asList(ImmunohistochemistryStatus.READY_PATHOLOGIST)));

        Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
        Instant weekAgoInstant = Instant.now().minus(7, ChronoUnit.DAYS);
        Timestamp weekAgoTimestamp = Timestamp.from(weekAgoInstant);

        count.setComplete(immunohistochemistrySampleService.getCountWithStatusBetweenDates(
                Arrays.asList(ImmunohistochemistryStatus.COMPLETED), weekAgoTimestamp, currentTimestamp));

        return ResponseEntity.ok(count);
    }

    @PostMapping(value = "/rest/immunohistochemistry/assignTechnician", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> assignTechnician(@RequestParam Integer immunohistochemistrySampleId,
            HttpServletRequest request) {
        String currentUserId = getSysUserId(request);
        immunohistochemistrySampleService.assignTechnician(immunohistochemistrySampleId,
                systemUserService.get(currentUserId));
        return ResponseEntity.ok("ok");
    }

    @PostMapping(value = "/rest/immunohistochemistry/assignPathologist", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> assignPathologist(@RequestParam Integer immunohistochemistrySampleId,
            HttpServletRequest request) {
        String currentUserId = getSysUserId(request);
        immunohistochemistrySampleService.assignPathologist(immunohistochemistrySampleId,
                systemUserService.get(currentUserId));
        return ResponseEntity.ok("ok");
    }

    @GetMapping(value = "/rest/immunohistochemistry/caseView/{immunohistochemistrySampleId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ImmunohistochemistryCaseViewDisplayItem getFilteredImmunohistochemistryEntries(
            @PathVariable("immunohistochemistrySampleId") Integer immunohistochemistrySampleId) {
        return immunohistochemistryDisplayService.convertToCaseDisplayItem(immunohistochemistrySampleId);
    }

    @PostMapping(value = "/rest/immunohistochemistry/caseView/{immunohistochemistrySampleId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ImmunohistochemistrySampleForm getFilteredImmunohistochemistryEntries(
            @PathVariable("immunohistochemistrySampleId") Integer immunohistochemistrySampleId,
            @RequestBody ImmunohistochemistrySampleForm form, HttpServletRequest request) {
        form.setSystemUserId(this.getSysUserId(request));
        immunohistochemistrySampleService.updateWithFormValues(immunohistochemistrySampleId, form);

        return form;
    }
}
