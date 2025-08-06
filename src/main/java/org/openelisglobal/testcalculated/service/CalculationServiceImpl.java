package org.openelisglobal.testcalculated.service;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.testcalculated.dao.CalculationDAO;
import org.openelisglobal.testcalculated.valueholder.Calculation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CalculationServiceImpl extends AuditableBaseObjectServiceImpl<Calculation, Integer>
        implements CalculationService {
    @Autowired
    CalculationDAO calculationDAO;

    public CalculationServiceImpl() {
        super(Calculation.class);
    }

    @Override
    protected BaseDAO<Calculation, Integer> getBaseObjectDAO() {
        return calculationDAO;
    }

}
