package org.openelisglobal.storage.dao;

import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class StorageRoomDAOImpl extends BaseDAOImpl<StorageRoom, Integer> implements StorageRoomDAO {

    public StorageRoomDAOImpl() {
        super(StorageRoom.class);
    }

    @Override
    @Transactional(readOnly = true)
    public StorageRoom findByCode(String code) {
        try {
            String hql = "FROM StorageRoom WHERE code = :code";
            var query = entityManager.createQuery(hql, StorageRoom.class);
            query.setParameter("code", code);
            query.setMaxResults(1); // Ensure only one result is returned
            List<StorageRoom> results = query.getResultList();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding StorageRoom by code", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StorageRoom findByName(String name) {
        try {
            String hql = "FROM StorageRoom WHERE name = :name";
            var query = entityManager.createQuery(hql, StorageRoom.class);
            query.setParameter("name", name);
            query.setMaxResults(1);
            List<StorageRoom> results = query.getResultList();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding StorageRoom by name", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StorageRoom findByNameAndDepartmentTestSectionId(String name, Integer departmentTestSectionId) {
        try {
            String sql;
            if (departmentTestSectionId == null) {
                sql = """
                        SELECT id
                        FROM clinlims.storage_room
                        WHERE name = :name
                          AND department_test_section_id IS NULL
                        ORDER BY id
                        LIMIT 1
                        """;
            } else {
                sql = """
                        SELECT id
                        FROM clinlims.storage_room
                        WHERE name = :name
                          AND department_test_section_id = :departmentTestSectionId
                        ORDER BY id
                        LIMIT 1
                        """;
            }
            var query = entityManager.createNativeQuery(sql);
            query.setParameter("name", name);
            if (departmentTestSectionId != null) {
                query.setParameter("departmentTestSectionId", departmentTestSectionId);
            }
            List<?> results = query.getResultList();
            if (results.isEmpty() || results.get(0) == null) {
                return null;
            }
            Number id = (Number) results.get(0);
            return get(id.intValue()).orElse(null);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding StorageRoom by name and department", e);
        }
    }
}
