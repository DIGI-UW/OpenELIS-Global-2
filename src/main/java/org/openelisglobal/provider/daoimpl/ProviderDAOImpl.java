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
 */
package org.openelisglobal.provider.daoimpl;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Vector;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.provider.dao.ProviderDAO;
import org.openelisglobal.provider.valueholder.Provider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author diane benz
 */
@Component
@Transactional
public class ProviderDAOImpl extends BaseDAOImpl<Provider, String> implements ProviderDAO {

    public ProviderDAOImpl() {
        super(Provider.class);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(Provider provider) throws LIMSRuntimeException {
        try {
            Provider prov = entityManager.unwrap(Session.class).get(Provider.class, provider.getId());
            if (prov != null) {
                PropertyUtils.copyProperties(provider, prov);
            } else {
                provider.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ProviderDAOImpl getData()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Provider> getAllProviders() throws LIMSRuntimeException {
        List<Provider> list = new Vector<>();
        try {
            String sql = "from Provider";
            Query<Provider> query = entityManager.unwrap(Session.class).createQuery(sql, Provider.class);
            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ProviderDAOImpl getAllProviders()", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Provider> getPageOfProviders(int startingRecNo) throws LIMSRuntimeException {
        List<Provider> list = new Vector<>();
        try {
            // calculate maxRow to be one more than the page size
            int endingRecNo = startingRecNo
                    + (Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
                            + 1);

            String sql = "from Provider p order by p.id";
            Query<Provider> query = entityManager.unwrap(Session.class).createQuery(sql, Provider.class);
            query.setFirstResult(startingRecNo - 1);
            query.setMaxResults(endingRecNo - 1);

            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ProviderDAOImpl getPageOfProviders()", e);
        }

        return list;
    }

    public Provider readProvider(String idString) {
        Provider provider = null;
        try {
            provider = entityManager.unwrap(Session.class).get(Provider.class, idString);
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ProviderDAOImpl readProvider()", e);
        }

        return provider;
    }

    @Override
    @Transactional(readOnly = true)
    public Provider getProviderByPerson(Person person) throws LIMSRuntimeException {
        List<Provider> list = null;
        try {
            String sql = "from Provider p where p.person.id = :personId";
            Query<Provider> query = entityManager.unwrap(Session.class).createQuery(sql, Provider.class);
            query.setParameter("personId", Integer.parseInt(person.getId()));

            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ProviderDAOImpl getProviderByPerson()", e);
        }

        if (list.size() > 0) {
            return list.get(0);
        } else {
            return null;
        }
    }

    @Override
    public int getTotalSearchedProviderCount(String parameter) {
        List<Provider> list = null;
        try {
            String sql = "from Provider p where lower(p.person.firstName) like concat('%', lower(:searchValue),"
                    + " '%') or lower(p.person.lastName) like concat('%', lower(:searchValue), '%') or"
                    + " lower(concat(p.person.firstName, ' ', p.person.lastName)) like concat('%',"
                    + " lower(:searchValue), '%')";
            Query<Provider> query = entityManager.unwrap(Session.class).createQuery(sql, Provider.class);
            query.setParameter("searchValue", parameter);

            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ProviderDAOImpl getTotalSearchedProviderCount()", e);
        }

        return list.size();
    }

    @Override
    public List<Provider> getPagesOfSearchedProviders(int startingRecNo, String parameter) {
        List<Provider> list = new Vector<>();
        try {
            // calculate maxRow to be one more than the page size
            int endingRecNo = startingRecNo
                    + (Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
                            + 1);

            String sql = "from Provider p where lower(p.person.firstName) like concat('%', lower(:searchValue),"
                    + " '%') or lower(p.person.lastName) like concat('%', lower(:searchValue), '%') or"
                    + " lower(concat(p.person.firstName, ' ', p.person.lastName)) like concat('%',"
                    + " lower(:searchValue), '%') ORDER BY p.active DESC, p.person.lastName";
            Query<Provider> query = entityManager.unwrap(Session.class).createQuery(sql, Provider.class);
            query.setParameter("searchValue", parameter);
            query.setFirstResult(startingRecNo - 1);
            query.setMaxResults(endingRecNo - 1);

            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ProviderDAOImpl getPagesOfSearchedProviders()", e);
        }

        return list;
    }
}
