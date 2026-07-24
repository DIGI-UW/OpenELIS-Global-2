package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.form.AnalyzerResultValueOption;

public interface AnalyzerResultValueOptionService {

    List<AnalyzerResultValueOption> getOptions(String analyzerId, String analyzerTestCode);

    List<AnalyzerResultValueOption> findOptions(String analyzerId, String analyzerTestCode);

    AnalyzerResultValueOption requireValidOption(String analyzerId, String analyzerTestCode, String optionId);
}
