package org.openelisglobal.qaevent.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.qaevent.valueholder.NceHistory;
import org.springframework.security.access.prepost.PreAuthorize;

public interface NceHistoryService extends BaseObjectService<NceHistory, Integer> {

    /**
     * Find all history entries for a given NCE.
     *
     * @param nceId the NCE ID
     * @return list of history entries ordered by timestamp descending
     */
    @PreAuthorize("hasAuthority('PRIV_NCE_VIEW')")
    List<NceHistory> findByNceId(Integer nceId);

    /**
     * Log a history entry for an NCE.
     *
     * @param nceId       the NCE ID
     * @param activity    the activity type (e.g., "CREATED", "STATUS_CHANGED",
     *                    "UPDATED")
     * @param description detailed description of the change
     * @param oldValue    previous value (if applicable)
     * @param newValue    new value (if applicable)
     * @param userId      the user who made the change
     * @return the created history entry
     */
    @PreAuthorize("hasAuthority('PRIV_NCE_CREATE')")
    NceHistory logActivity(Integer nceId, String activity, String description, String oldValue, String newValue,
            Integer userId);
}
