package org.openelisglobal.common.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.math.BigInteger;
import org.hibernate.HibernateException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.common.valueholder.AccessionNumberInfo;
import org.openelisglobal.common.valueholder.AccessionNumberInfo.AccessionIdentity;
import org.springframework.stereotype.Component;

@Component
public class AccessionDAOImpl implements AccessionDAO {

    @PersistenceContext
    protected EntityManager entityManager;

    @Override
    public long getNextNumberIncrement(String prefix, AccessionFormat accessionFormat) {
        try {
            String sql = "UPDATE accession_number_info" //
                    + " SET cur_val = cur_val + 1 " //
                    + " WHERE prefix = :prefix" //
                    + " AND type = :type" //
                    + " RETURNING cur_val";
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("prefix", prefix);
            query.setParameter("type", accessionFormat.name());

            return ((BigInteger) query.getSingleResult()).longValue();
        } catch (NoResultException e) {
            return 0;
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException(
                    "Error in " + this.getClass().getSimpleName() + " " + "getNextNumberIncrement", e);
        }
    }

    @Override
    public long getNextNumberNoIncrement(String prefix, AccessionFormat accessionFormat) {
        try {
            String sql = "SELECT cur_val FROM accession_number_info" //
                    + " WHERE prefix = :prefix" //
                    + " AND type = :type";
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("prefix", prefix);
            query.setParameter("type", accessionFormat.name());

            return ((BigInteger) query.getSingleResult()).longValue() + 1;
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException(
                    "Error in " + this.getClass().getSimpleName() + " " + "getNextNumberNoIncrement", e);
        }
    }

    @Override
    public AccessionNumberInfo save(AccessionNumberInfo info) {
        try {
            entityManager.persist(info);
            return entityManager.find(AccessionNumberInfo.class, info.getAccessionIdentity());
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in " + this.getClass().getSimpleName() + " " + "save", e);
        }
    }

    @Override
    public AccessionNumberInfo get(AccessionIdentity accessionIdentity) {
        try {
            return entityManager.find(AccessionNumberInfo.class, accessionIdentity);
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in " + this.getClass().getSimpleName() + " " + "get", e);
        }
    }

    @Override
    public boolean exists(AccessionIdentity accessionIdentity) {
        try {
            return entityManager.find(AccessionNumberInfo.class, accessionIdentity) != null;
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in " + this.getClass().getSimpleName() + " " + "exists", e);
        }
    }
}
