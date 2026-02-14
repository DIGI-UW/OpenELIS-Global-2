package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.dao.AnalyzerDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyzerServiceImpl extends AuditableBaseObjectServiceImpl<Analyzer, String> implements AnalyzerService {
    @Autowired
    protected AnalyzerDAO baseObjectDAO;
    @Autowired
    private AnalyzerTestMappingService analyzerMappingService;

    AnalyzerServiceImpl() {
        super(Analyzer.class);
    }

    @Override
    protected AnalyzerDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public Analyzer getAnalyzerByName(String name) {
        return getMatch("name", name).orElse(null);
    }

    @Override
    @Transactional
    public void persistData(Analyzer analyzer, List<AnalyzerTestMapping> testMappings,
            List<AnalyzerTestMapping> existingMappings) {
        if (analyzer.getId() == null) {
            insert(analyzer);
        } else {
            update(analyzer);
        }

        for (AnalyzerTestMapping mapping : testMappings) {
            mapping.setAnalyzerId(analyzer.getId());
            if (newMapping(mapping, existingMappings)) {
                mapping.setSysUserId("1");
                analyzerMappingService.insert(mapping);
                existingMappings.add(mapping);
            } else {
                mapping.setLastupdated(analyzerMappingService.get(mapping.getId()).getLastupdated());
                mapping.setSysUserId("1");
                // update in case mapping was preserved before test was made
                analyzerMappingService.update(mapping);
            }
        }
    }

    private boolean newMapping(AnalyzerTestMapping mapping, List<AnalyzerTestMapping> existingMappings) {
        for (AnalyzerTestMapping existingMap : existingMappings) {
            if (existingMap.getAnalyzerId().equals(mapping.getAnalyzerId())
                    && existingMap.getAnalyzerTestName().equals(mapping.getAnalyzerTestName())) {
                return false;
            }
        }
        return true;
    }
}
