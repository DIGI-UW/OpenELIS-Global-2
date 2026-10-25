package org.openelisglobal.sample.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.controller.UnifiedSystemUserController;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.userrole.service.UserRoleService;
import org.openelisglobal.userrole.valueholder.UserLabUnitRoles;
import org.openelisglobal.vector.service.VectorPoolService;
import org.openelisglobal.vector.valueholder.VectorPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SampleLabUnitScopeServiceImpl implements SampleLabUnitScopeService {

    @Autowired
    private UserService userService;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    @Lazy
    private VectorPoolService vectorPoolService;

    @Override
    @Transactional(readOnly = true)
    public Set<String> restrictedSectionIds(String sysUserId) {
        if (sysUserId == null || isGlobalScopeUser(sysUserId) || isLoginLabUnitConstrained()) {
            return Collections.emptySet();
        }
        List<IdValuePair> userSections = userService.getUserTestSections(sysUserId, null);
        if (userSections == null || userSections.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> ids = new HashSet<>();
        for (IdValuePair section : userSections) {
            ids.add(section.getId());
        }
        expandWithChildSections(ids);
        return ids;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isVisible(String sysUserId, Sample sample) {
        if (sample == null) {
            return false;
        }
        Set<String> allowed = restrictedSectionIds(sysUserId);
        if (allowed.isEmpty()) {
            return true;
        }
        if (sysUserId.equals(sample.getSysUserId())) {
            return true;
        }
        return belongsToSections(sample, allowed);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSampleItemVisible(String sysUserId, String sampleItemId) {
        if (sampleItemId == null || sampleItemId.isBlank()) {
            return false;
        }
        SampleItem item = sampleItemService.getMatch("id", sampleItemId).orElse(null);
        return item != null && item.getSample() != null && isVisible(sysUserId, item.getSample());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean belongsToSections(Sample sample, Set<String> sectionIds) {
        List<Analysis> analyses = new java.util.ArrayList<>();
        List<Analysis> bySampleItem = analysisService.getAnalysesBySampleId(sample.getId());
        if (bySampleItem != null) {
            analyses.addAll(bySampleItem);
        }
        for (VectorPool pool : vectorPoolService.getBySampleId(sample.getId())) {
            analyses.addAll(analysisService.getAnalysesByVectorPoolId(String.valueOf(pool.getId())));
        }
        if (analyses.isEmpty()) {
            return true;
        }
        for (Analysis analysis : analyses) {
            String sectionId = analysis.getTestSection() != null ? analysis.getTestSection().getId() : null;
            if (sectionId != null && sectionIds.contains(sectionId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Analyses are filed under child sections (e.g. "Entomology"), not the
     * domain-level parent (e.g. "Vector Surveillance") a user is assigned to.
     */
    private void expandWithChildSections(Set<String> ids) {
        List<TestSection> allSections = testSectionService.getAllActiveTestSections();
        for (TestSection section : allSections) {
            TestSection parent = section.getParentTestSection();
            if (parent != null && ids.contains(parent.getId())) {
                ids.add(section.getId());
            }
        }
    }

    /**
     * A global administrator or a user mapped to {@code AllLabUnits} already covers
     * every section; their section list is indistinguishable from an explicit
     * per-section assignment, so the roles decide.
     */
    private boolean isGlobalScopeUser(String sysUserId) {
        if (userRoleService.userInRole(sysUserId, Constants.ROLE_GLOBAL_ADMIN)) {
            return true;
        }
        UserLabUnitRoles labUnitRoles = userService.getUserLabUnitRoles(sysUserId);
        if (labUnitRoles == null || labUnitRoles.getLabUnitRoleMap() == null) {
            return false;
        }
        return labUnitRoles.getLabUnitRoleMap().stream()
                .anyMatch(roleMap -> UnifiedSystemUserController.ALL_LAB_UNITS.equals(roleMap.getLabUnit()));
    }

    /**
     * With lab unit selection required at login a non-admin's resolved section list
     * is the single unit chosen at login rather than their assignments.
     */
    private boolean isLoginLabUnitConstrained() {
        return ConfigurationProperties.getInstance().isPropertyValueEqual(Property.REQUIRE_LAB_UNIT_AT_LOGIN, "true");
    }
}
