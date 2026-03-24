package org.openelisglobal.unitofmeasure.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.unitofmeasure.dao.UomTypeMapDAO;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.openelisglobal.unitofmeasure.valueholder.UomTypeMap;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class UomTypeMapDAOImpl extends BaseDAOImpl<UomTypeMap, String> implements UomTypeMapDAO {

    public UomTypeMapDAOImpl() {
        super(UomTypeMap.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnitOfMeasure> getUnitOfMeasuresByType(String uomType) {
        String sql = "SELECT m.unitOfMeasure FROM UomTypeMap m WHERE m.uomType = :uomType";
        Query<UnitOfMeasure> query = entityManager.unwrap(Session.class).createQuery(sql, UnitOfMeasure.class);
        query.setParameter("uomType", uomType);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UomTypeMap> getMappingsForUom(String uomId) {
        String sql = "FROM UomTypeMap m WHERE m.unitOfMeasure.id = :uomId";
        Query<UomTypeMap> query = entityManager.unwrap(Session.class).createQuery(sql, UomTypeMap.class);
        query.setParameter("uomId", uomId);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getTypesForUom(String uomId) {
        String sql = "SELECT m.uomType FROM UomTypeMap m WHERE m.unitOfMeasure.id = :uomId";
        Query<String> query = entityManager.unwrap(Session.class).createQuery(sql, String.class);
        query.setParameter("uomId", uomId);
        return query.list();
    }
}
