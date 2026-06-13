package org.openelisglobal.testmethod.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.sql.Date;
import java.util.List;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
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
        java.sql.Date effectiveDate;
        try {
            effectiveDate = Date.valueOf(req.effectiveDate);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("effectiveDate must be yyyy-MM-dd");
        }
        if (testMethodService.testMethodLinkExists(testId, req.methodId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Method already linked to this test");
        }
        TestMethod tm = new TestMethod();
        tm.setTestId(testId);
        tm.setMethodId(req.methodId);
        tm.setIsDefaultMethod(req.isDefault);
        tm.setEffectiveDate(effectiveDate);
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
        try {
            data.effectiveDate = Date.valueOf(req.effectiveDate);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("effectiveDate must be yyyy-MM-dd");
        }
        data.sysUserId = getSysUserId(request);
        TestMethodDto dto;
        try {
            dto = testMethodService.createAndLinkMethod(testId, data);
        } catch (RuntimeException e) {
            // Only a genuine uniqueness/constraint violation is a 409; anything
            // else is a real server error and must not be masked as a conflict.
            if (hasCause(e, org.hibernate.exception.ConstraintViolationException.class)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Method code already exists");
            }
            LogEvent.logError(e);
            throw e;
        }
        // Refresh the cached method display lists AFTER the create transaction
        // commits — refreshing inside the service @Transactional would poison the
        // process-wide static cache if the transaction later rolled back.
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.METHODS);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.METHODS_INACTIVE);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    private static boolean hasCause(Throwable t, Class<? extends Throwable> type) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (type.isInstance(c)) {
                return true;
            }
        }
        return false;
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateLink(@PathVariable String testId, @PathVariable String id,
            @RequestBody UpdateLinkRequest req, HttpServletRequest request) {
        TestMethod existing = testMethodService.findLinkById(id);
        if (existing == null || !existing.getTestId().equals(testId)) {
            return ResponseEntity.notFound().build();
        }
        java.sql.Date effectiveDate = existing.getEffectiveDate();
        if (req.effectiveDate != null && !req.effectiveDate.isBlank()) {
            try {
                effectiveDate = Date.valueOf(req.effectiveDate);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("effectiveDate must be yyyy-MM-dd");
            }
        }
        // Carry the new values on a fresh, unmanaged instance — never mutate the
        // request-managed entity, or OSIV auto-flushes it and collides with the
        // bulk @Version update (StaleObjectStateException -> 500).
        TestMethod update = new TestMethod();
        update.setId(existing.getId());
        update.setTestId(existing.getTestId());
        update.setMethodId(existing.getMethodId());
        update.setIsActive(existing.getIsActive());
        update.setIsDefaultMethod(req.isDefault != null ? req.isDefault : existing.getIsDefaultMethod());
        update.setEffectiveDate(effectiveDate);
        update.setSysUserId(getSysUserId(request));
        return ResponseEntity.ok(testMethodService.updateLinkDto(update));
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<?> removeLink(@PathVariable String testId, @PathVariable String id,
            HttpServletRequest request) {
        TestMethod tm = testMethodService.findLinkById(id);
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
