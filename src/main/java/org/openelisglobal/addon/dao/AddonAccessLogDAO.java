package org.openelisglobal.addon.dao;

import java.time.Instant;
import java.util.List;

/**
 * The access log of the add-on API ({@code addon_access_log}): one row per
 * authenticated request, whatever its answer.
 */
public interface AddonAccessLogDAO {

    record Entry(Instant at, String addonId, String credential, String userId, String actor, String purpose,
            String method, String endpoint, String subjectType, String subject, int status, Integer rows) {
    }

    void insert(Entry entry);

    /** Newest first. Every filter may be null. */
    List<Entry> find(String addonId, Instant from, Instant to, String actor, String subject, int limit);

    int purgeOlderThan(int days);
}
