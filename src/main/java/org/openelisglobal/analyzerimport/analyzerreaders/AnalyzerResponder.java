package org.openelisglobal.analyzerimport.analyzerreaders;

import java.util.List;

public interface AnalyzerResponder {
    public String buildResponse(List<String> lines);
}
