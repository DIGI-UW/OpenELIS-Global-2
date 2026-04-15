package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.valueholder.UnitMapping;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for UnitMapping. Entity uses JPA annotations.
 */
@Component
@Transactional
public class UnitMappingDAOImpl extends BaseDAOImpl<UnitMapping, String> implements UnitMappingDAO {

    public UnitMappingDAOImpl() {
        super(UnitMapping.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnitMapping> findByAnalyzerFieldId(String analyzerFieldId) {
        try {
            String hql = "FROM UnitMapping u WHERE u.analyzerFieldId = :analyzerFieldId";
            Query<UnitMapping> query = entityManager.unwrap(Session.class).createQuery(hql, UnitMapping.class);
            query.setParameter("analyzerFieldId", analyzerFieldId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding UnitMapping by analyzer field ID", e);
        }
    }

    @Override
    public String insert(UnitMapping mapping) {
        // @PrePersist in entity handles UUID and lastupdated
        return super.insert(mapping);
    }
}
