package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerLabUnit;
import org.openelisglobal.common.service.BaseObjectService;

public interface AnalyzerLabUnitService
        extends BaseObjectService<AnalyzerLabUnit, org.openelisglobal.analyzer.valueholder.AnalyzerLabUnitId> {

    List<AnalyzerLabUnit> getLabUnitsByAnalyzerId(String analyzerId);

    void replaceLabUnitsForAnalyzer(String analyzerId, List<String> labUnitIds);
}
