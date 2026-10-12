package org.openelisglobal.dashboard.management.equipment;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.openelisglobal.dashboard.management.ScopeRange;
import org.openelisglobal.dashboard.management.equipment.AnalyzerActivity.Status;
import org.openelisglobal.test.service.TestSectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipmentServiceImpl implements EquipmentService {

    static final Duration ACTIVE_WITHIN = Duration.ofHours(1);
    static final Duration QUIET_WITHIN = Duration.ofHours(24);

    @Autowired
    private EquipmentDAO equipmentDAO;
    @Autowired
    private TestSectionService testSectionService;

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerActivity> getEquipment(ScopeRange scope) {
        Instant now = Instant.now();
        return equipmentDAO.getActivity(scope.from(), scope.to()).stream().map(row -> {
            Instant last = row[3] == null ? null : ((Timestamp) row[3]).toInstant();
            String sectionId = (String) row[4];
            return new AnalyzerActivity((String) row[0], (String) row[1], status(last, now),
                    ((Number) row[2]).longValue(),
                    sectionId == null ? null : testSectionService.getTestSectionById(sectionId).getLocalizedName(),
                    last == null ? null : last.toString());
        }).toList();
    }

    static Status status(Instant last, Instant now) {
        if (last == null) {
            return Status.NO_ACTIVITY;
        }
        Duration idle = Duration.between(last, now);
        if (idle.compareTo(ACTIVE_WITHIN) < 0) {
            return Status.ACTIVE;
        }
        return idle.compareTo(QUIET_WITHIN) <= 0 ? Status.QUIET : Status.NO_ACTIVITY;
    }
}
