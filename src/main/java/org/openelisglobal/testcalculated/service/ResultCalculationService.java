package org.openelisglobal.testcalculated.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testcalculated.valueholder.Calculation;
import org.openelisglobal.testcalculated.valueholder.ResultCalculation;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ResultCalculationService extends BaseObjectService<ResultCalculation, Integer> {

    @PreAuthorize("hasAuthority('PRIV_TESTCALC_VIEW')")
    List<ResultCalculation> getResultCalculationByPatientAndTest(Patient patient, Test test);

    @PreAuthorize("hasAuthority('PRIV_TESTCALC_VIEW')")
    List<ResultCalculation> getResultCalculationByTest(Test test);

    @PreAuthorize("hasAuthority('PRIV_TESTCALC_VIEW')")
    List<ResultCalculation> getResultCalculationByPatientAndCalculation(Patient patient, Calculation calculation);
}
