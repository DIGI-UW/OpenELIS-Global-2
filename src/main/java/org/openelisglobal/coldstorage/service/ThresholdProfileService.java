package org.openelisglobal.coldstorage.service;

import java.time.OffsetDateTime;
import java.util.List;
import org.openelisglobal.coldstorage.valueholder.FreezerThresholdProfile;
import org.openelisglobal.coldstorage.valueholder.ThresholdProfile;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ThresholdProfileService {

    @PreAuthorize("hasAuthority('PRIV_COLDSTORAGE_VIEW')")
    List<ThresholdProfile> listProfiles();

    @PreAuthorize("hasAuthority('PRIV_COLDSTORAGE_MANAGE')")
    ThresholdProfile createProfile(ThresholdProfile profile, String username);

    @PreAuthorize("hasAuthority('PRIV_COLDSTORAGE_MANAGE')")
    FreezerThresholdProfile assignProfile(Long freezerId, Long profileId, OffsetDateTime effectiveStart,
            OffsetDateTime effectiveEnd, boolean isDefault);
}
