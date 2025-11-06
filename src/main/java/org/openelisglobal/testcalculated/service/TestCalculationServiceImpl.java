package org.openelisglobal.testcalculated.service;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.testcalculated.dao.TestCalculationDAO;
import org.openelisglobal.testcalculated.valueholder.Calculation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TestCalculationServiceImpl extends AuditableBaseObjectServiceImpl<Calculation, Integer>
        implements TestCalculationService {
    @Autowired
    TestCalculationDAO testCalculationDAOdao;

    public TestCalculationServiceImpl() {
        super(Calculation.class);
    }

    @Override
    protected TestCalculationDAO getBaseObjectDAO() {
        return testCalculationDAOdao;
    }
}
