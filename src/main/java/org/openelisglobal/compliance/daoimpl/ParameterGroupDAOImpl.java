package org.openelisglobal.compliance.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.compliance.dao.ParameterGroupDAO;
import org.openelisglobal.compliance.valueholder.ParameterGroup;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of ParameterGroupDAO following OpenELIS patterns. Uses bulk
 * operations to keep N-row queries to one round-trip and proper transaction
 * boundaries.
 */
@Component
@Transactional
public class ParameterGroupDAOImpl extends BaseDAOImpl<ParameterGroup, String> implements ParameterGroupDAO {

    public ParameterGroupDAOImpl() {
        super(ParameterGroup.class);
    }

    @Override
    @Transactional(readOnly = true)
    public ParameterGroup findByStandardIdAndName(String standardId, String groupName) throws LIMSRuntimeException {
        try {
            String hql = "FROM ParameterGroup pg WHERE pg.standard.id = :standardId AND pg.name = :groupName";
            TypedQuery<ParameterGroup> query = entityManager.createQuery(hql, ParameterGroup.class);
            query.setParameter("standardId", standardId);
            query.setParameter("groupName", groupName);
            query.setMaxResults(1);
            List<ParameterGroup> results = query.getResultList();
            return results.isEmpty() ? null : results.get(0);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ParameterGroup findByStandardIdAndName()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParameterGroup> getGroupsByStandardId(String standardId) throws LIMSRuntimeException {
        try {
            String hql = "FROM ParameterGroup pg WHERE pg.standard.id = :standardId ORDER BY pg.sortOrder, pg.name";
            TypedQuery<ParameterGroup> query = entityManager.createQuery(hql, ParameterGroup.class);
            query.setParameter("standardId", standardId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ParameterGroup getGroupsByStandardId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Integer> countGroupsByStandardIds(Collection<String> standardIds) throws LIMSRuntimeException {
        if (standardIds == null || standardIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            String hql = "SELECT pg.standard.id, COUNT(pg) FROM ParameterGroup pg "
                    + "WHERE pg.standard.id IN :ids GROUP BY pg.standard.id";
            TypedQuery<Object[]> query = entityManager.createQuery(hql, Object[].class);
            query.setParameter("ids", standardIds);
            List<Object[]> rows = query.getResultList();
            Map<String, Integer> counts = new HashMap<>(rows.size());
            for (Object[] row : rows) {
                String standardId = String.valueOf(row[0]);
                int count = ((Number) row[1]).intValue();
                counts.put(standardId, count);
            }
            return counts;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ParameterGroup countGroupsByStandardIds()", e);
        }
    }
}
