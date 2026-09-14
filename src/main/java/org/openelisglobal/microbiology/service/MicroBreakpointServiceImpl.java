package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.dao.MicroBreakpointRuleDAO;
import org.openelisglobal.microbiology.dao.MicroBreakpointStandardDAO;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroBreakpointServiceImpl implements MicroBreakpointService {

    private final MicroBreakpointStandardDAO standardDAO;
    private final MicroBreakpointRuleDAO ruleDAO;

    public MicroBreakpointServiceImpl(MicroBreakpointStandardDAO standardDAO, MicroBreakpointRuleDAO ruleDAO) {
        this.standardDAO = standardDAO;
        this.ruleDAO = ruleDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public MicroBreakpointStandard getActiveStandard(String authority, String version) {
        return standardDAO.getActiveStandard(authority, version);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroBreakpointStandard> getActiveStandards() {
        return standardDAO.getActiveStandards();
    }

    @Override
    @Transactional(readOnly = true)
    public MicroBreakpointStandard getStandard(String standardId) {
        return standardDAO.get(standardId).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public MicroBreakpointRule findBreakpointRule(String standardId, String organismId, String organismGroup,
            String antibioticId, String method, String specimenTypeId, String breakpointType) {
        return ruleDAO.findBestRule(standardId, organismId, organismGroup, antibioticId, method, specimenTypeId,
                breakpointType);
    }
}
