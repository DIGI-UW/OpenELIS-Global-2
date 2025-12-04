package org.openelisglobal.coldstorage.service;

import java.time.OffsetDateTime;
import java.util.List;
import org.openelisglobal.coldstorage.valueholder.FreezerThresholdProfile;
import org.openelisglobal.coldstorage.valueholder.ThresholdProfile;

public interface ThresholdProfileService {

    List<ThresholdProfile> listProfiles();

    ThresholdProfile createProfile(ThresholdProfile profile, String username);

    FreezerThresholdProfile assignProfile(Long freezerId, Long profileId, OffsetDateTime effectiveStart,
            OffsetDateTime effectiveEnd, boolean isDefault);
}
