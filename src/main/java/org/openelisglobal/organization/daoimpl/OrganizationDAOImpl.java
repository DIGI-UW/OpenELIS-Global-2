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
package org.openelisglobal.organization.daoimpl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.organization.dao.OrganizationDAO;
import org.openelisglobal.organization.valueholder.Organization;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author diane benz
 */
@Component
@Transactional
public class OrganizationDAOImpl extends BaseDAOImpl<Organization, String> implements OrganizationDAO {

    public OrganizationDAOImpl() {
        super(Organization.class);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(Organization organization) throws LIMSRuntimeException {
        try {
            Organization org = entityManager.unwrap(Session.class).get(Organization.class, organization.getId());
            if (org != null) {
                PropertyUtils.copyProperties(organization, org);
            } else {
                // bugzilla 1366
                organization.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Organization getData()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Organization> getAllOrganizations() throws LIMSRuntimeException {
        List<Organization> list;
        try {
            String sql = "from Organization";
            Query<Organization> query = entityManager.unwrap(Session.class).createQuery(sql, Organization.class);
            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Organization getAllOrganizations()", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Organization> getPageOfOrganizations(int startingRecNo) throws LIMSRuntimeException {
        List<Organization> list;
        try {
            // calculate maxRow to be one more than the page size
            int endingRecNo = startingRecNo
                    + (Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
                            + 1);

            // bugzilla 1399
            String sql = "from Organization o order by o.organizationName";
            Query<Organization> query = entityManager.unwrap(Session.class).createQuery(sql, Organization.class);
            query.setFirstResult(startingRecNo - 1);
            query.setMaxResults(endingRecNo - 1);

            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Organization getPageOfOrganizations()", e);
        }

        return list;
    }

    // bugzilla 2372
    @Override
    @Transactional(readOnly = true)
    public List<Organization> getPagesOfSearchedOrganizations(int startingRecNo, String searchString)
            throws LIMSRuntimeException {
        List<Organization> list;
        String wildCard = "*";
        String newSearchStr;
        String sql;

        try {
            int endingRecNo = startingRecNo
                    + (Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
                            + 1);
            int wCdPosition = searchString.indexOf(wildCard);

            if (wCdPosition == -1) // no wild card looking for exact match
            {
                newSearchStr = searchString.toLowerCase().trim();
                sql = "from Organization o where trim(lower (o.organizationName)) = :param  order by"
                        + " o.organizationName";
            } else {
                newSearchStr = searchString.replace(wildCard, "%").toLowerCase().trim();
                sql = "from Organization o where trim(lower (o.organizationName)) like :param  order by"
                        + " o.organizationName";
            }
            Query<Organization> query = entityManager.unwrap(Session.class).createQuery(sql, Organization.class);
            query.setParameter("param", newSearchStr);
            query.setFirstResult(startingRecNo - 1);
            query.setMaxResults(endingRecNo - 1);

            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in OrganizationDAOImpl getPageOfSearchedOrganizations()", e);
        }

        return list;
    }
    // end bugzilla 2372

    public Organization readOrganization(String idString) {
        Organization org = null;
        try {
            org = entityManager.unwrap(Session.class).get(Organization.class, idString);
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Organization readOrganization()", e);
        }

        return org;
    }

    // this is for autocomplete
    @Override
    @Transactional(readOnly = true)
    public List<Organization> getOrganizations(String filter) throws LIMSRuntimeException {
        List<Organization> list;
        try {
            String sql = "from Organization o where upper(o.organizationName) like upper(:param) and"
                    + " o.isActive='Y' order by upper(o.organizationName)";
            Query<Organization> query = entityManager.unwrap(Session.class).createQuery(sql, Organization.class);
            query.setParameter("param", filter + "%");

            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Organization getOrganizations(String filter)", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public Organization getActiveOrganizationByName(Organization organization, boolean ignoreCase)
            throws LIMSRuntimeException {
        String sql = null;
        try {
            if (ignoreCase) {
                sql = "from Organization o where trim(lower(o.organizationName)) = :param and o.isActive='Y'";
            } else {
                sql = "from Organization o where o.organizationName = :param and o.isActive='Y'";
            }

            Query<Organization> query = entityManager.unwrap(Session.class).createQuery(sql, Organization.class);
            if (ignoreCase) {
                query.setParameter("param", organization.getOrganizationName().trim().toLowerCase());
            } else {
                query.setParameter("param", organization.getOrganizationName());
            }

            List<Organization> list = query.list();
            Organization org = null;
            if (list.size() > 0) {
                org = list.get(0);
            }

            return org;

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Organization getActiveOrganizationByName()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Organization> getActiveOrganizations() throws LIMSRuntimeException {
        String sql = null;
        try {
            sql = "from Organization o where o.isActive='Y'";

            Query<Organization> query = entityManager.unwrap(Session.class).createQuery(sql, Organization.class);
            List<Organization> list = query.list();

            return list;

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Organization getActiveOrganizations()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Organization getOrganizationByName(Organization organization, boolean ignoreCase)
            throws LIMSRuntimeException {
        String sql = null;
        try {
            if (ignoreCase) {
                sql = "from Organization o where trim(lower(o.organizationName)) = :param";
            } else {
                sql = "from Organization o where o.organizationName = :param";
            }

            Query<Organization> query = entityManager.unwrap(Session.class).createQuery(sql, Organization.class);
            if (ignoreCase) {
                query.setParameter("param", organization.getOrganizationName().trim().toLowerCase());
            } else {
                query.setParameter("param", organization.getOrganizationName());
            }

            List<Organization> list = query.list();
            Organization org = null;
            if (list.size() > 0) {
                org = list.get(0);
            }

            return org;

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Organization getOrganizationByName()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Organization getOrganizationByShortName(String shortName, boolean ignoreCase) throws LIMSRuntimeException {
        String sql = null;
        try {
            if (ignoreCase) {
                sql = "from Organization o where trim(lower(o.shortName)) = :param";
            } else {
                sql = "from Organization o where o.shortName = :param";
            }

            Query<Organization> query = entityManager.unwrap(Session.class).createQuery(sql, Organization.class);
            if (ignoreCase) {
                query.setParameter("param", shortName.trim().toLowerCase());
            } else {
                query.setParameter("param", shortName);
            }

            List<Organization> list = query.list();
            Organization org = null;
            if (list.size() > 0) {
                org = list.get(0);
            }

            return org;

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e.toString(), e);
            throw new LIMSRuntimeException("Error in Organization getOrganizationByShortName()", e);
        }
    }

    // bugzilla 2069
    @Override
    @Transactional(readOnly = true)
    public Organization getOrganizationByLocalAbbreviation(Organization organization, boolean ignoreCase)
            throws LIMSRuntimeException {
        String sql = null;
        try {
            if (ignoreCase) {
                sql = "from Organization o where trim(lower(o.organizationLocalAbbreviation)) = :param and"
                        + " o.isActive='Y'";
            } else {
                sql = "from Organization o where o.organizationLocalAbbreviation = :param and o.isActive='Y'";
            }

            Query<Organization> query = entityManager.unwrap(Session.class).createQuery(sql, Organization.class);
            if (ignoreCase) {
                query.setParameter("param", organization.getOrganizationLocalAbbreviation().trim().toLowerCase());
            } else {
                query.setParameter("param", organization.getOrganizationLocalAbbreviation());
            }

            List<Organization> list = query.list();
            Organization org = null;
            if (list.size() > 0) {
                org = list.get(0);
            }

            return org;

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Organization getOrganizationByLocalAbbreviation()", e);
        }
    }

    // bugzilla 1411
    @Override
    @Transactional(readOnly = true)
    public Integer getTotalOrganizationCount() throws LIMSRuntimeException {
        return getCount();
    }

    @Override
    public boolean duplicateOrganizationExists(Organization organization) throws LIMSRuntimeException {
        try {

            List<Organization> list = new ArrayList<>();

            // only check if the test to be inserted/updated is active
            if (organization.getIsActive().equalsIgnoreCase(IActionConstants.YES)) {
                // not case sensitive hemolysis and Hemolysis are considered
                // duplicates
                String sql = "from Organization o where ((trim(lower(o.organizationName))) = :organizationName and"
                        + " ((:parentOrgId is null and o.organization is null) or o.organization.id ="
                        + " :parentOrgId) and o.isActive='Y' and o.id != :orgId)";
                Query<Organization> query = entityManager.unwrap(Session.class).createQuery(sql, Organization.class);
                // initialize with 0 (for new records where no id has been generated yet
                String orgId = "0";
                if (!StringUtil.isNullorNill(organization.getId())) {
                    orgId = organization.getId();
                }
                if (organization.getOrganization() != null
                        && !StringUtil.isNullorNill(organization.getOrganization().getId())) {
                    query.setParameter("parentOrgId", Integer.parseInt(organization.getOrganization().getId()));
                } else {
                    // workaround so hiberate knows null is of type int...
                    query.setParameter("parentOrgId", 1);
                    query.setParameter("parentOrgId", null);
                }

                LogEvent.logDebug(this.getClass().getSimpleName(), "duplicateOrganizationExists", "org id is " + orgId);
                query.setParameter("orgId", Integer.parseInt(orgId));
                query.setParameter("organizationName", organization.getOrganizationName());

                list = query.list();
            }

            if (list.size() > 0) {
                return true;
            } else {
                return false;
            }

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in duplicateOrganizationExists()", e);
        }
    }

    // bugzilla 2372 get total searched results
    @Override
    @Transactional(readOnly = true)
    public Integer getTotalSearchedOrganizationCount(String searchString) throws LIMSRuntimeException {

        String wildCard = "*";
        String newSearchStr;
        String sql;
        Integer count = null;

        try {

            int wCdPosition = searchString.indexOf(wildCard);

            if (wCdPosition == -1) // no wild card looking for exact match
            {
                newSearchStr = searchString.toLowerCase().trim();
                sql = "select count (*) from Organization o where trim(lower (o.organizationName)) = :param ";
            } else {
                newSearchStr = searchString.replace(wildCard, "%").toLowerCase().trim();
                sql = "select count (*) from Organization o where trim(lower (o.organizationName)) like" + " :param ";
            }
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(sql, Long.class);
            query.setParameter("param", newSearchStr);

            List<Long> results = query.list();
            if (results != null && results.get(0) != null) {
                if (results.get(0) != null) {
                    count = results.get(0).intValue();
                }
            }

        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in OrganizationDAOImpl getTotalSearchedOrganizations()", e);
        }

        return count;
    }
    // end bugzilla 2372

    /**
     * @see org.openelisglobal.organization.dao.OrganizationDAO#getOrganizationsByOrgTypeName(java.lang.String,
     *      java.lang.String[])
     */
    @Override
    @Transactional(readOnly = true)
    public List<Organization> getOrganizationsByTypeName(String orderByProperty, String... typeNames) {
        try {
            String sql = "SELECT o FROM Organization o INNER JOIN o.organizationTypes ot WHERE ot.name IN (:names)"
                    + " ";
            sql += " AND o.isActive = 'Y' ";
            if (null != orderByProperty && columnNameIsInjectionSafe(orderByProperty)) {
                sql += " ORDER BY o." + orderByProperty;
            }
            Session session = entityManager.unwrap(Session.class);
            Query<Organization> query = session.createQuery(sql, Organization.class).setParameterList("names",
                    typeNames);

            List<Organization> orgs = query.list();
            return orgs;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in OrganizationType getOrganizationTypeByName()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Organization> getOrganizationsByTypeNameAndLeadingChars(String partialName, String typeName) {

        try {
            String sql = "SELECT o FROM Organization AS o INNER JOIN o.organizationTypes AS ot WHERE ot.name ="
                    + " :typeName  AND o.isActive = 'Y' AND upper(o.organizationName) like"
                    + " upper(:partialName) order by upper(o.organizationName)";
            Query<Organization> query = entityManager.unwrap(Session.class).createQuery(sql, Organization.class);
            query.setParameter("typeName", typeName);
            query.setParameter("partialName", partialName + "%");

            List<Organization> orgs = query.list();
            return orgs;
        } catch (RuntimeException e) {
            handleException(e, "getOrganizationsByTypeNameAndLeadingChars");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Organization getOrganizationById(String organizationId) throws LIMSRuntimeException {
        if (!GenericValidator.isBlankOrNull(organizationId)) {
            String sql = "from Organization o where o.id = :organizationId";

            try {
                Query<Organization> query = entityManager.unwrap(Session.class).createQuery(sql, Organization.class);
                query.setParameter("organizationId", Integer.parseInt(organizationId));
                Organization organization = query.uniqueResult();

                return organization;
            } catch (HibernateException e) {
                handleException(e, "getOrganizationById");
            }
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Organization> getOrganizationsByParentId(String parentId) throws LIMSRuntimeException {
        if (GenericValidator.isBlankOrNull(parentId)) {
            return new ArrayList<>();
        }

        String sql = "from Organization o where o.organization.id = :parentId order by o.id";

        try {
            Query<Organization> query = entityManager.unwrap(Session.class).createQuery(sql, Organization.class);
            query.setParameter("parentId", Integer.parseInt(parentId));
            List<Organization> orgs = query.list();

            return orgs;
        } catch (HibernateException e) {
            handleException(e, "getOrganizationsByParentId");
        }

        return null;
    }

    @Override
    public Organization getOrganizationByFhirId(String uuid) {
        if (GenericValidator.isBlankOrNull(uuid)) {
            return null;
        }

        String sql = "from Organization o where o.fhirUuid = :uuid";

        try {
            Query<Organization> query = entityManager.unwrap(Session.class).createQuery(sql, Organization.class);
            query.setParameter("uuid", UUID.fromString(uuid));
            List<Organization> list = query.list();
            Organization org = null;
            if (list.size() > 0) {
                org = list.get(0);
            }

            return org;
        } catch (HibernateException e) {
            handleException(e, "getOrganizationByFhirId");
        }

        return null;
    }
}
