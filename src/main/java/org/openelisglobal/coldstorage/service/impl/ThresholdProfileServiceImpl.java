package org.openelisglobal.coldstorage.service.impl;

import java.time.OffsetDateTime;
import java.util.List;
import org.openelisglobal.coldstorage.dao.FreezerDAO;
import org.openelisglobal.coldstorage.dao.FreezerThresholdProfileDAO;
import org.openelisglobal.coldstorage.dao.ThresholdProfileDAO;
import org.openelisglobal.coldstorage.service.ThresholdProfileService;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.coldstorage.valueholder.FreezerThresholdProfile;
import org.openelisglobal.coldstorage.valueholder.ThresholdProfile;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings("unused")
public class ThresholdProfileServiceImpl implements ThresholdProfileService {

    private final ThresholdProfileDAO thresholdProfileDAO;
    private final FreezerDAO freezerDAO;
    private final FreezerThresholdProfileDAO freezerThresholdProfileDAO;
    private final SystemUserService systemUserService;

    public ThresholdProfileServiceImpl(ThresholdProfileDAO thresholdProfileDAO, FreezerDAO freezerDAO,
            FreezerThresholdProfileDAO freezerThresholdProfileDAO, SystemUserService systemUserService) {
        this.thresholdProfileDAO = thresholdProfileDAO;
        this.freezerDAO = freezerDAO;
        this.freezerThresholdProfileDAO = freezerThresholdProfileDAO;
        this.systemUserService = systemUserService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ThresholdProfile> listProfiles() {
        return thresholdProfileDAO.getAllOrdered("name", false);
    }

    @Override
    @Transactional
    public ThresholdProfile createProfile(ThresholdProfile profile, String username) {
        if (profile.getCreatedAt() == null) {
            profile.setCreatedAt(OffsetDateTime.now());
        }

        SystemUser createdBy = systemUserService.getDataForLoginUser(username);
        if (createdBy == null) {
            createdBy = systemUserService.get("1");
            if (createdBy == null) {
                throw new IllegalStateException("System user not found. Cannot create threshold profile.");
            }
        }
        profile.setCreatedBy(createdBy);

        Long id = thresholdProfileDAO.insert(profile);
        profile.setId(id);
        return profile;
    }

    @Override
    @Transactional
    public FreezerThresholdProfile assignProfile(Long freezerId, Long profileId, OffsetDateTime effectiveStart,
            OffsetDateTime effectiveEnd, boolean isDefault) {
        Freezer freezer = freezerDAO.get(freezerId)
                .orElseThrow(() -> new IllegalArgumentException("Freezer not found: " + freezerId));
        ThresholdProfile profile = thresholdProfileDAO.get(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found: " + profileId));

        FreezerThresholdProfile assignment = new FreezerThresholdProfile();
        assignment.setFreezer(freezer);
        assignment.setThresholdProfile(profile);
        assignment.setEffectiveStart(effectiveStart != null ? effectiveStart : OffsetDateTime.now());
        assignment.setEffectiveEnd(effectiveEnd);
        assignment.setIsDefault(isDefault);

        Long id = freezerThresholdProfileDAO.insert(assignment);
        assignment.setId(id);
        return assignment;
    }
}
