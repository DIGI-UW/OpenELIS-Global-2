package org.openelisglobal.validationpolicy.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.services.StaleSaveGuard;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.validationpolicy.dao.ValidationPolicyDAO;
import org.openelisglobal.validationpolicy.form.LabUnitPoliciesView;
import org.openelisglobal.validationpolicy.form.LabUnitPolicyForm;
import org.openelisglobal.validationpolicy.form.LabUnitPolicyView;
import org.openelisglobal.validationpolicy.form.LevelRoleOption;
import org.openelisglobal.validationpolicy.form.ValidationPolicyForm;
import org.openelisglobal.validationpolicy.form.ValidationPolicyView;
import org.openelisglobal.validationpolicy.valueholder.ReviewTrigger;
import org.openelisglobal.validationpolicy.valueholder.ValidationPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ValidationPolicyServiceImpl implements ValidationPolicyService {

    static final String VALIDATE_ALL_RESULTS = "validate all results";

    private static final String ONE_OVERRIDE_PER_UNIT = "uq_validation_policy_test_section";

    static final String AUDIT_TABLE = "validation_policy";

    @Autowired
    private ValidationPolicyDAO validationPolicyDAO;
    @Lazy
    @Autowired
    private RoleService roleService;
    @Lazy
    @Autowired
    private SiteInformationService siteInformationService;
    @Lazy
    @Autowired
    private TestSectionService testSectionService;
    @Lazy
    @Autowired
    private AnalysisReviewService analysisReviewService;
    @Lazy
    @Autowired
    private AuditTrailService auditTrailService;

    @Override
    @Transactional(readOnly = true)
    public ValidationPolicyView getLabWideDefault() {
        return toView(labWideDefault());
    }

    @Override
    @Transactional
    public ValidationPolicyView saveLabWideDefault(ValidationPolicyForm form, String sysUserId) {
        ValidationPolicy policy = validationPolicyDAO.findLabWideDefault().orElseGet(ValidationPolicy::new);
        refuseStale(policy, form.lastupdated());
        ReviewTrigger trigger = apply(policy, form, sysUserId);
        writeValidateAllResults(trigger == ReviewTrigger.ALL, sysUserId);
        return toView(policy);
    }

    @Override
    @Transactional(readOnly = true)
    public LabUnitPoliciesView getLabUnitPolicies() {
        List<LabUnitPoliciesView.LabUnit> labUnits = testSectionService.getAllActiveTestSections().stream()
                .map(unit -> new LabUnitPoliciesView.LabUnit(unit.getId(), labUnitName(unit), unit.getDomain()))
                .toList();
        List<LabUnitPolicyView> overrides = validationPolicyDAO.findLabUnitOverrides().stream().map(this::toLabUnitView)
                .toList();
        return new LabUnitPoliciesView(labUnits, overrides);
    }

    @Override
    @Transactional
    public LabUnitPolicyView addLabUnitOverride(LabUnitPolicyForm form, String sysUserId) {
        if (StringUtils.isBlank(form.testSectionId())) {
            throw new ValidationPolicyRefusedException("testSectionId", "labUnitRequired");
        }
        TestSection unit = activeLabUnit(form.testSectionId());
        Integer testSectionId = Integer.valueOf(unit.getId());
        if (validationPolicyDAO.findByTestSectionId(testSectionId).isPresent()) {
            throw new ValidationPolicyRefusedException("testSectionId", "labUnitHasOverride");
        }
        ValidationPolicy policy = new ValidationPolicy();
        policy.setTestSectionId(testSectionId);
        try {
            apply(policy, form.policy(), sysUserId);
        } catch (RuntimeException e) {
            if (violatesOneOverridePerUnit(e)) {
                throw new ValidationPolicyRefusedException("testSectionId", "labUnitHasOverride");
            }
            throw e;
        }
        return toLabUnitView(policy);
    }

    private static boolean violatesOneOverridePerUnit(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (String.valueOf(cause.getMessage()).contains(ONE_OVERRIDE_PER_UNIT)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional
    public LabUnitPolicyView saveLabUnitOverride(Long id, ValidationPolicyForm form, String sysUserId) {
        ValidationPolicy policy = labUnitOverride(id);
        refuseStale(policy, form.lastupdated());
        if (!policy.isActive()) {
            throw new ValidationPolicyRefusedException("id", "overrideDeactivated");
        }
        apply(policy, form, sysUserId);
        return toLabUnitView(policy);
    }

    @Override
    @Transactional
    public LabUnitPolicyView setLabUnitOverrideActive(Long id, boolean active, String lastupdated, String sysUserId) {
        ValidationPolicy policy = labUnitOverride(id);
        refuseStale(policy, lastupdated);
        if (policy.isActive() != active) {
            ValidationPolicy prior = copyOf(policy);
            policy.setActive(active);
            stamp(policy, sysUserId);
            validationPolicyDAO.update(policy);
            auditTrailService.saveHistory(policy, prior, sysUserId, IActionConstants.AUDIT_TRAIL_UPDATE, AUDIT_TABLE);
        }
        return toLabUnitView(policy);
    }

    private static void refuseStale(ValidationPolicy policy, String lastupdated) {
        if (policy.getId() != null && StaleSaveGuard.isStale(lastupdated, policy.getLastupdated())) {
            throw new ValidationPolicyStaleException(policy.getId(), policy.getLastupdated());
        }
    }

    private static ValidationPolicy copyOf(ValidationPolicy policy) {
        ValidationPolicy copy = new ValidationPolicy();
        copy.setId(policy.getId());
        copy.setTestSectionId(policy.getTestSectionId());
        copy.setReviewTrigger(policy.getReviewTrigger());
        copy.setReviewLevels(policy.getReviewLevels());
        copy.setActive(policy.isActive());
        copy.setChangedBy(policy.getChangedBy());
        copy.setLevelRoleIds(new HashMap<>(policy.getLevelRoleIds()));
        copy.setLastupdated(policy.getLastupdated());
        copy.setSysUserId(policy.getSysUserId());
        return copy;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReviewTrigger> entryReviewTrigger(String testSectionId) {
        return effectivePolicy(testSectionId).map(ValidationPolicy::getReviewTrigger);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ValidationPolicy> effectivePolicy(String testSectionId) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        Optional<ValidationPolicy> override = StringUtils.isNumeric(testSectionId)
                ? validationPolicyDAO.findByTestSectionId(Integer.valueOf(testSectionId))
                        .filter(ValidationPolicy::isActive)
                : Optional.empty();
        return Optional.of(override.orElseGet(this::labWideDefault));
    }

    private ValidationPolicy labWideDefault() {
        return validationPolicyDAO.findLabWideDefault().orElseGet(this::fromValidateAllResults);
    }

    private ReviewTrigger apply(ValidationPolicy policy, ValidationPolicyForm form, String sysUserId) {
        ReviewTrigger trigger = parseTrigger(form.trigger());
        int levels = checkedLevels(trigger, form.levels());
        Map<Integer, Integer> roles = levelRoles(levels, form.roleIds());
        ValidationPolicy prior = policy.getId() == null ? null : copyOf(policy);
        policy.setReviewTrigger(trigger);
        policy.setReviewLevels(levels);
        policy.setActive(true);
        policy.getLevelRoleIds().clear();
        policy.getLevelRoleIds().putAll(roles);
        stamp(policy, sysUserId);
        if (prior == null) {
            policy.setId(validationPolicyDAO.insert(policy));
            auditTrailService.saveNewHistory(policy, sysUserId, AUDIT_TABLE);
        } else {
            validationPolicyDAO.update(policy);
            auditTrailService.saveHistory(policy, prior, sysUserId, IActionConstants.AUDIT_TRAIL_UPDATE, AUDIT_TABLE);
        }
        return trigger;
    }

    private void stamp(ValidationPolicy policy, String sysUserId) {
        policy.setSysUserId(sysUserId);
        policy.setChangedBy(StringUtils.isNumeric(sysUserId) ? Integer.valueOf(sysUserId) : null);
    }

    private TestSection activeLabUnit(String testSectionId) {
        if (!StringUtils.isNumeric(testSectionId)) {
            throw new ValidationPolicyRefusedException("testSectionId", "labUnitUnknown");
        }
        return testSectionService.getMatch("id", testSectionId).filter(unit -> "Y".equals(unit.getIsActive()))
                .orElseThrow(() -> new ValidationPolicyRefusedException("testSectionId", "labUnitUnknown"));
    }

    private ValidationPolicy labUnitOverride(Long id) {
        return Optional.ofNullable(id).flatMap(validationPolicyDAO::get)
                .filter(policy -> policy.getTestSectionId() != null)
                .orElseThrow(() -> new ValidationPolicyRefusedException("id", "overrideUnknown"));
    }

    private boolean isEnabled() {
        return ConfigurationProperties.getInstance().isPropertyValueEqual(Property.USE_MULTI_LEVEL_VALIDATION, "true");
    }

    private ReviewTrigger parseTrigger(String trigger) {
        try {
            return ReviewTrigger.valueOf(StringUtils.trimToEmpty(trigger).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ValidationPolicyRefusedException("trigger", "triggerRequired");
        }
    }

    private int checkedLevels(ReviewTrigger trigger, Integer levels) {
        if (levels == null) {
            throw new ValidationPolicyRefusedException("levels", "levelsRequired");
        }
        if (levels < 0 || levels > MAX_LEVELS) {
            throw new ValidationPolicyRefusedException("levels", "levelsOutOfRange");
        }
        if (trigger == ReviewTrigger.NONE && levels > 0) {
            throw new ValidationPolicyRefusedException("levels", "noReviewWithReviews");
        }
        if (trigger != ReviewTrigger.NONE && levels == 0) {
            throw new ValidationPolicyRefusedException("levels", "zeroReviewsNeedsNoReview");
        }
        return levels;
    }

    private Map<Integer, Integer> levelRoles(int levels, List<Integer> roleIds) {
        Integer validationRoleId = validationRoleId();
        java.util.Set<Integer> available = signableRoles().stream().map(LevelRoleOption::id)
                .collect(java.util.stream.Collectors.toSet());
        Map<Integer, Integer> roles = new HashMap<>();
        for (int level = 1; level <= levels; level++) {
            Integer roleId = roleIds == null || roleIds.size() < level || roleIds.get(level - 1) == null
                    ? validationRoleId
                    : roleIds.get(level - 1);
            if (!available.contains(roleId)) {
                throw new ValidationPolicyRefusedException("roleIds", "roleNotAvailable");
            }
            roles.put(level, roleId);
        }
        return roles;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LevelRoleOption> levelRoleOptions(String labUnitId) {
        return signableRoles().stream().map(role -> new LevelRoleOption(role.id(), role.name(), role.labUnitRole(),
                analysisReviewService.signerCount(labUnitId, role.id()))).toList();
    }

    private List<LevelRoleOption> signableRoles() {
        Role labUnitRoles = roleService.getRoleByName(AnalysisReviewServiceImpl.LAB_UNIT_ROLES);
        String labUnitRolesId = labUnitRoles == null ? null : labUnitRoles.getId();
        return roleService.getAllActiveRoles().stream().filter(role -> StringUtils.isNumeric(role.getId())
                && (role.getGroupingParent() != null || LEVEL_ROLE_NAME.equals(StringUtils.trim(role.getName()))))
                .map(role -> new LevelRoleOption(Integer.valueOf(role.getId()), StringUtils.trim(role.getName()),
                        LEVEL_ROLE_NAME.equals(StringUtils.trim(role.getName()))
                                || Objects.equals(role.getGroupingParent(), labUnitRolesId),
                        0))
                .sorted(java.util.Comparator
                        .comparing((LevelRoleOption option) -> !LEVEL_ROLE_NAME.equals(option.name()))
                        .thenComparing(LevelRoleOption::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private Integer validationRoleId() {
        Role role = roleService.getRoleByName(LEVEL_ROLE_NAME);
        if (role == null || !StringUtils.isNumeric(role.getId())) {
            throw new IllegalStateException("The " + LEVEL_ROLE_NAME + " role is missing");
        }
        return Integer.valueOf(role.getId());
    }

    private void writeValidateAllResults(boolean validateAll, String sysUserId) {
        SiteInformation setting = siteInformationService.getSiteInformationByName(VALIDATE_ALL_RESULTS);
        if (setting == null) {
            return;
        }
        String value = Boolean.toString(validateAll);
        if (!value.equals(StringUtils.trimToEmpty(setting.getValue()))) {
            setting.setValue(value);
            setting.setSysUserId(sysUserId);
            siteInformationService.update(setting);
        }
    }

    private ValidationPolicy fromValidateAllResults() {
        ValidationPolicy policy = new ValidationPolicy();
        boolean validateAll = ConfigurationProperties.getInstance()
                .isPropertyValueEqual(Property.ALWAYS_VALIDATE_RESULTS, "true");
        policy.setReviewTrigger(validateAll ? ReviewTrigger.ALL : ReviewTrigger.NEEDS_REVIEW);
        policy.setReviewLevels(1);
        policy.getLevelRoleIds().put(1, validationRoleId());
        return policy;
    }

    private String labUnitName(TestSection unit) {
        return StringUtils.defaultIfBlank(testSectionService.getUserLocalizedTesSectionName(unit),
                StringUtils.trimToEmpty(unit.getTestSectionName()));
    }

    private List<ValidationPolicyView.Level> levels(ValidationPolicy policy) {
        String labUnitId = policy.getTestSectionId() == null ? null : policy.getTestSectionId().toString();
        List<ValidationPolicyView.Level> levels = new ArrayList<>();
        for (int level = 1; level <= policy.getReviewLevels(); level++) {
            Integer roleId = policy.getLevelRoleIds().get(level);
            Role role = roleId == null ? null : roleService.getRoleById(roleId.toString());
            levels.add(new ValidationPolicyView.Level(level, roleId,
                    role == null ? null : StringUtils.trim(role.getName()),
                    analysisReviewService.signerCount(labUnitId, roleId)));
        }
        return levels;
    }

    private LabUnitPolicyView toLabUnitView(ValidationPolicy policy) {
        String testSectionId = policy.getTestSectionId().toString();
        Optional<TestSection> unit = testSectionService.getMatch("id", testSectionId);
        return new LabUnitPolicyView(policy.getId(), testSectionId, unit.map(this::labUnitName).orElse(testSectionId),
                unit.map(TestSection::getDomain).orElse(null), policy.getReviewTrigger().name(),
                policy.getReviewLevels(), levels(policy), policy.isActive(),
                StaleSaveGuard.token(policy.getLastupdated()));
    }

    private ValidationPolicyView toView(ValidationPolicy policy) {
        boolean blockSelfValidation = ConfigurationProperties.getInstance()
                .isPropertyValueEqual(Property.BLOCK_SELF_VALIDATION, "true");
        return new ValidationPolicyView(policy.getReviewTrigger().name(), policy.getReviewLevels(), levels(policy),
                isEnabled(), MAX_LEVELS, blockSelfValidation, StaleSaveGuard.token(policy.getLastupdated()));
    }
}
