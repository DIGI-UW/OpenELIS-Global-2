package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.common.service.BaseObjectService;

public interface AnalyzerService extends BaseObjectService<Analyzer, String> {
    Analyzer getAnalyzerByName(String name);

    void persistData(Analyzer analyzer, List<AnalyzerTestMapping> testMappings,
            List<AnalyzerTestMapping> existingMappings);
}
