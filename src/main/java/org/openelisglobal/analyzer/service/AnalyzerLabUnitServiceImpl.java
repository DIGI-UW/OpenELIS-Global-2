package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.dao.AnalyzerLabUnitDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerLabUnit;
import org.openelisglobal.analyzer.valueholder.AnalyzerLabUnitId;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AnalyzerLabUnitServiceImpl extends BaseObjectServiceImpl<AnalyzerLabUnit, AnalyzerLabUnitId>
        implements AnalyzerLabUnitService {

    private final AnalyzerLabUnitDAO analyzerLabUnitDAO;

    @Autowired
    public AnalyzerLabUnitServiceImpl(AnalyzerLabUnitDAO analyzerLabUnitDAO) {
        super(AnalyzerLabUnit.class);
        this.analyzerLabUnitDAO = analyzerLabUnitDAO;
    }

    @Override
    protected BaseDAO<AnalyzerLabUnit, AnalyzerLabUnitId> getBaseObjectDAO() {
        return analyzerLabUnitDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerLabUnit> getLabUnitsByAnalyzerId(String analyzerId) {
        Integer id = Integer.valueOf(analyzerId);
        return analyzerLabUnitDAO.findByAnalyzerId(id);
    }

    @Override
    public void replaceLabUnitsForAnalyzer(String analyzerId, List<String> labUnitIds) {
        Integer id = Integer.valueOf(analyzerId);
        analyzerLabUnitDAO.replaceForAnalyzer(id, labUnitIds);
    }
}
