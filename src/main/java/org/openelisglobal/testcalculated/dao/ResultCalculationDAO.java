package org.openelisglobal.testcalculated.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testcalculated.valueholder.Calculation;
import org.openelisglobal.testcalculated.valueholder.ResultCalculation;

public interface ResultCalculationDAO extends BaseDAO<ResultCalculation, Integer> {

    List<ResultCalculation> getResultCalculationByPatientAndTest(Patient patient, Test test)
            throws LIMSRuntimeException;

    List<ResultCalculation> getResultCalculationByTest(Test test) throws LIMSRuntimeException;

    List<ResultCalculation> getResultCalculationByPatientAndCalculation(Patient patient, Calculation calculation)
            throws LIMSRuntimeException;
}
