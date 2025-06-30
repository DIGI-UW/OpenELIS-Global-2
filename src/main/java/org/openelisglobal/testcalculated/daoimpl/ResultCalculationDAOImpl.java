package org.openelisglobal.testcalculated.daoimpl;

import jakarta.transaction.Transactional;
import java.util.Collections;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testcalculated.dao.ResultCalculationDAO;
import org.openelisglobal.testcalculated.valueholder.Calculation;
import org.openelisglobal.testcalculated.valueholder.ResultCalculation;
import org.springframework.stereotype.Component;

@Component
@Transactional
public class ResultCalculationDAOImpl extends BaseDAOImpl<ResultCalculation, Integer> implements ResultCalculationDAO {

    public ResultCalculationDAOImpl() {
        super(ResultCalculation.class);
    }

    @Override
    public List<ResultCalculation> getResultCalculationByPatientAndTest(Patient patient, Test test) {
        try {

            String sql = "from ResultCalculation r WHERE r.patient.id = :patientId AND :test IN ELEMENTS(r.test)";
            Query<ResultCalculation> query = entityManager.unwrap(Session.class).createQuery(sql,
                    ResultCalculation.class);
            query.setParameter("patientId", Integer.parseInt(patient.getId()));
            query.setParameter("test", test);

            List<ResultCalculation> results = query.list();
            if (results.size() > 0) {
                return results;
            }
        } catch (RuntimeException e) {
            handleException(e, "getResultCalculationByPatientAndTest");
        }
        return Collections.emptyList();
    }

    @Override
    public List<ResultCalculation> getResultCalculationByTest(Test test) {
        try {

            String sql = "from ResultCalculation r JOIN r.test t WHERE t.id = :testId";
            Query<ResultCalculation> query = entityManager.unwrap(Session.class).createQuery(sql,
                    ResultCalculation.class);
            query.setParameter("testId", Integer.parseInt(test.getId()));

            List<ResultCalculation> results = query.list();
            if (results.size() > 0) {
                return results;
            }
        } catch (RuntimeException e) {
            handleException(e, "getResultCalculationByPatientAndTest");
        }
        return Collections.emptyList();
    }

    @Override
    public List<ResultCalculation> getResultCalculationByPatientAndCalculation(Patient patient,
            Calculation calculation) {
        try {

            String sql = "from ResultCalculation r WHERE r.patient.id = :patientId AND r.calculation.id ="
                    + " :calculationId";
            Query<ResultCalculation> query = entityManager.unwrap(Session.class).createQuery(sql,
                    ResultCalculation.class);
            query.setParameter("patientId", Integer.parseInt(patient.getId()));
            query.setParameter("calculationId", calculation.getId());

            List<ResultCalculation> results = query.list();
            if (results.size() > 0) {
                return results;
            }
        } catch (RuntimeException e) {
            handleException(e, "getResultCalculationByPatientAndCalculation");
        }
        return Collections.emptyList();
    }
}
