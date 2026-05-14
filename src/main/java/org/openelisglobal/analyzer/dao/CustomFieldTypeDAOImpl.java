package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.valueholder.CustomFieldType;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class CustomFieldTypeDAOImpl extends BaseDAOImpl<CustomFieldType, String> implements CustomFieldTypeDAO {

    public CustomFieldTypeDAOImpl() {
        super(CustomFieldType.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomFieldType> findAllActive() {
        try {
            String hql = "FROM CustomFieldType WHERE isActive = true ORDER BY displayName";
            Query<CustomFieldType> query = entityManager.unwrap(Session.class).createQuery(hql, CustomFieldType.class);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding all active CustomFieldType", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CustomFieldType findByName(String name) {
        try {
            String hql = "FROM CustomFieldType WHERE displayName = :name";
            Query<CustomFieldType> query = entityManager.unwrap(Session.class).createQuery(hql, CustomFieldType.class);
            query.setParameter("name", name);
            query.setMaxResults(1);
            List<CustomFieldType> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding CustomFieldType by name", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CustomFieldType findByTypeName(String typeName) {
        try {
            String hql = "FROM CustomFieldType WHERE typeName = :typeName";
            Query<CustomFieldType> query = entityManager.unwrap(Session.class).createQuery(hql, CustomFieldType.class);
            query.setParameter("typeName", typeName);
            query.setMaxResults(1);
            List<CustomFieldType> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding CustomFieldType by type name", e);
        }
    }
}
