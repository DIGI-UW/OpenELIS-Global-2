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
package org.openelisglobal.project.daoimpl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.project.dao.ProjectDAO;
import org.openelisglobal.project.valueholder.Project;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author diane benz
 */
@Component
@Transactional
public class ProjectDAOImpl extends BaseDAOImpl<Project, String> implements ProjectDAO {

    public ProjectDAOImpl() {
        super(Project.class);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(Project project) throws LIMSRuntimeException {
        try {
            Project proj = entityManager.unwrap(Session.class).get(Project.class, project.getId());
            if (proj != null) {
                if (proj.getStartedDate() != null) {
                    proj.setStartedDateForDisplay(DateUtil.convertSqlDateToStringDate(proj.getStartedDate()));
                }
                if (proj.getCompletedDate() != null) {
                    proj.setCompletedDateForDisplay(DateUtil.convertSqlDateToStringDate(proj.getCompletedDate()));
                }
                PropertyUtils.copyProperties(project, proj);
            } else {
                project.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Project getData()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> getAllProjects() throws LIMSRuntimeException {
        List<Project> list;
        try {
            String sql = "from Project";
            Query<Project> query = entityManager.unwrap(Session.class).createQuery(sql, Project.class);
            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Project getAllProjects()", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> getPageOfProjects(int startingRecNo) throws LIMSRuntimeException {
        List<Project> list;
        try {
            // calculate maxRow to be one more than the page size
            int endingRecNo = startingRecNo
                    + (Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
                            + 1);

            // bugzilla 1399
            // bugzilla 2438 order by local abbreviation
            String sql = "from Project p order by p.localAbbreviation desc";
            Query<Project> query = entityManager.unwrap(Session.class).createQuery(sql, Project.class);
            query.setFirstResult(startingRecNo - 1);
            query.setMaxResults(endingRecNo - 1);

            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Project getPageOfProjects()", e);
        }

        return list;
    }

    public Project readProject(String idString) {
        Project pro = null;
        try {
            pro = entityManager.unwrap(Session.class).get(Project.class, idString);
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Project readProject()", e);
        }

        return pro;
    }

    // bugzilla 1978: added param activeOnly
    @Override
    @Transactional(readOnly = true)
    public Project getProjectByName(Project project, boolean ignoreCase, boolean activeOnly)
            throws LIMSRuntimeException {
        try {
            String sql = null;
            if (activeOnly) {
                if (ignoreCase) {
                    sql = "from Project p where trim(lower(p.projectName)) = :param and p.isActive='Y'";
                } else {
                    sql = "from Project p where p.projectName = :param and p.isActive='Y'";
                }
            } else {
                if (ignoreCase) {
                    sql = "from Project p where trim(lower(p.projectName)) = :param";
                } else {
                    sql = "from Project p where p.projectName = :param";
                }
            }
            Query<Project> query = entityManager.unwrap(Session.class).createQuery(sql, Project.class);
            if (ignoreCase) {
                query.setParameter("param", project.getProjectName().toLowerCase().trim());
            } else {
                query.setParameter("param", project.getProjectName().trim());
            }

            List<Project> list = query.list();
            Project pro = null;
            if (list.size() > 0) {
                pro = list.get(0);
            }

            return pro;

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Project getProjectByName()", e);
        }
    }

    // this is for autocomplete
    // bugzilla 1978: added param activeOnly
    @Override
    @Transactional(readOnly = true)
    public List<Project> getProjects(String filter, boolean activeOnly) throws LIMSRuntimeException {
        try {
            String sql = "";
            if (activeOnly) {
                sql = "from Project p where upper(p.projectName) like upper(:param) and p.isActive='Y' order"
                        + " by upper(p.projectName)";
            } else {
                sql = "from Project p where upper(p.projectName) like upper(:param) order by" + " upper(p.projectName)";
            }
            Query<Project> query = entityManager.unwrap(Session.class).createQuery(sql, Project.class);
            query.setParameter("param", filter + "%");

            List<Project> list = query.list();
            return list;

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Project getProjects(String filter)", e);
        }
    }

    // bugzilla 1411
    @Override
    @Transactional(readOnly = true)
    public Integer getTotalProjectCount() throws LIMSRuntimeException {
        return getCount();
    }

    // overriding BaseDAOImpl bugzilla 1427 pass in name not id
    // bugzilla 1482
    @Override
    public boolean duplicateProjectExists(Project project) throws LIMSRuntimeException {
        try {

            List<Project> list = new ArrayList<>();

            // not case sensitive hemolysis and Hemolysis are considered
            // duplicates

            // bugzilla 2438 adding local abbreviation to duplicate check
            String sql = "from Project t where ((trim(lower(t.projectName)) = :param and t.id != :param2) or"
                    + " (trim(lower(t.localAbbreviation)) = :param3 and t.id != :param2))";
            Query<Project> query = entityManager.unwrap(Session.class).createQuery(sql, Project.class);
            query.setParameter("param", project.getProjectName().toLowerCase().trim());
            query.setParameter("param3", project.getLocalAbbreviation().toLowerCase().trim());

            // initialize with 0 (for new records where no id has been generated yet
            String projId = "0";
            if (!StringUtil.isNullorNill(project.getId())) {
                projId = project.getId();
            }
            query.setParameter("param2", projId);

            list = query.list();

            if (list.size() > 0) {
                return true;
            } else {
                return false;
            }

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in duplicateProjectExists()", e);
        }
    }

    // bugzilla 2438
    @Override
    @Transactional(readOnly = true)
    public Project getProjectByLocalAbbreviation(Project project, boolean activeOnly) throws LIMSRuntimeException {
        try {
            String sql = null;
            if (activeOnly) {
                sql = "from Project p where trim(lower(p.localAbbreviation)) = :param and p.isActive='Y'";
            } else {
                sql = "from Project p where trim(lower(p.localAbbreviation)) = :param";
            }
            Query<Project> query = entityManager.unwrap(Session.class).createQuery(sql, Project.class);
            query.setParameter("param", project.getLocalAbbreviation().toLowerCase().trim());
            List<Project> list = query.list();
            Project pro = null;
            if (list.size() > 0) {
                pro = list.get(0);
            }

            return pro;

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Project getProjectByLocalAbbreviation()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Project getProjectById(String id) throws LIMSRuntimeException {
        if (!GenericValidator.isBlankOrNull(id)) {
            try {
                Query<Project> query = entityManager.unwrap(Session.class)
                        .createQuery("from Project p where p.id = :id", Project.class);
                query.setParameter("id", Integer.parseInt(id));
                Project project = query.uniqueResult();

                return project;
            } catch (HibernateException e) {
                handleException(e, "getProjectById");
            }
        }
        return null;
    }
}
