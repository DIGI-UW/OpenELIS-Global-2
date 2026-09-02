package org.openelisglobal.compliance.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.compliance.dao.ComplianceStandardDAO;
import org.openelisglobal.compliance.valueholder.ComplianceStandard;
import org.openelisglobal.compliance.valueholder.ComplianceStandard.ComplianceStandardStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of ComplianceStandardDAO following OpenELIS patterns.
 *
 * Constitutional compliance: uses @Transactional, exception-handling wrappers
 * with LogEvent, HQL queries (no native SQL), and pagination via setFirstResult
 * + setMaxResults.
 */
@Component
@Transactional
public class ComplianceStandardDAOImpl extends BaseDAOImpl<ComplianceStandard, String>
        implements ComplianceStandardDAO {

    public ComplianceStandardDAOImpl() {
        super(ComplianceStandard.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getStandardsByStatus(ComplianceStandardStatus status) throws LIMSRuntimeException {
        try {
            String hql = "FROM ComplianceStandard cs WHERE cs.status = :status "
                    + "ORDER BY cs.issuingBody, cs.regulationNumber, cs.version";
            TypedQuery<ComplianceStandard> query = entityManager.createQuery(hql, ComplianceStandard.class);
            query.setParameter("status", status);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard getStandardsByStatus()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> getPageOfStandards(int startingRecNo) throws LIMSRuntimeException {
        try {
            int pageSize = Integer
                    .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));

            // Protect against integer overflow.
            long endingRecNoLong = (long) startingRecNo + pageSize + 1;
            int endingRecNo = endingRecNoLong > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) endingRecNoLong;
            int maxResults = Math.max(1, endingRecNo - 1);

            String hql = "FROM ComplianceStandard cs ORDER BY cs.issuingBody, cs.regulationNumber, cs.version";
            TypedQuery<ComplianceStandard> query = entityManager.createQuery(hql, ComplianceStandard.class);
            query.setFirstResult(startingRecNo - 1);
            query.setMaxResults(maxResults);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard getPageOfStandards()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean duplicateStandardExists(ComplianceStandard standard) throws LIMSRuntimeException {
        try {
            // FRS Section 5 natural key: (name, regulationNumber, version).
            String hql = "SELECT COUNT(cs) FROM ComplianceStandard cs WHERE cs.name = :name "
                    + "AND cs.regulationNumber = :regulationNumber AND cs.version = :version";

            // If updating an existing standard, exclude its own id from the
            // duplicate check so a no-rename edit doesn't match itself.
            if (standard.getId() != null) {
                hql += " AND cs.id != :currentId";
            }

            TypedQuery<Long> query = entityManager.createQuery(hql, Long.class);
            query.setParameter("name", standard.getName());
            query.setParameter("regulationNumber", standard.getRegulationNumber());
            query.setParameter("version", standard.getVersion());

            if (standard.getId() != null) {
                query.setParameter("currentId", standard.getId());
            }

            return query.getSingleResult() > 0;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard duplicateStandardExists()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceStandard> searchStandards(String name, String issuingBody, String regulationNumber,
            ComplianceStandardStatus status, String countryRegion, String sampleType) throws LIMSRuntimeException {
        try {
            StringBuilder hql = new StringBuilder("FROM ComplianceStandard cs WHERE 1=1");

            if (name != null && !name.trim().isEmpty()) {
                hql.append(" AND LOWER(cs.name) LIKE :name");
            }
            if (issuingBody != null && !issuingBody.trim().isEmpty()) {
                hql.append(" AND LOWER(cs.issuingBody) LIKE :issuingBody");
            }
            if (regulationNumber != null && !regulationNumber.trim().isEmpty()) {
                hql.append(" AND LOWER(cs.regulationNumber) LIKE :regulationNumber");
            }
            if (status != null) {
                hql.append(" AND cs.status = :status");
            }
            if (countryRegion != null && !countryRegion.trim().isEmpty()) {
                hql.append(" AND LOWER(cs.countryRegion) LIKE :countryRegion");
            }
            if (sampleType != null && !sampleType.trim().isEmpty()) {
                // Backed by the compliance_standard_sample_type join table.
                // Empty-set rows count as a match so older standards without
                // declared sample types stay reachable until backfilled.
                hql.append(" AND (cs.sampleTypes IS EMPTY OR :sampleType MEMBER OF cs.sampleTypes)");
            }

            hql.append(" ORDER BY cs.issuingBody, cs.regulationNumber, cs.version");

            TypedQuery<ComplianceStandard> query = entityManager.createQuery(hql.toString(), ComplianceStandard.class);

            if (name != null && !name.trim().isEmpty()) {
                query.setParameter("name", "%" + name.toLowerCase() + "%");
            }
            if (issuingBody != null && !issuingBody.trim().isEmpty()) {
                query.setParameter("issuingBody", "%" + issuingBody.toLowerCase() + "%");
            }
            if (regulationNumber != null && !regulationNumber.trim().isEmpty()) {
                query.setParameter("regulationNumber", "%" + regulationNumber.toLowerCase() + "%");
            }
            if (status != null) {
                query.setParameter("status", status);
            }
            if (countryRegion != null && !countryRegion.trim().isEmpty()) {
                query.setParameter("countryRegion", "%" + countryRegion.toLowerCase() + "%");
            }
            if (sampleType != null && !sampleType.trim().isEmpty()) {
                // No more LIKE-wildcard - the join-table membership check
                // expects an exact match against an entry in cs.sampleTypes.
                query.setParameter("sampleType", sampleType);
            }

            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard searchStandards()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceStandard getByRegulationNumberAndName(String regulationNumber, String name)
            throws LIMSRuntimeException {
        try {
            String hql = "FROM ComplianceStandard cs WHERE cs.regulationNumber = :regulationNumber AND cs.name = :name";
            TypedQuery<ComplianceStandard> query = entityManager.createQuery(hql, ComplianceStandard.class);
            query.setParameter("regulationNumber", regulationNumber);
            query.setParameter("name", name);
            List<ComplianceStandard> results = query.getResultList();
            return results.isEmpty() ? null : results.get(0);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard getByRegulationNumberAndName()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getLinkedTests(String standardId) throws LIMSRuntimeException {
        try {
            // FR-4-002: linked tests organised by parameter group. Returns one
            // row per (group, test) so the UI can group on the frontend.
            String hql = "SELECT DISTINCT pg.id as groupId, pg.name as groupName, pg.sortOrder as groupSortOrder, "
                    + "t.id as testId, t.name as testName, t.description as testCode " + "FROM ComplianceThreshold ct "
                    + "JOIN ct.group pg " + "JOIN ct.test t " + "WHERE pg.standard.id = :standardId "
                    + "ORDER BY pg.sortOrder, pg.name, t.name";
            TypedQuery<Object[]> query = entityManager.createQuery(hql, Object[].class);
            query.setParameter("standardId", standardId);
            List<Object[]> rows = query.getResultList();
            List<Map<String, Object>> out = new java.util.ArrayList<>(rows.size());
            for (Object[] row : rows) {
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("groupId", row[0]);
                m.put("groupName", row[1]);
                m.put("groupSortOrder", row[2]);
                m.put("testId", row[3]);
                m.put("testName", row[4]);
                m.put("testCode", row[5]);
                out.add(m);
            }
            return out;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard getLinkedTests()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getDistinctCountryRegions() throws LIMSRuntimeException {
        try {
            String hql = "SELECT DISTINCT cs.countryRegion FROM ComplianceStandard cs "
                    + "WHERE cs.countryRegion IS NOT NULL AND cs.countryRegion <> '' " + "ORDER BY cs.countryRegion";
            TypedQuery<String> query = entityManager.createQuery(hql, String.class);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceStandard getDistinctCountryRegions()", e);
        }
    }
}
