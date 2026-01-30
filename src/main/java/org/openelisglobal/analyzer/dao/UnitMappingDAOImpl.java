package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.hibernate.Session;
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
            // Use native SQL to avoid HQL property name resolution issues with XML mappings
            // XML mappings define column as analyzer_field_id, but HQL resolves
            // analyzerFieldId to analyzerfieldid
            String sql = "SELECT * FROM unit_mapping WHERE analyzer_field_id = :analyzerFieldId";
            Session session = entityManager.unwrap(Session.class);
            @SuppressWarnings("unchecked")
            org.hibernate.query.NativeQuery<UnitMapping> query = (org.hibernate.query.NativeQuery<UnitMapping>) session
                    .createNativeQuery(sql).addEntity(UnitMapping.class);
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
