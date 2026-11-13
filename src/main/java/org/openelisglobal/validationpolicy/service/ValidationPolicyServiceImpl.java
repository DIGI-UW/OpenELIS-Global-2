package org.openelisglobal.validationpolicy.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.openelisglobal.validationpolicy.dao.ValidationPolicyDAO;
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

    @Autowired
    private ValidationPolicyDAO validationPolicyDAO;
    @Lazy
    @Autowired
    private RoleService roleService;
    @Lazy
    @Autowired
    private SiteInformationService siteInformationService;

    @Override
    @Transactional(readOnly = true)
    public ValidationPolicyView getLabWideDefault() {
        return toView(validationPolicyDAO.findLabWideDefault().orElseGet(this::fromValidateAllResults));
    }

    @Override
    @Transactional
    public ValidationPolicyView saveLabWideDefault(ValidationPolicyForm form, String sysUserId) {
        ReviewTrigger trigger = parseTrigger(form.trigger());
        int levels = checkedLevels(trigger, form.levels());
        Map<Integer, Integer> roles = levelRoles(levels, form.roleIds());
        ValidationPolicy policy = validationPolicyDAO.findLabWideDefault().orElseGet(ValidationPolicy::new);
        policy.setReviewTrigger(trigger);
        policy.setReviewLevels(levels);
        policy.setActive(true);
        policy.getLevelRoleIds().clear();
        policy.getLevelRoleIds().putAll(roles);
        policy.setSysUserId(sysUserId);
        policy.setChangedBy(StringUtils.isNumeric(sysUserId) ? Integer.valueOf(sysUserId) : null);
        if (policy.getId() == null) {
            policy.setId(validationPolicyDAO.insert(policy));
        } else {
            validationPolicyDAO.update(policy);
        }
        writeValidateAllResults(trigger == ReviewTrigger.ALL, sysUserId);
        return toView(policy);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReviewTrigger> entryReviewTrigger() {
        if (!isEnabled()) {
            return Optional.empty();
        }
        return Optional.of(
                validationPolicyDAO.findLabWideDefault().orElseGet(this::fromValidateAllResults).getReviewTrigger());
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
        Map<Integer, Integer> roles = new HashMap<>();
        for (int level = 1; level <= levels; level++) {
            Integer roleId = roleIds == null || roleIds.size() < level || roleIds.get(level - 1) == null
                    ? validationRoleId
                    : roleIds.get(level - 1);
            if (!roleId.equals(validationRoleId)) {
                throw new ValidationPolicyRefusedException("roleIds", "roleNotAvailable");
            }
            roles.put(level, roleId);
        }
        return roles;
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

    private ValidationPolicyView toView(ValidationPolicy policy) {
        List<ValidationPolicyView.Level> levels = new ArrayList<>();
        for (int level = 1; level <= policy.getReviewLevels(); level++) {
            Integer roleId = policy.getLevelRoleIds().get(level);
            Role role = roleId == null ? null : roleService.getRoleById(roleId.toString());
            levels.add(new ValidationPolicyView.Level(level, roleId,
                    role == null ? null : StringUtils.trim(role.getName())));
        }
        boolean blockSelfValidation = ConfigurationProperties.getInstance()
                .isPropertyValueEqual(Property.BLOCK_SELF_VALIDATION, "true");
        return new ValidationPolicyView(policy.getReviewTrigger().name(), policy.getReviewLevels(), levels, isEnabled(),
                MAX_LEVELS, blockSelfValidation);
    }
}
