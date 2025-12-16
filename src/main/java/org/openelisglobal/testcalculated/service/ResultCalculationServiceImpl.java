package org.openelisglobal.testcalculated.service;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testcalculated.dao.ResultCalculationDAO;
import org.openelisglobal.testcalculated.valueholder.Calculation;
import org.openelisglobal.testcalculated.valueholder.ResultCalculation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ResultCalculationServiceImpl extends AuditableBaseObjectServiceImpl<ResultCalculation, Integer>
        implements ResultCalculationService {
    @Autowired
    ResultCalculationDAO resultCalculationDAO;

    public ResultCalculationServiceImpl() {
        super(ResultCalculation.class);
    }

    @Override
    protected BaseDAO<ResultCalculation, Integer> getBaseObjectDAO() {
        return resultCalculationDAO;
    }

    @Override
    public List<ResultCalculation> getResultCalculationByPatientAndTest(Patient patient, Test test) {
        return resultCalculationDAO.getResultCalculationByPatientAndTest(patient, test);
    }

    @Override
    public List<ResultCalculation> getResultCalculationByTest(Test test) {
        return resultCalculationDAO.getResultCalculationByTest(test);
    }

    @Override
    public List<ResultCalculation> getResultCalculationByPatientAndCalculation(Patient patient,
            Calculation calculation) {
        return resultCalculationDAO.getResultCalculationByPatientAndCalculation(patient, calculation);
    }
}
