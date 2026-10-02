package org.openelisglobal.organization.locations;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.organization.locations.LocationsApi.Usage;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * OGC-1363 (Dependency 10): how many orders reference each organization,
 * department or sampling site, split into open (not finished) and total. The
 * referring site and its department both sit in sample_requester as
 * organization requesters; the older sample_organization link is counted too; a
 * sampling site is the collection location of a sample item.
 */
@Repository
public class LocationsUsageDAO {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public Map<String, Usage> orderCountsForOrganizations(Collection<String> organizationIds,
            Set<String> closedStatusIds) {
        Map<String, int[]> counts = new HashMap<>();
        if (organizationIds == null || organizationIds.isEmpty()) {
            return new HashMap<>();
        }
        List<String> ids = new ArrayList<>(organizationIds);
        List<String> closed = closedStatusIds == null || closedStatusIds.isEmpty() ? List.of("-1")
                : new ArrayList<>(closedStatusIds);
        merge(counts,
                entityManager.createNativeQuery("select cast(r.requester_id as text), count(*),"
                        + " sum(case when cast(s.status_id as text) in (:closed) then 0 else 1 end)"
                        + " from clinlims.sample_requester r join clinlims.sample s on s.id = r.sample_id"
                        + " where r.requester_type_id = 1 and cast(r.requester_id as text) in (:ids)"
                        + " group by cast(r.requester_id as text)"),
                ids, closed);
        merge(counts,
                entityManager.createNativeQuery("select cast(so.org_id as text), count(*),"
                        + " sum(case when cast(s.status_id as text) in (:closed) then 0 else 1 end)"
                        + " from clinlims.sample_organization so join clinlims.sample s on s.id = so.samp_id"
                        + " where cast(so.org_id as text) in (:ids)" + " group by cast(so.org_id as text)"),
                ids, closed);
        return toUsage(counts);
    }

    @Transactional(readOnly = true)
    public Map<String, Usage> orderCountsForSites(Collection<String> siteIds, Set<String> closedStatusIds) {
        Map<String, int[]> counts = new HashMap<>();
        if (siteIds == null || siteIds.isEmpty()) {
            return new HashMap<>();
        }
        List<String> closed = closedStatusIds == null || closedStatusIds.isEmpty() ? List.of("-1")
                : new ArrayList<>(closedStatusIds);
        merge(counts,
                entityManager.createNativeQuery("select cast(si.collection_location_id as text), count(*),"
                        + " sum(case when cast(s.status_id as text) in (:closed) then 0 else 1 end)"
                        + " from clinlims.sample_item si join clinlims.sample s on s.id = si.samp_id"
                        + " where cast(si.collection_location_id as text) in (:ids)"
                        + " group by cast(si.collection_location_id as text)"),
                new ArrayList<>(siteIds), closed);
        return toUsage(counts);
    }

    private static void merge(Map<String, int[]> counts, Query query, List<String> ids, List<String> closed) {
        query.setParameter("ids", ids);
        query.setParameter("closed", closed);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        for (Object[] row : rows) {
            String id = String.valueOf(row[0]);
            int total = ((Number) row[1]).intValue();
            int open = row[2] == null ? 0 : ((Number) row[2]).intValue();
            int[] current = counts.computeIfAbsent(id, k -> new int[2]);
            current[0] += open;
            current[1] += total;
        }
    }

    private static Map<String, Usage> toUsage(Map<String, int[]> counts) {
        Map<String, Usage> usage = new HashMap<>();
        counts.forEach((id, pair) -> usage.put(id, new Usage(pair[0], pair[1])));
        return usage;
    }
}
