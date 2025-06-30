/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.siteinformation.daoimpl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.siteinformation.dao.SiteInformationDAO;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class SiteInformationDAOImpl extends BaseDAOImpl<SiteInformation, String> implements SiteInformationDAO {

    public SiteInformationDAOImpl() {
        super(SiteInformation.class);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(SiteInformation siteInformation) throws LIMSRuntimeException {
        try {
            SiteInformation tmpSiteInformation = entityManager.unwrap(Session.class).get(SiteInformation.class,
                    siteInformation.getId());
            if (tmpSiteInformation != null) {
                PropertyUtils.copyProperties(siteInformation, tmpSiteInformation);
            } else {
                siteInformation.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SiteInformation getData()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SiteInformation> getAllSiteInformation() throws LIMSRuntimeException {
        List<SiteInformation> list;
        try {
            String sql = "from SiteInformation";
            Query<SiteInformation> query = entityManager.unwrap(Session.class).createQuery(sql, SiteInformation.class);
            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SiteInformation getAllSiteInformation()", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SiteInformation> getPageOfSiteInformationByDomainName(int startingRecNo, String domainName)
            throws LIMSRuntimeException {
        List<SiteInformation> list = new ArrayList<>();
        try {

            int endingRecNo = startingRecNo
                    + (Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
                            + 1);

            String sql = "from SiteInformation si where si.domain.name = :domainName order by si.name";
            Query<SiteInformation> query = entityManager.unwrap(Session.class).createQuery(sql, SiteInformation.class);
            query.setParameter("domainName", domainName);
            query.setFirstResult(startingRecNo - 1);
            query.setMaxResults(endingRecNo - 1);

            list = query.list();
        } catch (RuntimeException e) {
            handleException(e, "getPageOfSiteInformationByDomainName");
        }

        return list;
    }

    public SiteInformation readSiteInformation(String idString) {
        SiteInformation recoveredSiteInformation;
        try {
            recoveredSiteInformation = entityManager.unwrap(Session.class).get(SiteInformation.class, idString);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SiteInformation readSiteInformation()", e);
        }

        return recoveredSiteInformation;
    }

    @Override
    @Transactional(readOnly = true)
    public SiteInformation getSiteInformationByName(String siteName) throws LIMSRuntimeException {
        String sql = "From SiteInformation si where name = :name";

        try {
            Query<SiteInformation> query = entityManager.unwrap(Session.class).createQuery(sql, SiteInformation.class);
            query.setParameter("name", siteName);
            SiteInformation information = query.uniqueResult();
            return information;
        } catch (HibernateException e) {
            handleException(e, "getSiteInformationByName");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public int getCountForDomainName(String domainName) throws LIMSRuntimeException {
        String sql = "select count(*) from SiteInformation si where si.domain.name = :domainName";

        try {
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(sql, Long.class);
            query.setParameter("domainName", domainName);
            Integer count = query.uniqueResult().intValue();
            return count;
        } catch (HibernateException e) {
            handleException(e, "getSiteInformationForDomainName");
        }
        return 0;
    }

    @Override
    @Transactional(readOnly = true)
    public SiteInformation getSiteInformationById(String id) throws LIMSRuntimeException {

        try {
            SiteInformation info = entityManager.unwrap(Session.class).get(SiteInformation.class, id);
            return info;
        } catch (HibernateException e) {
            handleException(e, "getSiteInformationById");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SiteInformation> getSiteInformationByDomainName(String domainName) {
        String sql = "From SiteInformation si where si.domain.name = :domainName";
        try {
            Query<SiteInformation> query = entityManager.unwrap(Session.class).createQuery(sql, SiteInformation.class);
            query.setParameter("domainName", domainName);
            List<SiteInformation> list = query.list();
            return list;
        } catch (HibernateException e) {
            handleException(e, "getSiteInformationByDomainName");
        }

        return null;
    }
}
