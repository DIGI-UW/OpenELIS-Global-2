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
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.analyzerimport.analyzerreaders;

import com.ibm.icu.text.CharsetDetector;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.plugin.AnalyzerImporterPlugin;
import org.openelisglobal.spring.util.SpringContext;

public class ASTMAnalyzerReader extends AnalyzerReader {

    private List<String> lines;
    private AnalyzerImporterPlugin plugin;
    private AnalyzerLineInserter inserter;
    private AnalyzerResponder responder;
    private String error;
    private boolean hasResponse = false;
    private String responseBody;

    @Override
    public boolean readStream(InputStream stream) {
        error = null;
        inserter = null;
        lines = new ArrayList<>();
        BufferedInputStream bis = new BufferedInputStream(stream);
        CharsetDetector detector = new CharsetDetector();
        try {
            detector.setText(bis);
            String charsetName = detector.detect().getName();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(bis, charsetName));

            try {
                for (String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
                    lines.add(line);
                }
            } catch (IOException e) {
                error = "Unable to read input stream";
                LogEvent.logError(e);
                return false;
            }
        } catch (IOException e) {
            error = "Unable to determine message encoding";
            LogEvent.logError("an error occured detecting the encoding of the analyzer message", e);
            return false;
        }

        if (!lines.isEmpty()) {
            setInserterResponder();
            if (inserter == null) {
                error = "Unable to understand which analyzer sent the message";
                return false;
            }
            return true;
        } else {
            error = "Empty message";
            return false;
        }
    }

    public boolean processData(String currentUserId) {
        // it is assumed that all requests are either requests for information
        // or analyzer results to be entered
        if (plugin.isAnalyzerResult(lines)) {
            return insertAnalyzerData(currentUserId);
        } else {
            responseBody = buildResponseForQuery();
            hasResponse = true;
            return true;
        }
    }

    public boolean hasResponse() {
        return hasResponse;
    }

    public String getResponse() {
        return responseBody;
    }

    private void setInserterResponder() {
        for (AnalyzerImporterPlugin plugin : SpringContext.getBean(PluginAnalyzerService.class).getAnalyzerPlugins()) {
            if (plugin.isTargetAnalyzer(lines)) {
                try {
                    this.plugin = plugin;
                    inserter = plugin.getAnalyzerLineInserter();
                    responder = plugin.getAnalyzerResponder();
                    return;
                } catch (RuntimeException e) {
                    LogEvent.logError(e);
                }
            }
        }
    }

    private String buildResponseForQuery() {
        if (responder == null) {
            error = "Unable to understand which analyzer sent the query or plugin doesn't support responding";
            LogEvent.logError(this.getClass().getSimpleName(), "buildResponseForQuery", error);
            return "";
        } else {
            LogEvent.logDebug(this.getClass().getSimpleName(), "buildResponseForQuery", "building response");
            return responder.buildResponse(lines);
        }
    }

    @Override
    public boolean insertAnalyzerData(String systemUserId) {
        if (inserter == null) {
            error = "Unable to understand which analyzer sent the file";
            LogEvent.logError(this.getClass().getSimpleName(), "buildResponseForQuery", error);
            return false;
        } else {
            boolean success = inserter.insert(lines, systemUserId);
            if (!success) {
                error = inserter.getError();
                LogEvent.logError(this.getClass().getSimpleName(), "buildResponseForQuery", error);
            }
            return success;
        }
    }

    @Override
    public String getError() {
        return error;
    }
}
