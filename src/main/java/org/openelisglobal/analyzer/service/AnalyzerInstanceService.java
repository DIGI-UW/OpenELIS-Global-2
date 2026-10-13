package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.openelisglobal.analyzer.form.AnalyzerInstanceRequest;

public interface AnalyzerInstanceService {

    AnalyzerInstanceView ensureConnection(String analyzerId, ObjectNode values, String actor);

    AnalyzerInstanceView create(AnalyzerInstanceRequest request, String actor);

    List<AnalyzerInstanceState> list();

    AnalyzerInstanceView get(String analyzerId);

    AnalyzerInstanceView update(String analyzerId, AnalyzerInstanceRequest request, String actor);

    AnalyzerInstanceView applyMapping(String analyzerId, String mappingId, int revision, String mappingFingerprint,
            String actor);
}
