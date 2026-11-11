package org.openelisglobal.unitofmeasure.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.unitofmeasure.dao.UomTypeMapDAO;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.openelisglobal.unitofmeasure.valueholder.UomTypeMap;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class UomTypeMapDAOImpl extends BaseDAOImpl<UomTypeMap, Integer> implements UomTypeMapDAO {

    public UomTypeMapDAOImpl() {
        super(UomTypeMap.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnitOfMeasure> getUnitOfMeasuresByType(String uomType) {
        String jpql = "SELECT m.unitOfMeasure FROM UomTypeMap m WHERE m.uomType = :uomType";
        TypedQuery<UnitOfMeasure> query = entityManager.createQuery(jpql, UnitOfMeasure.class);
        query.setParameter("uomType", uomType);
        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UomTypeMap> getMappingsForUom(String uomId) {
        String jpql = "FROM UomTypeMap m WHERE m.unitOfMeasure.id = :uomId";
        TypedQuery<UomTypeMap> query = entityManager.createQuery(jpql, UomTypeMap.class);
        query.setParameter("uomId", uomId);
        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getTypesForUom(String uomId) {
        String jpql = "SELECT m.uomType FROM UomTypeMap m WHERE m.unitOfMeasure.id = :uomId";
        TypedQuery<String> query = entityManager.createQuery(jpql, String.class);
        query.setParameter("uomId", uomId);
        return query.getResultList();
    }
}
