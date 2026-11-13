package org.openelisglobal.validationpolicy.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.services.IResultSaveService;
import org.openelisglobal.common.services.registration.interfaces.IResultUpdate;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.esig.service.ElectronicSignatureService;
import org.openelisglobal.esig.valueholder.ElectronicSignature;
import org.openelisglobal.esig.valueholder.SignatureMeaning;
import org.openelisglobal.microbiology.service.MicrobiologyCaseAccessService;
import org.openelisglobal.resultvalidation.service.ResultSelfValidationPolicy;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.userrole.service.UserRoleService;
import org.openelisglobal.validationpolicy.dao.AnalysisReviewPlanDAO;
import org.openelisglobal.validationpolicy.dao.AnalysisReviewSignOffDAO;
import org.openelisglobal.validationpolicy.form.ReviewState;
import org.openelisglobal.validationpolicy.valueholder.AnalysisReviewPlan;
import org.openelisglobal.validationpolicy.valueholder.AnalysisReviewSignOff;
import org.openelisglobal.validationpolicy.valueholder.AnalysisReviewSignOff.SupersededReason;
import org.openelisglobal.validationpolicy.valueholder.ValidationPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalysisReviewServiceImpl implements AnalysisReviewService {

    static final String LAB_UNIT_ROLES = "Lab Unit Roles";

    private static final String ONE_SIGNATURE_PER_LEVEL = "uq_analysis_review_sign_off_level";
    private static final String ONE_LEVEL_PER_PERSON = "uq_analysis_review_sign_off_signer";

    @Autowired
    private AnalysisReviewPlanDAO planDAO;
    @Autowired
    private AnalysisReviewSignOffDAO signOffDAO;
    @Lazy
    @Autowired
    private ValidationPolicyService validationPolicyService;
    @Lazy
    @Autowired
    private RoleService roleService;
    @Lazy
    @Autowired
    private UserRoleService userRoleService;
    @Lazy
    @Autowired
    private SystemUserService systemUserService;
    @Lazy
    @Autowired
    private MicrobiologyCaseAccessService labUnitAccess;
    @Lazy
    @Autowired
    private ResultSelfValidationPolicy selfValidationPolicy;
    @Lazy
    @Autowired
    private ElectronicSignatureService electronicSignatureService;
    @Lazy
    @Autowired
    private AnalysisService analysisService;

    @Override
    public boolean isEnabled() {
        return ConfigurationProperties.getInstance().isPropertyValueEqual(Property.USE_MULTI_LEVEL_VALIDATION, "true");
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, ReviewState> statesFor(Collection<Analysis> analyses, String sysUserId) {
        Map<String, ReviewState> states = new HashMap<>();
        if (analyses == null || analyses.isEmpty()) {
            return states;
        }
        Map<Integer, List<AnalysisReviewSignOff>> signOffs = new HashMap<>();
        signOffDAO.findByAnalysisIds(analyses.stream().map(a -> Integer.valueOf(a.getId())).distinct().toList())
                .forEach(signOff -> signOffs.computeIfAbsent(signOff.getAnalysisId(), id -> new ArrayList<>())
                        .add(signOff));
        Lookups lookups = new Lookups();
        for (Analysis analysis : analyses) {
            states.computeIfAbsent(analysis.getId(),
                    id -> state(analysis, signOffs.getOrDefault(Integer.valueOf(id), List.of()), sysUserId, lookups));
        }
        return states;
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewState stateOf(Analysis analysis, String sysUserId) {
        return state(analysis, signOffDAO.findByAnalysisIds(List.of(Integer.valueOf(analysis.getId()))), sysUserId,
                new Lookups());
    }

    @Override
    @Transactional
    public ReviewState signEarlierLevel(Analysis analysis, String sysUserId, Long signatureId) {
        ReviewState state = signable(analysis, sysUserId);
        if (state.finalLevel()) {
            throw new ReviewSignRefusedException("finalLevel");
        }
        checkSignature(signatureId, sysUserId, SignatureMeaning.REVIEWED);
        record(analysis, state, sysUserId, signatureId);
        analysis.setSysUserId(sysUserId);
        analysisService.update(analysis);
        return stateOf(analysis, sysUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public IResultUpdate finalLevelRecorder(Analysis analysis, String sysUserId, Long signatureId) {
        if (!isEnabled()) {
            return NO_STEP;
        }
        ReviewState state = signable(analysis, sysUserId);
        if (!state.finalLevel()) {
            throw new ReviewSignRefusedException("earlierLevelsUnsigned");
        }
        checkSignature(signatureId, sysUserId, SignatureMeaning.VALIDATED_AND_RELEASED);
        return new Step(() -> record(analysis, state, sysUserId, signatureId));
    }

    @Override
    public IResultUpdate restartRecorder(Analysis analysis, SupersededReason reason, String sysUserId) {
        if (!isEnabled()) {
            return NO_STEP;
        }
        return new Step(() -> supersede(analysis.getId(), reason, sysUserId));
    }

    @Override
    @Transactional
    public void resultValueChanged(String analysisId, String sysUserId) {
        if (analysisId != null && isEnabled()) {
            supersede(analysisId, SupersededReason.MODIFIED, sysUserId);
        }
    }

    private void supersede(String analysisId, SupersededReason reason, String sysUserId) {
        OffsetDateTime now = OffsetDateTime.now();
        Integer by = StringUtils.isNumeric(sysUserId) ? Integer.valueOf(sysUserId) : null;
        for (AnalysisReviewSignOff signOff : signOffDAO.findByAnalysisIds(List.of(Integer.valueOf(analysisId)))) {
            if (!signOff.isSuperseded()) {
                signOff.supersede(reason, by, now);
                signOffDAO.update(signOff);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int signerCount(String labUnitId, Integer roleId) {
        Lookups lookups = new Lookups();
        Role role = lookups.role(roleId);
        if (role == null) {
            return 0;
        }
        int signers = 0;
        for (SystemUser user : systemUserService.getAllSystemUsers()) {
            if ("Y".equals(user.getIsActive()) && canSign(user.getId(), labUnitId, role, lookups)) {
                signers++;
            }
        }
        return signers;
    }

    private boolean canSign(String userId, String labUnitId, Role role, Lookups lookups) {
        if (!covers(labUnitAccess.permittedLabUnitIds(userId, Constants.ROLE_VALIDATION), labUnitId)) {
            return false;
        }
        if (Constants.ROLE_VALIDATION.equals(roleName(role))) {
            return true;
        }
        if (Objects.equals(role.getGroupingParent(), lookups.labUnitRolesId())) {
            return covers(labUnitAccess.permittedLabUnitIds(userId, roleName(role)), labUnitId);
        }
        return userRoleService.userInRole(userId, roleName(role));
    }

    private static boolean covers(java.util.Set<String> labUnitIds, String labUnitId) {
        if (labUnitIds == null) {
            return true;
        }
        return labUnitId == null ? !labUnitIds.isEmpty() : labUnitIds.contains(labUnitId);
    }

    private ReviewState signable(Analysis analysis, String sysUserId) {
        ReviewState state = stateOf(analysis, sysUserId);
        if (state.blockedReason() != null) {
            throw new ReviewSignRefusedException(state.blockedReason());
        }
        if (selfValidationPolicy.blocked(analysis, sysUserId)) {
            throw new ReviewSignRefusedException("selfValidationBlocked");
        }
        return state;
    }

    private void record(Analysis analysis, ReviewState state, String sysUserId, Long signatureId) {
        Integer analysisId = Integer.valueOf(analysis.getId());
        long signed = signOffDAO.findByAnalysisIds(List.of(analysisId)).stream().filter(s -> !s.isSuperseded()).count();
        if (signed < state.nextLevel() - 1) {
            throw new ReviewSignRefusedException("resultChanged");
        }
        if (signed > state.nextLevel() - 1) {
            throw new ReviewSignRefusedException("levelAlreadySigned");
        }
        if (planDAO.get(analysisId).isEmpty()) {
            Plan plan = planFor(analysis);
            AnalysisReviewPlan saved = new AnalysisReviewPlan();
            saved.setId(analysisId);
            saved.setReviewLevels(plan.levels());
            saved.setCreatedAt(OffsetDateTime.now());
            saved.getLevelRoleIds().putAll(plan.roles());
            planDAO.insert(saved);
        }
        AnalysisReviewSignOff signOff = new AnalysisReviewSignOff();
        signOff.setAnalysisId(analysisId);
        signOff.setLevel(state.nextLevel());
        signOff.setReviewLevels(state.levels());
        signOff.setSignerId(Integer.valueOf(sysUserId));
        signOff.setRoleId(state.nextRoleId());
        signOff.setSignedAt(OffsetDateTime.now());
        signOff.setElectronicSignatureId(signatureId);
        try {
            signOffDAO.insert(signOff);
        } catch (RuntimeException e) {
            if (causedBy(e, ONE_SIGNATURE_PER_LEVEL)) {
                throw new ReviewSignRefusedException("levelAlreadySigned");
            }
            if (causedBy(e, ONE_LEVEL_PER_PERSON)) {
                throw new ReviewSignRefusedException("alreadySigned");
            }
            throw e;
        }
    }

    private void checkSignature(Long signatureId, String sysUserId, SignatureMeaning meaning) {
        if (signatureId == null) {
            return;
        }
        ElectronicSignature signature = electronicSignatureService.getMatch("id", signatureId)
                .orElseThrow(() -> new ReviewSignRefusedException("signatureUnknown"));
        if (!Objects.equals(String.valueOf(signature.getSignerId()), sysUserId)
                || signature.getSignatureMeaning() != meaning) {
            throw new ReviewSignRefusedException("signatureMismatch");
        }
    }

    private ReviewState state(Analysis analysis, List<AnalysisReviewSignOff> signOffs, String sysUserId,
            Lookups lookups) {
        Plan plan = planDAO.get(Integer.valueOf(analysis.getId()))
                .map(saved -> new Plan(saved.getReviewLevels(), saved.getLevelRoleIds()))
                .orElseGet(() -> planFor(analysis));
        List<AnalysisReviewSignOff> current = signOffs.stream().filter(s -> !s.isSuperseded())
                .sorted(Comparator.comparingInt(AnalysisReviewSignOff::getLevel)).toList();
        int nextLevel = Math.min(current.size() + 1, plan.levels());
        Integer roleId = plan.roles().getOrDefault(nextLevel, validationRoleId(lookups));
        Role role = lookups.role(roleId);
        String blocked = isEnabled() ? blockedReason(analysis, current, role, sysUserId, lookups) : null;
        return new ReviewState(plan.levels(), nextLevel, roleId, roleName(role),
                current.stream().map(s -> view(s, lookups)).toList(),
                signOffs.stream().filter(AnalysisReviewSignOff::isSuperseded).map(s -> view(s, lookups)).toList(),
                blocked);
    }

    private String blockedReason(Analysis analysis, List<AnalysisReviewSignOff> current, Role role, String sysUserId,
            Lookups lookups) {
        if (current.stream().anyMatch(s -> Objects.equals(String.valueOf(s.getSignerId()), sysUserId))) {
            return "alreadySigned";
        }
        String labUnitId = analysis.getTestSection() == null ? null : analysis.getTestSection().getId();
        if (!labUnitAccess.hasLabUnitRole(sysUserId, labUnitId, Constants.ROLE_VALIDATION)) {
            return "validationRightsRequired";
        }
        if (role != null && !Constants.ROLE_VALIDATION.equals(roleName(role))
                && !holdsRole(sysUserId, role, labUnitId, lookups)) {
            return "roleRequired";
        }
        return null;
    }

    private boolean holdsRole(String sysUserId, Role role, String labUnitId, Lookups lookups) {
        if (Objects.equals(role.getGroupingParent(), lookups.labUnitRolesId())) {
            return labUnitAccess.hasLabUnitRole(sysUserId, labUnitId, roleName(role));
        }
        return userRoleService.userInRole(sysUserId, roleName(role));
    }

    private Plan planFor(Analysis analysis) {
        String labUnitId = analysis.getTestSection() == null ? null : analysis.getTestSection().getId();
        Optional<ValidationPolicy> policy = validationPolicyService.effectivePolicy(labUnitId);
        if (policy.isEmpty() || policy.get().getReviewLevels() < 1) {
            return new Plan(1, Map.of());
        }
        return new Plan(policy.get().getReviewLevels(), Map.copyOf(policy.get().getLevelRoleIds()));
    }

    private ReviewState.SignOff view(AnalysisReviewSignOff signOff, Lookups lookups) {
        return new ReviewState.SignOff(signOff.getLevel(), signOff.getReviewLevels(),
                lookups.userName(signOff.getSignerId()), roleName(lookups.role(signOff.getRoleId())),
                signOff.getSignedAt() == null ? null : signOff.getSignedAt().toString(),
                signOff.getSupersededReason() == null ? null : signOff.getSupersededReason().name(),
                signOff.getSupersededAt() == null ? null : signOff.getSupersededAt().toString());
    }

    private Integer validationRoleId(Lookups lookups) {
        return lookups.validationRoleId();
    }

    private static String roleName(Role role) {
        return role == null ? null : StringUtils.trim(role.getName());
    }

    private static boolean causedBy(Throwable error, String constraint) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (String.valueOf(cause.getMessage()).contains(constraint)) {
                return true;
            }
        }
        return false;
    }

    private record Plan(int levels, Map<Integer, Integer> roles) {
    }

    private final class Lookups {
        private final Map<Integer, Role> roles = new HashMap<>();
        private final Map<Integer, String> users = new HashMap<>();
        private Integer validationRoleId;
        private String labUnitRolesId;

        Role role(Integer roleId) {
            if (roleId == null) {
                return null;
            }
            return roles.computeIfAbsent(roleId, id -> roleService.getRoleById(id.toString()));
        }

        Integer validationRoleId() {
            if (validationRoleId == null) {
                Role role = roleService.getRoleByName(Constants.ROLE_VALIDATION);
                validationRoleId = role == null ? null : Integer.valueOf(role.getId());
            }
            return validationRoleId;
        }

        String labUnitRolesId() {
            if (labUnitRolesId == null) {
                Role grouping = roleService.getRoleByName(LAB_UNIT_ROLES);
                labUnitRolesId = grouping == null ? "" : grouping.getId();
            }
            return labUnitRolesId;
        }

        String userName(Integer userId) {
            return users.computeIfAbsent(userId, id -> {
                SystemUser user = systemUserService.getMatch("id", id.toString()).orElse(null);
                return user == null ? id.toString() : StringUtils.trim(user.getNameForDisplay());
            });
        }
    }

    private static final IResultUpdate NO_STEP = new Step(() -> {
    });

    private record Step(Runnable run) implements IResultUpdate {

        @Override
        public void transactionalUpdate(IResultSaveService resultService) {
            run.run();
        }

        @Override
        public void postTransactionalCommitUpdate(IResultSaveService resultService) {
        }
    }
}
