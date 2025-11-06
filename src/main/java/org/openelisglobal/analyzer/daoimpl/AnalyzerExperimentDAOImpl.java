package org.openelisglobal.analyzer.daoimpl;

import org.openelisglobal.analyzer.dao.AnalyzerExperimentDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerExperiment;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;

@Component
public class AnalyzerExperimentDAOImpl extends BaseDAOImpl<AnalyzerExperiment, Integer>
        implements AnalyzerExperimentDAO {

    public AnalyzerExperimentDAOImpl() {
        super(AnalyzerExperiment.class);
    }
}
