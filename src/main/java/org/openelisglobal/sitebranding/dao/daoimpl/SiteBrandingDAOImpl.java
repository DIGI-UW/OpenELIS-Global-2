package org.openelisglobal.sitebranding.dao.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.sitebranding.dao.SiteBrandingDAO;
import org.openelisglobal.sitebranding.valueholder.SiteBranding;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for SiteBranding entity
 * 
 * Task Reference: T013
 */
@Component
@Transactional
public class SiteBrandingDAOImpl extends BaseDAOImpl<SiteBranding, Integer> implements SiteBrandingDAO {

    public SiteBrandingDAOImpl() {
        super(SiteBranding.class);
    }

    @Override
    @Transactional(readOnly = true)
    public SiteBranding getBranding() {
        try {
            String hql = "FROM SiteBranding";
            Query<SiteBranding> query = entityManager.unwrap(Session.class).createQuery(hql, SiteBranding.class);
            query.setMaxResults(1); // Only one record should exist
            List<SiteBranding> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting SiteBranding", e);
        }
    }
}
