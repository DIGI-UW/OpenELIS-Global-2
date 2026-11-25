package org.openelisglobal.reports.dataexport.dao;

import java.util.List;
import java.util.Locale;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.organization.valueholder.OrganizationType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Walks the parent links of Organization Management from every facility up,
 * with a guard against a parent loop. A type's place in the order is the
 * farthest it sits above a facility, so a district always comes before the
 * region that holds it, even where some facilities hang straight off a region.
 */
@Component
@Transactional(readOnly = true)
public class ExportLocationDAOImpl extends BaseDAOImpl<OrganizationType, String> implements ExportLocationDAO {

    private static final String AREA_TYPES = """
            WITH RECURSIVE above(area_id, depth, path) AS (
              SELECT o.org_id, 1, CAST(ARRAY[o.id] AS numeric[])
                FROM clinlims.organization o
                JOIN clinlims.organization_organization_type oot ON oot.org_id = o.id
                JOIN clinlims.organization_type t ON t.id = oot.org_type_id
               WHERE lower(t.short_name) = :facilityType AND o.org_id IS NOT NULL AND o.org_id <> o.id
              UNION ALL
              SELECT parent.org_id, above.depth + 1, above.path || parent.id
                FROM above
                JOIN clinlims.organization parent ON parent.id = above.area_id
               WHERE parent.org_id IS NOT NULL AND NOT parent.org_id = ANY(above.path || parent.id)
            )
            SELECT t.id
              FROM above
              JOIN clinlims.organization_organization_type oot ON oot.org_id = above.area_id
              JOIN clinlims.organization_type t ON t.id = oot.org_type_id
             WHERE lower(t.short_name) NOT IN (:excluded)
             GROUP BY t.id, t.short_name
             ORDER BY max(above.depth), min(above.depth), lower(t.short_name), t.id
            """;

    public ExportLocationDAOImpl() {
        super(OrganizationType.class);
    }

    @Override
    public List<String> areaTypeIdsNearestFirst(String facilityTypeName, List<String> excluded) {
        List<String> names = excluded.stream().map(name -> name.toLowerCase(Locale.ROOT)).toList();
        List<?> rows = entityManager.createNativeQuery(AREA_TYPES)
                .setParameter("facilityType", facilityTypeName.toLowerCase(Locale.ROOT))
                .setParameter("excluded", names.isEmpty() ? List.of("") : names).getResultList();
        return rows.stream().map(String::valueOf).toList();
    }
}
