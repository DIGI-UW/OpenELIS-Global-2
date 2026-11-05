package org.openelisglobal.microbiology.dao;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroReportingRule;

public interface MicroReportingRuleDAO extends BaseDAO<MicroReportingRule, String> {
    List<MicroReportingRule> getAll();

    Optional<MicroReportingRule> findBest(String antibioticId, String organismId, Set<String> organismGroups,
            String sampleTypeId, String clinicalContext);
}
