package org.openelisglobal.dashboard.management.equipment;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.dashboard.management.ScopeRange;
import org.openelisglobal.test.service.TestSectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipmentServiceImpl implements EquipmentService {

    @Autowired
    private EquipmentDAO equipmentDAO;
    @Autowired
    private TestSectionService testSectionService;

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerActivity> getEquipment(ScopeRange scope) {
        return equipmentDAO.getActivity(scope.from(), scope.to()).stream().map(row -> {
            String sectionId = (String) row[4];
            return new AnalyzerActivity((String) row[0], (String) row[1], ((Number) row[2]).longValue(),
                    sectionId == null ? null : testSectionService.getTestSectionById(sectionId).getLocalizedName(),
                    row[3] == null ? null : ((Timestamp) row[3]).toInstant().toString());
        }).toList();
    }
}
