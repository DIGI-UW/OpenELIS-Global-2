package org.openelisglobal.testcatalog.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testcatalog.service.CatalogHealthService;
import org.openelisglobal.testvariant.service.TestVariantLinkService;
import org.openelisglobal.testvariant.valueholder.TestVariantLink;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OGC-1142 / FR-46–55 — specimen-variant grouping API.
 *
 * <p>
 * Admin linking ("Link variants" over ≥2 selected rows) and unlinking (FR-51),
 * plus a per-test group read used by the group header and combined editor.
 * Group membership is the persisted {@code test_variant_link} record; there is
 * no name matching (FR-46). Shares the {@code /rest/test-catalog} base +
 * ROLE_ADMIN gate with the other catalog controllers.
 */
@RestController
@RequestMapping("/rest/test-catalog/variants")
@PreAuthorize("hasRole('ADMIN')")
public class TestVariantRestController {

    private final TestVariantLinkService variantLinkService;

    private final TestService testService;

    private final CatalogHealthService catalogHealthService;

    public TestVariantRestController(TestVariantLinkService variantLinkService, TestService testService,
            CatalogHealthService catalogHealthService) {
        this.variantLinkService = variantLinkService;
        this.testService = testService;
        this.catalogHealthService = catalogHealthService;
    }

    public static class LinkRequest {
        public List<String> testIds;
    }

    public static class UnlinkRequest {
        public String testId;
    }

    public static class GroupMember {
        public String testId;
        public String name;
        public boolean active;
        public String domain;
    }

    public static class GroupResponse {
        public String groupId;
        public List<GroupMember> members = new ArrayList<>();
    }

    /** FR-51 — link the given tests into one assay group. */
    @PostMapping(value = "/link", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GroupResponse> link(@RequestBody LinkRequest body, HttpServletRequest request) {
        if (body == null || body.testIds == null || body.testIds.size() < 2) {
            return ResponseEntity.unprocessableEntity().build();
        }
        String groupId = variantLinkService.linkTests(body.testIds, ControllerUtills.getSysUserId(request));
        if (groupId == null) {
            return ResponseEntity.unprocessableEntity().build();
        }
        catalogHealthService.invalidate();
        return ResponseEntity.ok(toGroupResponse(groupId));
    }

    /** FR-51 — remove a test from its group without touching any field. */
    @PostMapping(value = "/unlink", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> unlink(@RequestBody UnlinkRequest body, HttpServletRequest request) {
        if (body == null || body.testId == null || body.testId.isBlank()) {
            return ResponseEntity.unprocessableEntity().build();
        }
        variantLinkService.unlink(body.testId, ControllerUtills.getSysUserId(request));
        catalogHealthService.invalidate();
        return ResponseEntity.noContent().build();
    }

    /** The variant group a test belongs to (empty members list if ungrouped). */
    @GetMapping(value = "/tests/{testId}/group", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GroupResponse> getGroup(@PathVariable String testId) {
        TestVariantLink link = variantLinkService.getByTestId(testId);
        if (link == null) {
            GroupResponse empty = new GroupResponse();
            return ResponseEntity.ok(empty);
        }
        return ResponseEntity.ok(toGroupResponse(link.getGroupId()));
    }

    private GroupResponse toGroupResponse(String groupId) {
        GroupResponse resp = new GroupResponse();
        resp.groupId = groupId;
        for (TestVariantLink link : variantLinkService.getByGroupId(groupId)) {
            Test test = testService.getTestById(link.getTestId());
            if (test == null) {
                continue;
            }
            GroupMember m = new GroupMember();
            m.testId = test.getId();
            m.name = TestServiceImpl.getLocalizedTestNameWithType(test);
            m.active = test.isActive();
            m.domain = test.getDomain();
            resp.members.add(m);
        }
        return resp;
    }
}
