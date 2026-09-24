package org.openelisglobal.testalertrule.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testalertrule.service.TestAlertRuleService;
import org.openelisglobal.testalertrule.valueholder.TestAlertRule;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * OGC-949 / OGC-763 — CRUD endpoints for per-test alert rules, consumed by the
 * v2 Alerts section (OGC-994..997).
 *
 * <p>
 * Authoring only: the rule's trigger, channels, recipients and
 * {@code acknowledgmentRequired} flag live here; delivery + message templates
 * are the shipped Notification system's responsibility. Base path stays in the
 * unified editor namespace; gated by {@code ROLE_ADMIN} (non-admins get 403),
 * matching the other Test Catalog editor controllers.
 */
@RestController
@RequestMapping("/rest/test-catalog/{testId}/alerts")
@PreAuthorize("hasRole('ADMIN')")
public class TestAlertRuleRestController extends BaseRestController {

    private static final Set<String> TRIGGER_TYPES = Set.of("ALL", "ABNORMAL", "CRITICAL", "SPECIFIC_VALUE",
            "COMPLIANCE_BREACH");

    private final TestAlertRuleService alertRuleService;

    private final TestService testService;

    private final RoleService roleService;

    private final TestResultComponentService testResultComponentService;

    private final TypeOfSampleService typeOfSampleService;

    public TestAlertRuleRestController(TestAlertRuleService alertRuleService, TestService testService,
            RoleService roleService, TestResultComponentService testResultComponentService,
            TypeOfSampleService typeOfSampleService) {
        this.alertRuleService = alertRuleService;
        this.testService = testService;
        this.roleService = roleService;
        this.testResultComponentService = testResultComponentService;
        this.typeOfSampleService = typeOfSampleService;
    }

    /**
     * The components a rule on this test may name, so the editor can offer them. A
     * single-component test returns its one component and the UI can stay simple; a
     * multi-component test has to be explicit about which measurement the rule is
     * about.
     */
    @GetMapping(value = "/components", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<IdValuePair> components(@PathVariable String testId) {
        requireTest(testId);
        return testResultComponentService.getActiveComponentsByTestId(testId).stream()
                .map(c -> new IdValuePair(c.getId(), c.getLabel() == null ? c.getCode() : c.getLabel()))
                .collect(Collectors.toList());
    }

    /** The specimens this test runs on, for the rule's specimen scope. */
    @GetMapping(value = "/sample-types", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<IdValuePair> sampleTypes(@PathVariable String testId) {
        requireTest(testId);
        return typeOfSampleService.getTypeOfSampleForTest(testId).stream()
                .map(t -> new IdValuePair(t.getId(), t.getLocalizedName())).collect(Collectors.toList());
    }

    /**
     * Create/update payload for an alert rule.
     *
     * <p>
     * {@code id}, {@code testId} and {@code lastupdated} are server-managed but
     * deliberately declared so a client may round-trip the representation GET
     * returns (the REST norm for PUT). Their values are ignored on write — identity
     * comes from the path and the version from the database. Truly unknown fields
     * still fail deserialization: the contract stays explicit.
     */
    public static class AlertRuleRequest {
        public String id;
        public String testId;
        public java.sql.Timestamp lastupdated;
        public String name;
        public Boolean enabled;
        public String triggerType;
        public String triggerValue;
        /** Result component the rule watches; null watches every component. */
        public String componentId;
        /** Specimen the rule watches; null watches every specimen. */
        public String sampleTypeId;
        public Boolean notifySms;
        public Boolean notifyEmail;
        public Boolean notifyOrderingPhysician;
        public Boolean notifyPatient;
        public Boolean notifyReferringFacility;
        public String notifyCustomPhone;
        public String notifyCustomEmail;
        public String notifyRoleId;
        public Boolean acknowledgmentRequired;
    }

    /**
     * Response shape for an alert rule — a plain DTO like every other catalog
     * section, never the Hibernate entity (which leaks {@code lastupdated} /
     * {@code sysUserId} and made the GET representation un-PUT-able).
     */
    public static class AlertRuleDto {
        public String id;
        public String testId;
        public String name;
        public Boolean enabled;
        public String triggerType;
        public String triggerValue;
        public String componentId;
        public String sampleTypeId;
        public Boolean notifySms;
        public Boolean notifyEmail;
        public Boolean notifyOrderingPhysician;
        public Boolean notifyPatient;
        public Boolean notifyReferringFacility;
        public String notifyCustomPhone;
        public String notifyCustomEmail;
        public String notifyRoleId;
        public Boolean acknowledgmentRequired;

        static AlertRuleDto of(TestAlertRule rule) {
            AlertRuleDto dto = new AlertRuleDto();
            dto.id = rule.getId();
            dto.testId = rule.getTestId();
            dto.name = rule.getName();
            dto.enabled = rule.getEnabled();
            dto.triggerType = rule.getTriggerType();
            dto.triggerValue = rule.getTriggerValue();
            dto.componentId = rule.getComponentId();
            dto.sampleTypeId = rule.getSampleTypeId();
            dto.notifySms = rule.getNotifySms();
            dto.notifyEmail = rule.getNotifyEmail();
            dto.notifyOrderingPhysician = rule.getNotifyOrderingPhysician();
            dto.notifyPatient = rule.getNotifyPatient();
            dto.notifyReferringFacility = rule.getNotifyReferringFacility();
            dto.notifyCustomPhone = rule.getNotifyCustomPhone();
            dto.notifyCustomEmail = rule.getNotifyCustomEmail();
            dto.notifyRoleId = rule.getNotifyRoleId();
            dto.acknowledgmentRequired = rule.getAcknowledgmentRequired();
            return dto;
        }
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AlertRuleDto> list(@PathVariable String testId) {
        requireTest(testId);
        return alertRuleService.getByTestId(testId).stream().map(AlertRuleDto::of).collect(Collectors.toList());
    }

    /**
     * Selectable roles for the "notify role" recipient (id + human description).
     * Grouping roles are excluded since they are containers, not assignable roles.
     */
    @GetMapping(value = "/roles", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<IdValuePair> roles(@PathVariable String testId) {
        return roleService.getAllActiveRoles().stream().filter(r -> !r.getGroupingRole())
                .map(r -> new IdValuePair(r.getId(), r.getName())).collect(Collectors.toList());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AlertRuleDto> create(@PathVariable String testId, @RequestBody AlertRuleRequest body,
            HttpServletRequest request) {
        requireTest(testId);
        validate(body);
        validateScope(testId, body);

        TestAlertRule rule = new TestAlertRule();
        rule.setTestId(testId);
        apply(rule, body);
        rule.setSysUserId(ControllerUtills.getSysUserId(request));
        alertRuleService.insert(rule);
        return ResponseEntity.status(HttpStatus.CREATED).body(AlertRuleDto.of(rule));
    }

    @PutMapping(value = "/{ruleId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public AlertRuleDto update(@PathVariable String testId, @PathVariable String ruleId,
            @RequestBody AlertRuleRequest body, HttpServletRequest request) {
        TestAlertRule rule = requireRule(testId, ruleId);
        validate(body);
        validateScope(testId, body);
        apply(rule, body);
        rule.setSysUserId(ControllerUtills.getSysUserId(request));
        alertRuleService.update(rule);
        return AlertRuleDto.of(rule);
    }

    @DeleteMapping(value = "/{ruleId}")
    public ResponseEntity<Void> delete(@PathVariable String testId, @PathVariable String ruleId,
            HttpServletRequest request) {
        TestAlertRule rule = requireRule(testId, ruleId);
        rule.setSysUserId(ControllerUtills.getSysUserId(request));
        alertRuleService.delete(rule);
        return ResponseEntity.noContent().build();
    }

    private void requireTest(String testId) {
        Test test = testService.getTestById(testId);
        if (test == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found: " + testId);
        }
    }

    private TestAlertRule requireRule(String testId, String ruleId) {
        requireTest(testId);
        // Scope the lookup to the test so a rule from another test 404s here too.
        return alertRuleService.getByTestId(testId).stream().filter(r -> r.getId().equals(ruleId)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Alert rule not found on this test: " + ruleId));
    }

    private void validate(AlertRuleRequest body) {
        if (body.name == null || body.name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        if (body.triggerType == null || !TRIGGER_TYPES.contains(body.triggerType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "triggerType must be one of ALL, ABNORMAL, CRITICAL, SPECIFIC_VALUE, COMPLIANCE_BREACH");
        }
        if ("SPECIFIC_VALUE".equals(body.triggerType) && (body.triggerValue == null || body.triggerValue.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "triggerValue is required when triggerType is SPECIFIC_VALUE");
        }
    }

    /**
     * A rule may only name a component of its own test and a specimen that test
     * actually runs on. Without this the API accepts a rule that can never match
     * anything — it looks configured and silently never fires.
     */
    private void validateScope(String testId, AlertRuleRequest body) {
        if (body.componentId != null && !body.componentId.isBlank()) {
            boolean belongs = testResultComponentService.getActiveComponentsByTestId(testId).stream()
                    .anyMatch(c -> body.componentId.equals(c.getId()));
            if (!belongs) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "componentId is not an active component of this test: " + body.componentId);
            }
        }
        if (body.sampleTypeId != null && !body.sampleTypeId.isBlank()) {
            boolean runsOn = typeOfSampleService.getTypeOfSampleForTest(testId).stream()
                    .anyMatch(t -> body.sampleTypeId.equals(t.getId()));
            if (!runsOn) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "sampleTypeId is not a specimen this test runs on: " + body.sampleTypeId);
            }
        }
    }

    private void apply(TestAlertRule rule, AlertRuleRequest body) {
        rule.setName(body.name);
        rule.setComponentId(body.componentId == null || body.componentId.isBlank() ? null : body.componentId);
        rule.setSampleTypeId(body.sampleTypeId == null || body.sampleTypeId.isBlank() ? null : body.sampleTypeId);
        rule.setEnabled(body.enabled == null ? Boolean.TRUE : body.enabled);
        rule.setTriggerType(body.triggerType);
        rule.setTriggerValue("SPECIFIC_VALUE".equals(body.triggerType) ? body.triggerValue : null);
        rule.setNotifySms(Boolean.TRUE.equals(body.notifySms));
        rule.setNotifyEmail(Boolean.TRUE.equals(body.notifyEmail));
        rule.setNotifyOrderingPhysician(Boolean.TRUE.equals(body.notifyOrderingPhysician));
        rule.setNotifyPatient(Boolean.TRUE.equals(body.notifyPatient));
        rule.setNotifyReferringFacility(Boolean.TRUE.equals(body.notifyReferringFacility));
        rule.setNotifyCustomPhone(body.notifyCustomPhone);
        rule.setNotifyCustomEmail(body.notifyCustomEmail);
        rule.setNotifyRoleId(body.notifyRoleId);
        rule.setAcknowledgmentRequired(Boolean.TRUE.equals(body.acknowledgmentRequired));
    }
}
