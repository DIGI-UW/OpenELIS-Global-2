package org.openelisglobal.testcalculated.daoimpl;

import jakarta.transaction.Transactional;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.testcalculated.dao.CalculationDAO;
import org.openelisglobal.testcalculated.valueholder.Calculation;
import org.springframework.stereotype.Component;

@Component
@Transactional
public class CalculationDAOImpl extends BaseDAOImpl<Calculation, Integer> implements CalculationDAO {

    public CalculationDAOImpl() {
        super(Calculation.class);
    }
}
