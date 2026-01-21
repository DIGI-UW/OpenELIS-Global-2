package org.openelisglobal.biorepository.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.biorepository.valueholder.BiorepositoryApprovedSampleType;
import org.openelisglobal.biorepository.valueholder.BiorepositoryApprovedSampleType.SampleCategory;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Repository;

/**
 * DAO implementation for BiorepositoryApprovedSampleType.
 */
@Repository
public class BiorepositoryApprovedSampleTypeDAOImpl extends BaseDAOImpl<BiorepositoryApprovedSampleType, Integer>
        implements BiorepositoryApprovedSampleTypeDAO {

    public BiorepositoryApprovedSampleTypeDAOImpl() {
        super(BiorepositoryApprovedSampleType.class);
    }

    @Override
    public List<BiorepositoryApprovedSampleType> getAllActive() {
        Session session = entityManager.unwrap(Session.class);
        Query<BiorepositoryApprovedSampleType> query = session.createQuery(
                "FROM BiorepositoryApprovedSampleType a WHERE a.isActive = true ORDER BY a.displayOrder, a.category",
                BiorepositoryApprovedSampleType.class);
        return query.list();
    }

    @Override
    public List<BiorepositoryApprovedSampleType> getByCategory(SampleCategory category) {
        Session session = entityManager.unwrap(Session.class);
        Query<BiorepositoryApprovedSampleType> query = session.createQuery(
                "FROM BiorepositoryApprovedSampleType a WHERE a.category = :category AND a.isActive = true ORDER BY a.displayOrder",
                BiorepositoryApprovedSampleType.class);
        query.setParameter("category", category);
        return query.list();
    }

    @Override
    public boolean isApprovedSampleType(String typeOfSampleId) {
        if (typeOfSampleId == null || typeOfSampleId.isEmpty()) {
            return false;
        }
        Session session = entityManager.unwrap(Session.class);
        Query<Long> query = session.createQuery(
                "SELECT COUNT(a) FROM BiorepositoryApprovedSampleType a WHERE a.typeOfSample.id = :typeOfSampleId AND a.isActive = true",
                Long.class);
        // TypeOfSample uses LIMSStringNumberUserType - ID is String in Java but NUMERIC
        // in DB
        // Must parse to Integer for the query parameter to match the DB column type
        query.setParameter("typeOfSampleId", Integer.parseInt(typeOfSampleId));
        Long count = query.uniqueResult();
        return count != null && count > 0;
    }

    @Override
    public BiorepositoryApprovedSampleType getByTypeOfSampleId(String typeOfSampleId) {
        if (typeOfSampleId == null || typeOfSampleId.isEmpty()) {
            return null;
        }
        Session session = entityManager.unwrap(Session.class);
        Query<BiorepositoryApprovedSampleType> query = session.createQuery(
                "FROM BiorepositoryApprovedSampleType a WHERE a.typeOfSample.id = :typeOfSampleId",
                BiorepositoryApprovedSampleType.class);
        // TypeOfSample uses LIMSStringNumberUserType - ID is String in Java but NUMERIC
        // in DB
        query.setParameter("typeOfSampleId", Integer.parseInt(typeOfSampleId));
        return query.uniqueResultOptional().orElse(null);
    }

    @Override
    public long countActive() {
        Session session = entityManager.unwrap(Session.class);
        Query<Long> query = session.createQuery(
                "SELECT COUNT(a) FROM BiorepositoryApprovedSampleType a WHERE a.isActive = true", Long.class);
        return query.uniqueResult();
    }
}
