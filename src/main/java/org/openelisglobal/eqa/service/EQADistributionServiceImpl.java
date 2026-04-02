package org.openelisglobal.eqa.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.eqa.dao.EQADistributionDAO;
import org.openelisglobal.eqa.valueholder.EQADistribution;
import org.openelisglobal.eqa.valueholder.EQADistributionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EQADistributionServiceImpl extends BaseObjectServiceImpl<EQADistribution, Long>
        implements EQADistributionService {

    private static final int MIN_PARTICIPANTS = 2;

    @Autowired
    private EQADistributionDAO eqaDistributionDAO;

    public EQADistributionServiceImpl() {
        super(EQADistribution.class);
    }

    @Override
    protected EQADistributionDAO getBaseObjectDAO() {
        return eqaDistributionDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EQADistribution> findByProgramId(Long programId) {
        return eqaDistributionDAO.findByProgramId(programId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EQADistribution> findByStatus(EQADistributionStatus status) {
        return eqaDistributionDAO.findByStatus(status);
    }

    @Override
    public EQADistribution advanceStatus(Long distributionId) {
        EQADistribution distribution = eqaDistributionDAO.get(distributionId)
                .orElseThrow(() -> new IllegalArgumentException("Distribution not found: " + distributionId));

        EQADistributionStatus currentStatus = distribution.getStatus();
        EQADistributionStatus nextStatus;

        switch (currentStatus) {
        case DRAFT:
            nextStatus = EQADistributionStatus.PREPARED;
            break;
        case PREPARED:
            nextStatus = EQADistributionStatus.SHIPPED;
            break;
        case SHIPPED:
            nextStatus = EQADistributionStatus.COMPLETED;
            break;
        case COMPLETED:
            throw new IllegalStateException("Distribution is already completed and cannot be advanced");
        default:
            throw new IllegalStateException("Unknown distribution status: " + currentStatus);
        }

        distribution.setStatus(nextStatus);
        return eqaDistributionDAO.update(distribution);
    }

    @Override
    public void validateMinParticipants(Long distributionId, int participantCount) {
        if (participantCount < MIN_PARTICIPANTS) {
            throw new IllegalArgumentException("Distribution requires at least " + MIN_PARTICIPANTS
                    + " participants for finalization. Current: " + participantCount);
        }
    }
}
