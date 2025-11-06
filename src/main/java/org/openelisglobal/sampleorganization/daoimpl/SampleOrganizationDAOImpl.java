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
package org.openelisglobal.sampleorganization.daoimpl;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleorganization.dao.SampleOrganizationDAO;
import org.openelisglobal.sampleorganization.valueholder.SampleOrganization;
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
public class SampleOrganizationDAOImpl extends BaseDAOImpl<SampleOrganization, String>
        implements SampleOrganizationDAO {

    public SampleOrganizationDAOImpl() {
        super(SampleOrganization.class);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(SampleOrganization sampleOrg) throws LIMSRuntimeException {
        try {
            SampleOrganization data = entityManager.unwrap(Session.class).get(SampleOrganization.class,
                    sampleOrg.getId());
            if (data != null) {
                PropertyUtils.copyProperties(sampleOrg, data);
            } else {
                sampleOrg.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SampleOrganization getData()", e);
        }
    }

    public SampleOrganization readSampleOrganization(String idString) {
        SampleOrganization so = null;
        try {
            so = entityManager.unwrap(Session.class).get(SampleOrganization.class, idString);
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SampleOrganization readSampleOrganization()", e);
        }

        return so;
    }

    @Override
    @Transactional(readOnly = true)
    public void getDataBySample(SampleOrganization sampleOrganization) throws LIMSRuntimeException {

        try {
            String sql = "from SampleOrganization so where samp_id = :param";
            Query<SampleOrganization> query = entityManager.unwrap(Session.class).createQuery(sql,
                    SampleOrganization.class);
            query.setParameter("param", Integer.valueOf(sampleOrganization.getSample().getId()));
            List<SampleOrganization> list = query.list();
            SampleOrganization so = null;
            if (list.size() > 0) {
                so = list.get(0);
                PropertyUtils.copyProperties(sampleOrganization, so);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SampleOrganization getData()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SampleOrganization getDataBySample(Sample sample) throws LIMSRuntimeException {
        String sql = "From SampleOrganization so where so.sample.id = :sampleId";
        try {
            Query<SampleOrganization> query = entityManager.unwrap(Session.class).createQuery(sql,
                    SampleOrganization.class);
            query.setParameter("sampleId", Integer.parseInt(sample.getId()));
            List<SampleOrganization> sampleOrg = query.list();
            // There was a bug that allowed the same sample id / organization id to be
            // entered twice
            return sampleOrg.isEmpty() ? null : sampleOrg.get(0);

        } catch (HibernateException e) {
            handleException(e, "getDataBySample");
        }
        return null;
    }
}
