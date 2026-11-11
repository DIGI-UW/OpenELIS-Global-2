package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerActivationRecord;
import org.openelisglobal.analyzer.valueholder.AnalyzerMapping;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingConfirmation;

public interface AnalyzerActivationRecordService {

    AnalyzerActivationRecord retain(Analyzer analyzer, AnalyzerMapping mappingRevision,
            AnalyzerMappingConfirmation confirmation, ObjectNode runtimeAcknowledgement, String intent, String actor);

    List<AnalyzerActivationRecord> findByAnalyzerId(String analyzerId);
}
