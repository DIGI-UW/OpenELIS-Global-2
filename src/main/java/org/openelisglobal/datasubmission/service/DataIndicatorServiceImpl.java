package org.openelisglobal.datasubmission.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.datasubmission.dao.DataIndicatorDAO;
import org.openelisglobal.datasubmission.valueholder.DataIndicator;
import org.openelisglobal.datasubmission.valueholder.TypeOfDataIndicator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataIndicatorServiceImpl extends AuditableBaseObjectServiceImpl<DataIndicator, String>
        implements DataIndicatorService {
    @Autowired
    protected DataIndicatorDAO baseObjectDAO;

    DataIndicatorServiceImpl() {
        super(DataIndicator.class);
    }

    @Override
    protected DataIndicatorDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public DataIndicator getIndicatorByTypeYearMonth(TypeOfDataIndicator type, int year, int month) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("typeOfIndicator.id", type.getId());
        properties.put("year", year);
        properties.put("month", month);
        return getMatch(properties).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DataIndicator> getIndicatorsByStatus(String status) {
        return getBaseObjectDAO().getAllMatching("status", status);
    }
}
