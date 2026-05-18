package org.openelisglobal.accreditation.controller.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.accreditation.service.AccreditingBodyService;
import org.openelisglobal.accreditation.service.TestAccreditationService;
import org.openelisglobal.accreditation.valueholder.TestAccreditation;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ControllerUtills;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN') and hasAuthority('TEST_CATALOG_MANAGE')")
public class TestAccreditationRestController extends BaseController {

    @Autowired
    private TestAccreditationService testAccreditationService;

    @Autowired
    private AccreditingBodyService accreditingBodyService;

    @GetMapping(value = "/test-accreditations", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAllTestAccreditations(@RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "bodyId", required = false) Long bodyId,
            @RequestParam(value = "testId", required = false) Long testId,
            @RequestParam(value = "sectionId", required = false) Long sectionId,
            @RequestParam(value = "q", required = false) String q) {
        try {
            if (testId != null || bodyId != null || sectionId != null || !GenericValidator.isBlankOrNull(q)) {
                return ResponseEntity.ok(testAccreditationService.getByFilters(testId, bodyId, sectionId, q));
            }
            if ("active".equalsIgnoreCase(status)) {
                return ResponseEntity.ok(testAccreditationService.getAllActive());
            }
            if ("expired".equalsIgnoreCase(status)) {
                return ResponseEntity.ok(testAccreditationService.getExpiringOnOrBefore(LocalDate.now().minusDays(1)));
            }
            return ResponseEntity.ok(testAccreditationService.getAll());
        } catch (LIMSRuntimeException e) {
            LogEvent.logDebug(e);
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMap);
        }
    }

    @PostMapping(value = "/test-accreditations", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createTestAccreditation(HttpServletRequest request,
            @RequestBody @Valid TestAccreditation testAccreditation, BindingResult result) {
        if (result.hasErrors()) {
            saveErrors(result);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.getAllErrors());
        }
        try {
            testAccreditation.setSysUserId(ControllerUtills.getSysUserId(request));
            Long id = testAccreditationService.insert(testAccreditation);
            TestAccreditation saved = testAccreditationService.get(id);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved != null ? saved : testAccreditation);
        } catch (LIMSDuplicateRecordException e) {
            LogEvent.logDebug(e);
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", e.getMessage());
            errorMap.put("status", 409);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorMap);
        } catch (LIMSRuntimeException e) {
            LogEvent.logDebug(e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PatchMapping(value = "/test-accreditations/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateTestAccreditation(HttpServletRequest request, @PathVariable Long id,
            @RequestBody @Valid TestAccreditation testAccreditation, BindingResult result) {
        if (result.hasErrors()) {
            saveErrors(result);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.getAllErrors());
        }
        try {
            testAccreditation.setId(id);
            testAccreditation.setSysUserId(ControllerUtills.getSysUserId(request));
            TestAccreditation updated = testAccreditationService.update(testAccreditation);
            return ResponseEntity.ok(updated);
        } catch (LIMSRuntimeException e) {
            LogEvent.logDebug(e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping(value = "/test-accreditations/{id}")
    public ResponseEntity<?> deleteTestAccreditation(HttpServletRequest request, @PathVariable Long id) {
        TestAccreditation existing = testAccreditationService.get(id);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("TestAccreditation not found with id: " + id);
        }
        try {
            existing.setSysUserId(ControllerUtills.getSysUserId(request));
            testAccreditationService.delete(existing);
            return ResponseEntity.noContent().build();
        } catch (LIMSRuntimeException e) {
            LogEvent.logDebug(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping(value = "/test-accreditations/bulk-extend", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> bulkExtend(HttpServletRequest request, @RequestBody BulkExtendRequest bulkRequest) {
        if (bulkRequest.getIds() == null || bulkRequest.getIds().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("ids must not be empty");
        }
        if (bulkRequest.getNewExpiresOn() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("new_expires_on is required");
        }
        try {
            testAccreditationService.bulkExtend(bulkRequest.getIds(), bulkRequest.getNewExpiresOn(),
                    ControllerUtills.getSysUserId(request));
            return ResponseEntity.ok("Extended " + bulkRequest.getIds().size() + " accreditation(s)");
        } catch (LIMSRuntimeException e) {
            LogEvent.logDebug(e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping(value = "/accreditation-summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAccreditationSummary() {
        try {
            AccreditationSummary summary = new AccreditationSummary();
            List<TestAccreditation> activeAccreditations = testAccreditationService.getAllActive();
            summary.setTotalActive(activeAccreditations.size());
            summary.setTotalExpired(
                    testAccreditationService.getExpiringOnOrBefore(LocalDate.now().minusDays(1)).size());
            summary.setExpiringSoon(testAccreditationService.getExpiringOnOrBefore(LocalDate.now().plusDays(30)).size()
                    - summary.getTotalExpired());
            summary.setTotalBodies(accreditingBodyService.getAllOrderedByDisplayOrder().size());

            Map<String, Long> countsByBody = activeAccreditations.stream()
                    .collect(Collectors.groupingBy(ta -> ta.getAccreditingBody().getCode(), Collectors.counting()));
            summary.setCountsByBody(countsByBody);

            return ResponseEntity.ok(summary);
        } catch (LIMSRuntimeException e) {
            LogEvent.logDebug(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

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

    public static class BulkExtendRequest {
        @JsonProperty("ids")
        private List<Long> ids;

        @JsonProperty("new_expires_on")
        private LocalDate newExpiresOn;

        public List<Long> getIds() {
            return ids;
        }

        public void setIds(List<Long> ids) {
            this.ids = ids;
        }

        public LocalDate getNewExpiresOn() {
            return newExpiresOn;
        }

        public void setNewExpiresOn(LocalDate newExpiresOn) {
            this.newExpiresOn = newExpiresOn;
        }
    }

    public static class AccreditationSummary {
        private int totalActive;
        private int totalExpired;
        private int expiringSoon;
        private int totalBodies;
        private Map<String, Long> countsByBody;

        public int getTotalActive() {
            return totalActive;
        }

        public void setTotalActive(int totalActive) {
            this.totalActive = totalActive;
        }

        public int getTotalExpired() {
            return totalExpired;
        }

        public void setTotalExpired(int totalExpired) {
            this.totalExpired = totalExpired;
        }

        public int getExpiringSoon() {
            return expiringSoon;
        }

        public void setExpiringSoon(int expiringSoon) {
            this.expiringSoon = expiringSoon;
        }

        public int getTotalBodies() {
            return totalBodies;
        }

        public void setTotalBodies(int totalBodies) {
            this.totalBodies = totalBodies;
        }

        public Map<String, Long> getCountsByBody() {
            return countsByBody;
        }

        public void setCountsByBody(Map<String, Long> countsByBody) {
            this.countsByBody = countsByBody;
        }
    }
}
