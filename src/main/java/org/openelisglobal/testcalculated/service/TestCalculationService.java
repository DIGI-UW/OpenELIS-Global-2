package org.openelisglobal.testcalculated.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.testcalculated.valueholder.Calculation;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
public interface TestCalculationService extends BaseObjectService<Calculation, Integer> {
}
