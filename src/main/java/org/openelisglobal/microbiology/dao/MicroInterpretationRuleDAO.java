package org.openelisglobal.microbiology.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroInterpretationRule;

public interface MicroInterpretationRuleDAO extends BaseDAO<MicroInterpretationRule, String> {
    List<MicroInterpretationRule> getAll();

    Optional<MicroInterpretationRule> findBest(String antibioticId, String organismId, String organismGroup);
}
