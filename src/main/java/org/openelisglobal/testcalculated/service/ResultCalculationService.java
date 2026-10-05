package org.openelisglobal.testcalculated.service;

import java.util.List;
import org.openelisglobal.common.security.CrudPrivileges;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testcalculated.valueholder.Calculation;
import org.openelisglobal.testcalculated.valueholder.ResultCalculation;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * The derived results themselves, written as a side effect of saving the result
 * they are calculated from.
 *
 * <p>
 * The inherited insert/update are reached from
 * {@code TestCalculatedUtil#addNewTestsToDBForCalculatedTests} on the result
 * save path, so they take the entering role's privilege rather than staying
 * open, which is what they were while this interface declared no
 * {@link CrudPrivileges}. The named reads below were gated on
 * PRIV_TESTCALC_VIEW alone, which no role is granted; one of them is called
 * from that same loop, so it would have denied Results mid-save. See
 * {@link TestCalculationService} for the definitions these are derived from,
 * and for the denial that surfaced this.
 */
@CrudPrivileges(write = "PRIV_RESULT_ENTER", read = "PRIV_RESULT_ENTER")
public interface ResultCalculationService extends BaseObjectService<ResultCalculation, Integer> {

    @PreAuthorize("hasAnyAuthority('PRIV_TESTCALC_VIEW','PRIV_RESULT_ENTER','PRIV_RESULT_VALIDATE',"
            + "'PRIV_ANALYZER_IMPORT')")
    List<ResultCalculation> getResultCalculationByPatientAndTest(Patient patient, Test test);

    @PreAuthorize("hasAnyAuthority('PRIV_TESTCALC_VIEW','PRIV_RESULT_ENTER','PRIV_RESULT_VALIDATE',"
            + "'PRIV_ANALYZER_IMPORT')")
    List<ResultCalculation> getResultCalculationByTest(Test test);

    @PreAuthorize("hasAnyAuthority('PRIV_TESTCALC_VIEW','PRIV_RESULT_ENTER','PRIV_RESULT_VALIDATE',"
            + "'PRIV_ANALYZER_IMPORT')")
    List<ResultCalculation> getResultCalculationByPatientAndCalculation(Patient patient, Calculation calculation);
}
