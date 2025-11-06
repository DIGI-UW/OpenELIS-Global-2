package org.openelisglobal.testcalculated.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testcalculated.valueholder.Calculation;
import org.openelisglobal.testcalculated.valueholder.ResultCalculation;

public interface ResultCalculationService extends BaseObjectService<ResultCalculation, Integer> {

    List<ResultCalculation> getResultCalculationByPatientAndTest(Patient patient, Test test);

    List<ResultCalculation> getResultCalculationByTest(Test test);

    List<ResultCalculation> getResultCalculationByPatientAndCalculation(Patient patient, Calculation calculation);
}
