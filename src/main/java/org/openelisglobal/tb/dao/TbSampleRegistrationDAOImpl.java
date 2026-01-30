package org.openelisglobal.tb.dao;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.tb.valueholder.TbSampleRegistration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Data access implementation for TB sample registrations.
 */
@Component
@Transactional
public class TbSampleRegistrationDAOImpl extends BaseDAOImpl<TbSampleRegistration, Integer>
        implements TbSampleRegistrationDAO {

    public TbSampleRegistrationDAOImpl() {
        super(TbSampleRegistration.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TbSampleRegistration> findBySampleItemId(String sampleItemId) {
        try {
            String hql = "FROM TbSampleRegistration r " + "LEFT JOIN FETCH r.sampleItem "
                    + "LEFT JOIN FETCH r.registeredBy " + "WHERE r.sampleItem.id = :sampleItemId";
            Query<TbSampleRegistration> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TbSampleRegistration.class);
            query.setParameter("sampleItemId", sampleItemId);
            return query.uniqueResultOptional();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding TB registration by sample item ID: " + sampleItemId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbSampleRegistration> findByDocumentNumber(String documentNumber) {
        try {
            String hql = "FROM TbSampleRegistration r " + "LEFT JOIN FETCH r.sampleItem "
                    + "WHERE r.documentNumber = :documentNumber " + "ORDER BY r.receivedDatetime DESC";
            Query<TbSampleRegistration> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TbSampleRegistration.class);
            query.setParameter("documentNumber", documentNumber);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding TB registrations by document number: " + documentNumber, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbSampleRegistration> findByReferringFacility(String facility) {
        try {
            String hql = "FROM TbSampleRegistration r " + "LEFT JOIN FETCH r.sampleItem "
                    + "WHERE r.referringFacility = :facility " + "ORDER BY r.receivedDatetime DESC";
            Query<TbSampleRegistration> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TbSampleRegistration.class);
            query.setParameter("facility", facility);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding TB registrations by facility: " + facility, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbSampleRegistration> findByReceivedSite(String site) {
        try {
            String hql = "FROM TbSampleRegistration r " + "LEFT JOIN FETCH r.sampleItem "
                    + "WHERE r.receivedSite = :site " + "ORDER BY r.receivedDatetime DESC";
            Query<TbSampleRegistration> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TbSampleRegistration.class);
            query.setParameter("site", site);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding TB registrations by site: " + site, e);
        }
    }
}
