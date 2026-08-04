package org.openelisglobal.microbiology.service;

import java.sql.Date;
import java.util.List;
import org.openelisglobal.microbiology.dao.MicroAstRunDAO;
import org.openelisglobal.microbiology.dao.MicroBreakpointActivationEventDAO;
import org.openelisglobal.microbiology.dao.MicroBreakpointRuleDAO;
import org.openelisglobal.microbiology.dao.MicroBreakpointStandardDAO;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointActivationEvent;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroBreakpointAdminServiceImpl implements MicroBreakpointAdminService {

    private final MicroBreakpointStandardDAO standardDAO;
    @SuppressWarnings("unused")
    private final MicroBreakpointRuleDAO ruleDAO;
    private final MicroBreakpointActivationEventDAO activationEventDAO;
    private final MicroAstRunDAO astRunDAO;

    public MicroBreakpointAdminServiceImpl(MicroBreakpointStandardDAO standardDAO, MicroBreakpointRuleDAO ruleDAO,
            MicroBreakpointActivationEventDAO activationEventDAO, MicroAstRunDAO astRunDAO) {
        this.standardDAO = standardDAO;
        this.ruleDAO = ruleDAO;
        this.activationEventDAO = activationEventDAO;
        this.astRunDAO = astRunDAO;
    }

    @Override
    @Transactional
    public void activate(String standardId, Date effectiveDate, String actorId) {
        requireActor(actorId);
        if (effectiveDate == null) {
            throw new IllegalArgumentException("effectiveDate is required");
        }
        MicroBreakpointStandard requested = getStandard(standardId);
        if ("ARCHIVED".equals(requested.getLifecycleStatus())) {
            throw new MicroReferenceConflictException("Archived breakpoint standards cannot be activated");
        }

        List<MicroBreakpointStandard> activeStandards = standardDAO.getActiveForAuthority(requested.getAuthority());
        for (MicroBreakpointStandard active : activeStandards) {
            if (active.getId().equals(requested.getId())) {
                continue;
            }
            active.setLifecycleStatus("LOADED");
            active.setIsActive("N");
            active.setLastUpdatedBy(actorId);
            standardDAO.update(active);
            activationEventDAO.insert(event(active.getId(), "DEACTIVATED", effectiveDate, actorId));
        }

        requested.setLifecycleStatus("ACTIVE");
        requested.setIsActive("Y");
        requested.setEffectiveDate(effectiveDate);
        requested.setLastUpdatedBy(actorId);
        standardDAO.update(requested);
        activationEventDAO.insert(event(requested.getId(), "ACTIVATED", effectiveDate, actorId));
    }

    @Override
    @Transactional
    public void archive(String standardId, String actorId) {
        requireActor(actorId);
        MicroBreakpointStandard standard = getStandard(standardId);
        if ("ACTIVE".equals(standard.getLifecycleStatus())) {
            throw new MicroReferenceConflictException("Activate a replacement before archiving this standard");
        }
        long unresolvedRuns = astRunDAO.countUnresolvedByBreakpointStandardId(standardId);
        if (unresolvedRuns > 0) {
            throw new MicroReferenceConflictException(
                    "Breakpoint standard is referenced by " + unresolvedRuns + " unresolved AST run(s)");
        }
        if ("ARCHIVED".equals(standard.getLifecycleStatus())) {
            return;
        }
        standard.setLifecycleStatus("ARCHIVED");
        standard.setIsActive("N");
        standard.setLastUpdatedBy(actorId);
        standardDAO.update(standard);
        activationEventDAO.insert(event(standardId, "ARCHIVED", standard.getEffectiveDate(), actorId));
    }

    private MicroBreakpointStandard getStandard(String standardId) {
        if (standardId == null || standardId.isBlank()) {
            throw new IllegalArgumentException("standardId is required");
        }
        return standardDAO.get(standardId)
                .orElseThrow(() -> new IllegalArgumentException("Breakpoint standard not found: " + standardId));
    }

    private MicroBreakpointActivationEvent event(String standardId, String action, Date effectiveDate, String actorId) {
        MicroBreakpointActivationEvent event = new MicroBreakpointActivationEvent();
        event.setStandardId(standardId);
        event.setAction(action);
        event.setEffectiveDate(effectiveDate);
        event.setActorId(actorId);
        return event;
    }

    private void requireActor(String actorId) {
        if (actorId == null || actorId.isBlank()) {
            throw new IllegalArgumentException("authenticated actor is required");
        }
    }
}
