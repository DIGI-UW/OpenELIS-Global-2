package org.openelisglobal.testmethod.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.sql.Date;
import java.util.List;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.testmethod.service.TestMethodService;
import org.openelisglobal.testmethod.service.TestMethodService.InlineCreateData;
import org.openelisglobal.testmethod.service.TestMethodService.TestMethodDto;
import org.openelisglobal.testmethod.valueholder.TestMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/test/{testId}/methods")
@PreAuthorize("hasRole('ADMIN')")
public class TestMethodRestController extends BaseController {

    @Autowired
    private TestMethodService testMethodService;

    // ── Request bodies ────────────────────────────────────────────────────────

    public static class LinkMethodRequest {
        @NotBlank
        public String methodId;
        public boolean isDefault;
        @NotBlank
        public String effectiveDate;
    }

    public static class InlineCreateRequest {
        @NotBlank
        public String nameEnglish;
        @NotBlank
        public String nameFrench;
        @NotBlank
        @Pattern(regexp = "^[A-Z0-9]{3,10}$", message = "Code must be 3-10 uppercase alphanumeric characters")
        public String code;
        public boolean isDefault;
        @NotBlank
        public String effectiveDate;
    }

    public static class UpdateLinkRequest {
        public Boolean isDefault;
        public String effectiveDate;
    }

    // ── Endpoints ─────────────────────────────────────────────────────────────

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TestMethodDto>> getLinkedMethods(@PathVariable String testId) {
        return ResponseEntity.ok(testMethodService.getLinkedMethodDtos(testId));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> linkMethod(@PathVariable String testId, @RequestBody @Valid LinkMethodRequest req,
            HttpServletRequest request) {
        if (testMethodService.testMethodLinkExists(testId, req.methodId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Method already linked to this test");
        }
        TestMethod tm = new TestMethod();
        tm.setTestId(testId);
        tm.setMethodId(req.methodId);
        tm.setIsDefaultMethod(req.isDefault);
        tm.setEffectiveDate(Date.valueOf(req.effectiveDate));
        tm.setSysUserId(getSysUserId(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(testMethodService.linkMethodDto(tm));
    }

    @PostMapping(value = "/inline-create", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> inlineCreateAndLink(@PathVariable String testId,
            @RequestBody @Valid InlineCreateRequest req, HttpServletRequest request) {
        InlineCreateData data = new InlineCreateData();
        data.nameEnglish = req.nameEnglish;
        data.nameFrench = req.nameFrench;
        data.code = req.code;
        data.isDefault = req.isDefault;
        data.effectiveDate = Date.valueOf(req.effectiveDate);
        data.sysUserId = getSysUserId(request);
        try {
            TestMethodDto dto = testMethodService.createAndLinkMethod(testId, data);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (Exception e) {
            LogEvent.logDebug(e);
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Method name or code already exists");
        }
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateLink(@PathVariable String testId, @PathVariable String id,
            @RequestBody UpdateLinkRequest req, HttpServletRequest request) {
        TestMethod tm = testMethodService.get(id);
        if (tm == null || !tm.getTestId().equals(testId)) {
            return ResponseEntity.notFound().build();
        }
        if (req.isDefault != null) {
            tm.setIsDefaultMethod(req.isDefault);
        }
        if (req.effectiveDate != null && !req.effectiveDate.isBlank()) {
            tm.setEffectiveDate(Date.valueOf(req.effectiveDate));
        }
        tm.setSysUserId(getSysUserId(request));
        return ResponseEntity.ok(testMethodService.updateLinkDto(tm));
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<?> removeLink(@PathVariable String testId, @PathVariable String id,
            HttpServletRequest request) {
        TestMethod tm = testMethodService.get(id);
        if (tm == null || !tm.getTestId().equals(testId)) {
            return ResponseEntity.notFound().build();
        }
        testMethodService.removeLink(id, getSysUserId(request));
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/copyFrom/{sourceTestId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> copyFromTest(@PathVariable String testId, @PathVariable String sourceTestId,
            HttpServletRequest request) {
        testMethodService.copyMethodsFromTest(sourceTestId, testId, getSysUserId(request));
        return ResponseEntity.ok(testMethodService.getLinkedMethodDtos(testId));
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected String findLocalForward(String forward) {
        return "PageNotFound";
    }

    @Override
    protected String getPageTitleKey() {
        return null;
    }

    @Override
    protected String getPageSubtitleKey() {
        return null;
    }
}
