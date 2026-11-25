package org.openelisglobal.validationpolicy.service;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.validationpolicy.dao.AnalysisAutoReleaseDAO;
import org.openelisglobal.validationpolicy.valueholder.AnalysisAutoRelease;
import org.openelisglobal.validationpolicy.valueholder.AnalysisAutoRelease.Rule;
import org.openelisglobal.validationpolicy.valueholder.AnalysisAutoRelease.Scope;
import org.openelisglobal.validationpolicy.valueholder.ReviewTrigger;
import org.openelisglobal.validationpolicy.valueholder.ValidationPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalysisAutoReleaseServiceImpl implements AnalysisAutoReleaseService {

    @Autowired
    private AnalysisAutoReleaseDAO autoReleaseDAO;
    @Lazy
    @Autowired
    private ValidationPolicyService validationPolicyService;
    @Lazy
    @Autowired
    private TestSectionService testSectionService;

    @Override
    @Transactional
    public void record(Collection<Analysis> analyses, String sysUserId) {
        OffsetDateTime now = OffsetDateTime.now();
        Integer releasedBy = StringUtils.isNumeric(sysUserId) ? Integer.valueOf(sysUserId) : null;
        for (Analysis analysis : analyses) {
            String labUnitId = analysis.getTestSection() == null ? null : analysis.getTestSection().getId();
            AnalysisAutoRelease release = new AnalysisAutoRelease();
            release.setAnalysisId(Integer.valueOf(analysis.getId()));
            release.setReleasedAt(now);
            release.setReleasedBy(releasedBy);
            release.setTestSectionId(StringUtils.isNumeric(labUnitId) ? Integer.valueOf(labUnitId) : null);
            Optional<ValidationPolicy> policy = validationPolicyService.effectivePolicy(labUnitId);
            if (policy.isPresent()) {
                ValidationPolicy inForce = policy.get();
                release.setScope(inForce.getTestSectionId() == null ? Scope.LAB_WIDE : Scope.LAB_UNIT);
                release.setReviewTrigger(inForce.getReviewTrigger());
                release.setReviewLevels(inForce.getReviewLevels());
                release.setPolicyId(inForce.getId());
                release.setPolicyVersion(inForce.getLastupdated());
                release.setRule(inForce.getReviewTrigger() == ReviewTrigger.NONE ? Rule.NO_REVIEW : Rule.CLEAR);
            } else {
                release.setScope(Scope.SETTING);
                release.setRule(Rule.CLEAR);
            }
            release.setSysUserId(sysUserId);
            autoReleaseDAO.insert(release);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalysisAutoRelease> releasesOf(String analysisId) {
        if (!StringUtils.isNumeric(analysisId)) {
            return List.of();
        }
        return autoReleaseDAO.findByAnalysisIds(List.of(Integer.valueOf(analysisId)));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, AnalysisAutoRelease> latestOf(Collection<String> analysisIds) {
        List<Integer> ids = analysisIds.stream().filter(StringUtils::isNumeric).map(Integer::valueOf).toList();
        Map<String, AnalysisAutoRelease> latest = new LinkedHashMap<>();
        for (AnalysisAutoRelease release : autoReleaseDAO.findByAnalysisIds(ids)) {
            latest.put(String.valueOf(release.getAnalysisId()), release);
        }
        return latest;
    }

    @Override
    public boolean isCurrent(AnalysisAutoRelease release, Analysis analysis) {
        return release != null && (analysis.getReleasedDate() == null
                || release.getReleasedAt().toInstant().isAfter(analysis.getReleasedDate().toInstant()));
    }

    @Override
    @Transactional(readOnly = true)
    public String describe(AnalysisAutoRelease release) {
        String labUnit = "";
        if (release.getScope() == Scope.LAB_UNIT && release.getTestSectionId() != null) {
            labUnit = testSectionService.getMatch("id", String.valueOf(release.getTestSectionId()))
                    .map(unit -> StringUtils.defaultIfBlank(testSectionService.getUserLocalizedTesSectionName(unit),
                            StringUtils.trimToEmpty(unit.getTestSectionName())))
                    .orElse(String.valueOf(release.getTestSectionId()));
        }
        return MessageUtil.getMessage(
                "validation.autoRelease." + release.getRule().name() + "." + release.getScope().name(),
                new Object[] { labUnit });
    }
}
