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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.analyzer.service.HL7MessageService;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.plugin.AnalyzerImporterPlugin;
import org.openelisglobal.spring.util.SpringContext;

/**
 * AnalyzerReader for HL7 v2.x ORU^R01 result messages.
 *
 * <p>
 * HL7 handling is plugin-only (e.g. GenericHL7). Parses message with
 * HL7MessageService for validation and segment lines, then delegates to the
 * matching plugin's inserter. No built-in/legacy HL7 inserter fallback.
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

        PluginAnalyzerService pluginService = SpringContext.getBean(PluginAnalyzerService.class);
        List<AnalyzerImporterPlugin> plugins = choosePluginOrder(pluginService);

        boolean pluginMatched = false;
        for (AnalyzerImporterPlugin plugin : plugins) {
            try {
                if (plugin.isTargetAnalyzer(lines)) {
                    pluginMatched = true;
                    AnalyzerLineInserter inserter = plugin.getAnalyzerLineInserter();
                    if (inserter != null) {
                        boolean success = inserter.insert(lines, systemUserId);
                        if (!success) {
                            error = inserter.getError();
                            LogEvent.logError(getClass().getSimpleName(), "insertAnalyzerData", error);
                        }
                        return success;
                    }
                }
            } catch (RuntimeException e) {
                pluginMatched = true;
                error = "Plugin " + plugin.getClass().getSimpleName() + " matched but failed: " + e.getMessage();
                LogEvent.logError(error, e);
            }
        }

        if (!pluginMatched) {
            error = "No HL7 plugin matched this message (e.g. configure GenericHL7 with matching identifier pattern)";
        }
        LogEvent.logError(getClass().getSimpleName(), "insertAnalyzerData", error);
        return false;
    }

    /**
     * Return the plugin list in default order. (preferGenericPlugin flag has been
     * removed.)
     */
    private List<AnalyzerImporterPlugin> choosePluginOrder(PluginAnalyzerService pluginService) {
        return pluginService.getAnalyzerPlugins();
    }

    /** HL7 MSH segment field 3 (sending application). Same as GenericHL7. */
    private String parseMsh3(List<String> lines) {
        if (lines == null) {
            return null;
        }
        for (String line : lines) {
            if (line != null && line.startsWith("MSH|")) {
                String[] fields = line.split("\\|");
                if (fields.length > 2 && !StringUtils.isBlank(fields[2])) {
                    return fields[2].trim();
                }
                break;
            }
        }
        return null;
    }

    @Override
    public String getError() {
        return error;
    }
}
