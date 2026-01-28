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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.analyzer.service.HL7MessageService;
import org.openelisglobal.analyzer.service.MappingApplicationService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerConfiguration;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.spring.util.SpringContext;

/**
 * AnalyzerReader for HL7 v2.x ORU^R01 result messages.
 *
 * <p>
 * Task Reference: T008 (M1) – extends AnalyzerReader, uses HAPI PipeParser via
 * HL7MessageService. Parses ORU^R01, identifies analyzer from MSH, delegates to
 * HL7AnalyzerLineInserter (optionally wrapped with
 * MappingAwareHL7AnalyzerLineInserter).
 */
public class HL7AnalyzerReader extends AnalyzerReader {

    private List<String> lines;
    private String error;

    @Override
    public boolean readStream(InputStream stream) {
        error = null;
        lines = null;
        try {
            String raw = IOUtils.toString(stream, StandardCharsets.UTF_8);
            if (StringUtils.isBlank(raw)) {
                error = "Empty HL7 message";
                return false;
            }
            HL7MessageService svc = SpringContext.getBean(HL7MessageService.class);
            svc.parseOruR01(raw);
            lines = svc.toSegmentLines(raw);
            return !lines.isEmpty();
        } catch (HL7MessageService.HL7ParseException e) {
            error = "HL7 parse error: " + e.getMessage();
            LogEvent.logError(e);
            return false;
        } catch (Exception e) {
            error = "Failed to read HL7 stream: " + e.getMessage();
            LogEvent.logError(e);
            return false;
        }
    }

    @Override
    public boolean insertAnalyzerData(String systemUserId) {
        if (lines == null || lines.isEmpty()) {
            error = "No HL7 message loaded";
            return false;
        }
        Optional<Analyzer> analyzerOpt = identifyAnalyzerFromMessage();
        if (!analyzerOpt.isPresent()) {
            error = "Unable to identify analyzer from HL7 MSH (sending application/facility)";
            LogEvent.logError(getClass().getSimpleName(), "insertAnalyzerData", error);
            return false;
        }
        Analyzer analyzer = analyzerOpt.get();
        AnalyzerLineInserter inserter = new HL7AnalyzerLineInserter(analyzer);
        AnalyzerLineInserter finalInserter = wrapInserterIfMappingsExist(inserter, analyzer);
        boolean success = finalInserter.insert(lines, systemUserId);
        if (!success) {
            error = finalInserter.getError();
            LogEvent.logError(getClass().getSimpleName(), "insertAnalyzerData", error);
        }
        return success;
    }

    @Override
    public String getError() {
        return error;
    }

    private AnalyzerLineInserter wrapInserterIfMappingsExist(AnalyzerLineInserter originalInserter, Analyzer analyzer) {
        try {
            MappingApplicationService mappingSvc = SpringContext.getBean(MappingApplicationService.class);
            if (mappingSvc != null && mappingSvc.hasActiveMappings(analyzer.getId())) {
                return new MappingAwareHL7AnalyzerLineInserter(originalInserter, analyzer);
            }
        } catch (Exception e) {
            LogEvent.logError("Error checking HL7 mappings, using plain inserter: " + e.getMessage(), e);
        }
        return originalInserter;
    }

    private Optional<Analyzer> identifyAnalyzerFromMessage() {
        if (lines == null || lines.isEmpty()) {
            return Optional.empty();
        }
        try {
            HL7MessageService svc = SpringContext.getBean(HL7MessageService.class);
            String raw = String.join("\r", lines);
            HL7MessageService.MshInfo msh = svc.extractMshInfo(raw);
            String app = msh.getSendingApplication();
            String fac = msh.getSendingFacility();
            if (StringUtils.isBlank(app) && StringUtils.isBlank(fac)) {
                return Optional.empty();
            }
            org.openelisglobal.analyzer.service.AnalyzerConfigurationService configService = SpringContext
                    .getBean(org.openelisglobal.analyzer.service.AnalyzerConfigurationService.class);
            String name = StringUtils.isNotBlank(app) ? app : fac;
            Optional<AnalyzerConfiguration> config = configService.getByAnalyzerName(name.trim());
            if (config.isPresent() && config.get().getAnalyzer() != null) {
                return Optional.of(config.get().getAnalyzer());
            }
            if (StringUtils.isNotBlank(app) && StringUtils.isNotBlank(fac)) {
                config = configService.getByAnalyzerName((app + " " + fac).trim());
                if (config.isPresent() && config.get().getAnalyzer() != null) {
                    return Optional.of(config.get().getAnalyzer());
                }
            }
        } catch (Exception e) {
            LogEvent.logError("Error identifying HL7 analyzer: " + e.getMessage(), e);
        }
        return Optional.empty();
    }
}
