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
package org.openelisglobal.samplehuman.daoimpl;

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
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.dao.SampleHumanDAO;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
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
public class SampleHumanDAOImpl extends BaseDAOImpl<SampleHuman, String> implements SampleHumanDAO {

    public SampleHumanDAOImpl() {
        super(SampleHuman.class);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(SampleHuman sampleHuman) throws LIMSRuntimeException {
        try {
            SampleHuman sampHuman = entityManager.unwrap(Session.class).get(SampleHuman.class, sampleHuman.getId());
            if (sampHuman != null) {
                PropertyUtils.copyProperties(sampleHuman, sampHuman);
            } else {
                sampleHuman.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SampleHuman getData()", e);
        }
    }

    public SampleHuman readSampleHuman(String idString) {
        SampleHuman sh = null;
        try {
            sh = entityManager.unwrap(Session.class).get(SampleHuman.class, idString);
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SampleHuman readSampleHuman()", e);
        }

        return sh;
    }

    @Override
    @Transactional(readOnly = true)
    public void getDataBySample(SampleHuman sampleHuman) throws LIMSRuntimeException {

        try {
            String sql = "from SampleHuman sh where samp_id = :param";
            Query<SampleHuman> query = entityManager.unwrap(Session.class).createQuery(sql, SampleHuman.class);
            query.setParameter("param", Integer.parseInt(sampleHuman.getSampleId()));
            List<SampleHuman> list = query.list();
            SampleHuman sh = null;
            if (list.size() > 0) {
                sh = list.get(0);
                PropertyUtils.copyProperties(sampleHuman, sh);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SampleHuman getDataBySample()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Patient getPatientForSample(Sample sample) throws LIMSRuntimeException {
        Patient patient = null;
        try {
            String sql = "select patient from Patient as patient, SampleHuman as sampleHuman where"
                    + " sampleHuman.patientId = patient.id and sampleHuman.sampleId = :sId";
            Query<Patient> query = entityManager.unwrap(Session.class).createQuery(sql, Patient.class);
            query.setParameter("sId", Integer.parseInt(sample.getId()));
            patient = query.uniqueResult();
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SampleHuman getPatientForSample()", e);
        }

        return patient;
    }

    @Override
    @Transactional(readOnly = true)
    public Provider getProviderForSample(Sample sample) throws LIMSRuntimeException {
        try {
            String sql = "select provider from Provider as provider, SampleHuman as sampleHuman where"
                    + " sampleHuman.providerId = provider.id and sampleHuman.sampleId = :sId";
            Query<Provider> query = entityManager.unwrap(Session.class).createQuery(sql, Provider.class);
            query.setParameter("sId", Integer.parseInt(sample.getId()));
            Provider provider = query.uniqueResult();

            return provider;
        } catch (HibernateException e) {
            handleException(e, "getProviderForSample");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sample> getSamplesForPatient(String patientID) throws LIMSRuntimeException {

        List<Sample> samples;

        try {
            String sql = "select sample from Sample as sample, SampleHuman as sampleHuman where"
                    + " sampleHuman.sampleId = sample.id and sampleHuman.patientId = :patientId order by"
                    + " sample.id";
            Query<Sample> query = entityManager.unwrap(Session.class).createQuery(sql, Sample.class);
            query.setParameter("patientId", Integer.parseInt(patientID));
            samples = query.list();
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException(e);
        }

        return samples;
    }

    @Override
    public List<Patient> getAllPatientsWithSampleEntered() {
        List<Patient> patients = new ArrayList<>();
        try {
            String sql = "select distinct patient from Patient as patient, SampleHuman as sampleHuman where"
                    + " sampleHuman.patientId = patient.id";
            Query<Patient> query = entityManager.unwrap(Session.class).createQuery(sql, Patient.class);
            patients = query.getResultList();
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SampleHuman getAllPatientsWithSampleEntered()", e);
        }

        return patients;
    }

    @Override
    public List<Patient> getAllPatientsWithSampleEnteredMissingFhirUuid() {
        List<Patient> patients = new ArrayList<>();
        try {
            String sql = "select distinct patient from Patient as patient, SampleHuman as sampleHuman where"
                    + " sampleHuman.patientId = patient.id AND patient.fhirUuid is null";
            Query<Patient> query = entityManager.unwrap(Session.class).createQuery(sql, Patient.class);
            patients = query.getResultList();
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SampleHuman getAllPatientsWithSampleEntered()", e);
        }

        return patients;
    }
}
