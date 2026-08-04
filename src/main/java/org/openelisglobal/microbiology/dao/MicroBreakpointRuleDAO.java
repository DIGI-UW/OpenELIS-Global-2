package org.openelisglobal.microbiology.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;

public interface MicroBreakpointRuleDAO extends BaseDAO<MicroBreakpointRule, String> {
    MicroBreakpointRule findBestRule(String standardId, String organismId, String organismGroup, String antibioticId,
            String method, String specimenTypeId, String breakpointType);

    Optional<MicroBreakpointRule> findBySourceRowHash(String sourceRowHash);

    Optional<MicroBreakpointRule> findByNaturalKey(String standardId, String organismId, String organismGroup,
            String antibioticId, String method, String specimenTypeId, String breakpointType);

    List<MicroBreakpointRule> getByStandardId(String standardId);
}
