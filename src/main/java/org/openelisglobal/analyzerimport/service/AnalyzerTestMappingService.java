package org.openelisglobal.analyzerimport.service;

import java.util.List;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMappingPK;
import org.openelisglobal.common.service.BaseObjectService;

public interface AnalyzerTestMappingService extends BaseObjectService<AnalyzerTestMapping, AnalyzerTestMappingPK> {

    List<AnalyzerTestMapping> getAllForAnalyzer(String analyzerId);
}
