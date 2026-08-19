package org.openelisglobal.eqa.service;

import java.sql.Date;
import java.util.List;
import org.openelisglobal.eqa.dao.EQAAnalystCompetencyEventDAO;
import org.openelisglobal.eqa.valueholder.EQAAnalystCompetencyEvent;
import org.openelisglobal.eqa.valueholder.EQACompetencyEventType;
import org.openelisglobal.eqa.valueholder.EQADismissalCategory;
import org.openelisglobal.eqa.valueholder.EQAParticipantResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EQAAnalystCompetencyServiceImpl implements EQAAnalystCompetencyService {

    @Autowired
    private EQAAnalystCompetencyEventDAO competencyEventDAO;

    @Override
    public EQAAnalystCompetencyEvent record(EQAParticipantResult result, EQACompetencyEventType type, Integer nceId,
            EQADismissalCategory category, String notes, String sysUserId) {
        if (result.getAssignedAnalystId() == null) {
            return null;
        }
        EQAAnalystCompetencyEvent event = new EQAAnalystCompetencyEvent();
        event.setAnalystId(result.getAssignedAnalystId());
        event.setEventType(type);
        event.setEventDate(new Date(System.currentTimeMillis()));
        event.setScheme(result.getCycle() == null ? null : result.getCycle().getScheme());
        event.setCycleId(result.getCycle() == null ? null : result.getCycle().getId());
        event.setParticipantResultId(result.getId());
        event.setAnalyteId(result.getAnalyteId());
        event.setNceId(nceId);
        event.setDismissalCategory(category);
        event.setNotes(notes);
        event.setSysUserId(sysUserId);
        event.setId(competencyEventDAO.insert(event));
        return event;
    }

    @Override
    public void attachNce(Long participantResultId, Integer nceId) {
        List<EQAAnalystCompetencyEvent> events = competencyEventDAO.getAllMatching("participantResultId",
                participantResultId);
        for (EQAAnalystCompetencyEvent event : events) {
            if (event.getNceId() == null && (event.getEventType() == EQACompetencyEventType.UNACCEPTABLE_SCORE
                    || event.getEventType() == EQACompetencyEventType.QUESTIONABLE_SCORE)) {
                event.setNceId(nceId);
                competencyEventDAO.update(event);
            }
        }
    }
}
