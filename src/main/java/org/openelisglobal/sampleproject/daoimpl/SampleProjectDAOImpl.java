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
package org.openelisglobal.sampleproject.daoimpl;

import java.lang.reflect.InvocationTargetException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.sampleproject.dao.SampleProjectDAO;
import org.openelisglobal.sampleproject.valueholder.SampleProject;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * $Header$
 *
 * @author Hung Nguyen
 * @date created 08/04/2006
 * @version $Revision$
 */
@Component
@Transactional
public class SampleProjectDAOImpl extends BaseDAOImpl<SampleProject, String> implements SampleProjectDAO {

    public SampleProjectDAOImpl() {
        super(SampleProject.class);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(SampleProject sampleProj) throws LIMSRuntimeException {
        try {
            SampleProject data = entityManager.unwrap(Session.class).get(SampleProject.class, sampleProj.getId());
            if (data != null) {
                PropertyUtils.copyProperties(sampleProj, data);
            } else {
                sampleProj.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SampleProject getData()", e);
        }
    }

    public SampleProject readSampleProject(String idString) {
        SampleProject sp = null;
        try {
            sp = entityManager.unwrap(Session.class).get(SampleProject.class, idString);
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SampleProject readSampleProject()", e);
        }

        return sp;
    }

    // AIS - bugzilla 1851
    // Diane - bugzilla 1920
    @Override
    @Transactional(readOnly = true)
    public List<SampleProject> getSampleProjectsByProjId(String projId) throws LIMSRuntimeException {
        List<SampleProject> sampleProjects = new ArrayList<>();

        try {
            String sql = "from SampleProject sp where sp.project = :param";
            Query<SampleProject> query = entityManager.unwrap(Session.class).createQuery(sql, SampleProject.class);
            query.setParameter("param", projId);

            sampleProjects = query.list();

            return sampleProjects;

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SampleProjectDAO getSampleProjectsByProjId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SampleProject getSampleProjectBySampleId(String id) throws LIMSRuntimeException {
        List<SampleProject> sampleProjects = null;

        try {
            String sql = "from SampleProject sp where sp.sample.id = :sampleId";
            Query<SampleProject> query = entityManager.unwrap(Session.class).createQuery(sql, SampleProject.class);
            query.setParameter("sampleId", Integer.parseInt(id));

            sampleProjects = query.list();

        } catch (RuntimeException e) {
            handleException(e, "getSampleProjectBySampleId");
        }

        return (sampleProjects == null || sampleProjects.isEmpty()) ? null : sampleProjects.get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleProject> getByOrganizationProjectAndReceivedOnRange(String organizationId, String projectName,
            Date lowReceivedDate, Date highReceivedDate) throws LIMSRuntimeException {
        List<SampleProject> list = null;
        try {
            String sql = "FROM SampleProject as sp  WHERE sp.project.projectName = :projectName AND sp.sample.id"
                    + " IN (SELECT so.sample.id FROM SampleOrganization as so WHERE"
                    + " so.sample.receivedTimestamp >= :dateLow AND so.sample.receivedTimestamp <="
                    + " :dateHigh  AND   so.organization.id = :organizationId ) ";
            Query<SampleProject> query = entityManager.unwrap(Session.class).createQuery(sql, SampleProject.class);

            query.setParameter("projectName", projectName);
            query.setParameter("dateLow", lowReceivedDate);
            query.setParameter("dateHigh", highReceivedDate);
            query.setParameter("organizationId", Integer.valueOf(organizationId));
            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException(
                    "Exception occurred in SampleNumberDAOImpl.getByOrganizationProjectAndReceivedOnRange", e);
        }

        return list;
    }
}
