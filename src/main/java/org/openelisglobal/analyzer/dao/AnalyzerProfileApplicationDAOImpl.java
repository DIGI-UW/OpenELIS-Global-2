package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileApplication;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalyzerProfileApplicationDAOImpl extends BaseDAOImpl<AnalyzerProfileApplication, String>
        implements AnalyzerProfileApplicationDAO {

    public AnalyzerProfileApplicationDAOImpl() {
        super(AnalyzerProfileApplication.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerProfileApplication> findByAnalyzerId(Integer analyzerId) {
        return getAllMatchingOrdered("analyzerId", analyzerId, "appliedAt", true);
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyzerProfileApplication findLatestByAnalyzerId(Integer analyzerId) {
        List<AnalyzerProfileApplication> list = getAllMatchingOrdered("analyzerId", analyzerId, "appliedAt", true);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsBySourceProfileId(String sourceProfileId) {
        return !getAllMatching("sourceProfileId", sourceProfileId).isEmpty();
    }
}
