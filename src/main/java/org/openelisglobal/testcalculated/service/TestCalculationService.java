package org.openelisglobal.testcalculated.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.testcalculated.valueholder.Calculation;

public interface TestCalculationService extends BaseObjectService<Calculation, Integer> {

    /**
     * Refuses a calculation that could not run safely: a name another rule already
     * holds, a rule that reads the result it writes, and, when the rule is to be
     * active, a second active rule writing the same result or a loop through other
     * active rules.
     *
     * @param willBeActive whether the rule is active once saved or switched on
     * @throws CalculationRuleViolation naming what is in the way
     */
    void validateRule(Calculation calculation, boolean willBeActive);
}
