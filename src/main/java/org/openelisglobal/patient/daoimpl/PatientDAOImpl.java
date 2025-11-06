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
package org.openelisglobal.patient.daoimpl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.patient.dao.PatientDAO;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.valueholder.Person;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author diane benz
 */
@Component
@Transactional
public class PatientDAOImpl extends BaseDAOImpl<Patient, String> implements PatientDAO {

    public PatientDAOImpl() {
        super(Patient.class);
    }

    @Override
    public Optional<Patient> get(String patientId) {
        Optional<Patient> patient = super.get(patientId);
        if (patient.isPresent()) {
            updateDisplayValues(patient.get());
        }
        return patient;
    }

    @Override
    @Transactional(readOnly = true)
    public Patient getData(String patientId) throws LIMSRuntimeException {
        try {
            Patient pat = entityManager.unwrap(Session.class).get(Patient.class, patientId);
            if (pat != null) {
                updateDisplayValues(pat);
            }
            return pat;
        } catch (HibernateException e) {
            handleException(e, "getData(patientId)");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(Patient patient) throws LIMSRuntimeException {
        try {
            Patient pat = entityManager.unwrap(Session.class).get(Patient.class, patient.getId());
            if (pat != null) {
                updateDisplayValues(pat);

                PropertyUtils.copyProperties(patient, pat);
            } else {
                patient.setId(null);
            }

        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Patient getData()", e);
        }
    }

    private void updateDisplayValues(Patient pat) {
        if (pat.getBirthDate() != null && pat.getBirthDateForDisplay() == null) {
            pat.setBirthDateForDisplay(DateUtil.convertTimestampToStringDate(pat.getBirthDate()));
        }
        if (pat.getBirthTime() != null) {
            pat.setBirthTimeForDisplay(DateUtil.convertSqlDateToStringDate(pat.getBirthTime()));
        }
        if (pat.getDeathDate() != null) {
            pat.setDeathDateForDisplay(DateUtil.convertSqlDateToStringDate(pat.getDeathDate()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Patient> getAllPatients() throws LIMSRuntimeException {
        List<Patient> list;
        try {
            String sql = "from Patient";
            Query<Patient> query = entityManager.unwrap(Session.class).createQuery(sql, Patient.class);
            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Patient getAllPatients()", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Patient> getPageOfPatients(int startingRecNo) throws LIMSRuntimeException {
        List<Patient> patients;
        try {
            // calculate maxRow to be one more than the page size
            int endingRecNo = startingRecNo
                    + (Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
                            + 1);

            String sql = "from Patient t order by t.id";
            Query<Patient> query = entityManager.unwrap(Session.class).createQuery(sql, Patient.class);
            query.setFirstResult(startingRecNo - 1);
            query.setMaxResults(endingRecNo - 1);

            patients = query.list();

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Patient getPageOfPatients()", e);
        }

        return patients;
    }

    @Override
    public Patient readPatient(String idString) {
        Patient pat = null;
        try {
            pat = entityManager.unwrap(Session.class).get(Patient.class, idString);
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Patient readPatient()", e);
        }

        return pat;
    }

    @Override
    public boolean externalIDExists(String patientExternalID) {

        List<Patient> results;

        try {
            String sql = "From Patient where external_id = :patientID";

            Query<Patient> query = entityManager.unwrap(Session.class).createQuery(sql, Patient.class);
            query.setParameter("patientID", patientExternalID);

            results = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Patient readPatient()", e);
        }

        return !results.isEmpty();
    }

    protected Patient getPatientByStringProperty(String propertyName, String propertyValue) {
        List<Patient> patients;

        try {
            String sql = "From Patient p where p." + propertyName + " = :" + propertyName;
            Query<Patient> query = entityManager.unwrap(Session.class).createQuery(sql, Patient.class);
            query.setParameter(propertyName, propertyValue);
            patients = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Patient getPatientByStringProperty(" + propertyName + "\", ) ", e);
        }
        return patients.isEmpty() ? null : patients.get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public Patient getPatientByNationalId(String nationalId) {
        return getPatientByStringProperty("nationalId", nationalId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Patient> getPatientsByNationalId(String nationalId) throws LIMSRuntimeException {
        try {
            String sql = "From Patient p where p.nationalId = :nationalId";
            Query<Patient> query = entityManager.unwrap(Session.class).createQuery(sql, Patient.class);
            query.setParameter("nationalId", nationalId);
            List<Patient> patients = query.list();
            return patients;
        } catch (RuntimeException e) {
            handleException(e, "getPatientsByNationalId");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Patient getPatientByExternalId(String externalId) {
        return getPatientByStringProperty("externalId", externalId);
    }

    @Override
    @Transactional(readOnly = true)
    public Patient getPatientByPerson(Person person) throws LIMSRuntimeException {
        List<Patient> patients;

        try {
            String sql = "From Patient p where p.person.id = :personID";

            Query<Patient> query = entityManager.unwrap(Session.class).createQuery(sql, Patient.class);
            query.setParameter("personID", Integer.parseInt(person.getId()));

            patients = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Patient getPatientByPerson()", e);
        }

        return patients.size() > 0 ? patients.get(0) : null;
    }

    @Override
    @Transactional
    public List<String> getPatientIdentityBySampleStatusIdAndProject(List<Integer> inclusiveStatusIdList,
            String project) throws LIMSRuntimeException {

        boolean useIdList = inclusiveStatusIdList != null && inclusiveStatusIdList.size() > 0;

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("select COALESCE(pat.national_id, pat.external_id) as subjectNo from clinlims.sample s ");
        sqlBuilder.append("join clinlims.sample_projects sp on sp.samp_id = s.id ");
        sqlBuilder.append("join clinlims.project p on sp.proj_id = p.id ");
        sqlBuilder.append("join clinlims.sample_human sh on sh.samp_id = s.id ");
        sqlBuilder.append("join clinlims.patient pat on pat.id = sh.patient_id  ");
        sqlBuilder.append("where p.name = :project ");
        if (useIdList) {
            sqlBuilder.append("and s.status_id in(:statusIdList) ");
        }

        try {
            NativeQuery query = entityManager.unwrap(Session.class).createNativeQuery(sqlBuilder.toString());
            if (useIdList) {
                query.setParameterList("statusIdList", inclusiveStatusIdList);
            }
            query.setParameter("project", project);

            List<String> subjectList = query.list();

            return subjectList;
        } catch (RuntimeException e) {
            handleException(e, "getPatientIdentityBySampleStatusIdAndProject");
        }

        return new ArrayList<>();
    }

    @Override
    @Transactional(readOnly = true)
    public Patient getPatientBySubjectNumber(String subjectNumber) {
        return getPatientByStringProperty("subjectNumber", subjectNumber);
    }

    @Override
    public List<Patient> getAllMissingFhirUuid() {
        String sql = "from Patient p where p.fhirUuid is NULL";
        try {
            Query<Patient> query = entityManager.unwrap(Session.class).createQuery(sql, Patient.class);
            List<Patient> patientList = query.list();

            return patientList;
        } catch (HibernateException e) {
            handleException(e, "getAllMissingFhirUuid");
        }
        return new ArrayList<>();
    }

    @Override
    public Patient getByExternalId(String id) {
        String sql = "from Patient p where p.externalId = :id";
        try {
            Query<Patient> query = entityManager.unwrap(Session.class).createQuery(sql, Patient.class);
            query.setParameter("id", id);
            return query.uniqueResult();
        } catch (HibernateException e) {
            handleException(e, "getByExternalId");
        }
        return null;
    }
}
