/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.analyzerimport.analyzerreaders;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.analyzer.service.HL7MessageService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.spring.util.SpringContext;

/**
 * Inserts HL7 ORU^R01 segment lines into analyzer results.
 *
 * <p>
 * segment lines, builds AnalyzerResults, persists.
 */
public class HL7AnalyzerLineInserter extends AnalyzerLineInserter {

    private final Analyzer analyzer;
    private String error;

    public HL7AnalyzerLineInserter(Analyzer analyzer) {
        this.analyzer = analyzer;
        this.error = null;
    }

    @Override
    public boolean insert(List<String> lines, String currentUserId) {
        error = null;
        if (lines == null || lines.isEmpty()) {
            error = "HL7 message lines are empty";
            return false;
        }
        if (analyzer == null || analyzer.getId() == null) {
            error = "Analyzer not configured for HL7 insert";
            return false;
        }
        try {
            String raw = String.join("\r", lines);
            HL7MessageService svc = SpringContext.getBean(HL7MessageService.class);
            HL7MessageService.OruR01ParseResult parsed = svc.parseOruR01(raw);
            List<AnalyzerResults> results = new ArrayList<>();
            String accession = StringUtils.isNotBlank(parsed.getFillerOrderNumber()) ? parsed.getFillerOrderNumber()
                    : parsed.getPlacerOrderNumber();
            if (StringUtils.isBlank(accession)) {
                accession = parsed.getPatientId();
            }
            if (StringUtils.isBlank(accession)) {
                accession = "HL7-UNKNOWN";
            }
            for (HL7MessageService.ObxResult obx : parsed.getResults()) {
                AnalyzerResults ar = new AnalyzerResults();
                ar.setAnalyzerId(analyzer.getId());
                ar.setAccessionNumber(accession);
                ar.setTestName(obx.getTestName() != null ? obx.getTestName() : obx.getTestCode());
                ar.setResult(obx.getValue());
                ar.setUnits(obx.getUnits());
                ar.setTestId("-1");
                ar.setResultType("N");
                ar.setCompleteDate(new Timestamp(System.currentTimeMillis()));
                results.add(ar);
            }
            if (results.isEmpty()) {
                error = "HL7 ORU^R01 has no OBX results";
                return false;
            }
            return persistImport(currentUserId, results);
        } catch (HL7MessageService.HL7ParseException e) {
            error = "HL7 parse error: " + e.getMessage();
            LogEvent.logError(e);
            return false;
        } catch (Exception e) {
            error = "HL7 insert error: " + e.getMessage();
            LogEvent.logError(e);
            return false;
        }
    }

    @Override
    public String getError() {
        return error;
    }
}
