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
package org.openelisglobal.analyzerimport.action;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analyzer.service.BidirectionalAnalyzer;
import org.openelisglobal.analyzerimport.analyzerreaders.ASTMAnalyzerReader;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerReader;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerReaderFactory;
import org.openelisglobal.analyzerimport.analyzerreaders.HL7AnalyzerReader;
import org.openelisglobal.analyzerimport.util.AnalyzerTestNameCache;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.login.service.LoginUserService;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.plugin.AnalyzerImporterPlugin;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class AnalyzerImportController implements IActionConstants {

    @Autowired
    protected LoginUserService loginService;
    @Autowired
    protected SystemUserService systemUserService;
    @Autowired
    private PluginAnalyzerService pluginAnalyzerService;

    @PostMapping("/importAnalyzer")
    protected void doPost(@RequestParam("file") MultipartFile file, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {

        AnalyzerReader reader = null;
        boolean fileRead = false;
        InputStream stream = file.getInputStream();

        reader = AnalyzerReaderFactory.getReaderFor(file.getOriginalFilename());

        if (reader != null) {
            fileRead = reader.readStream(stream);
        }
        if (fileRead) {
            boolean successful = reader.insertAnalyzerData(getSysUserId(request));

            if (successful) {
                response.getWriter().print("success");
                response.setStatus(HttpServletResponse.SC_OK);
                return;
            } else {
                if (reader != null) {
                    response.getWriter().print(reader.getError());
                }
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

        } else {
            if (reader != null) {
                response.getWriter().print(reader.getError());
            }
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
    }

    @PostMapping("/analyzer/astm")
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        ASTMAnalyzerReader reader = null;
        boolean read = false;
        InputStream stream = request.getInputStream();

        reader = (ASTMAnalyzerReader) AnalyzerReaderFactory.getReaderFor("astm");

        if (reader != null) {
            // Pass bridge source-identification headers to reader for deterministic lookup
            setBridgeHeaders(reader, request);
            read = reader.readStream(stream);
            if (read) {
                String userId = getSysUserId(request);
                if (userId == null) {
                    userId = "1"; // Fallback for unauthenticated pushes (bridge/mock)
                }
                boolean success = reader.processData(userId);
                if (reader.hasResponse()) {
                    response.getWriter().print(reader.getResponse());
                }
                if (success) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    return;
                } else {
                    if (reader.getError() != null) {
                        response.getWriter().print(reader.getError());
                    }
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
            } else {
                response.getWriter().print(reader.getError());
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
    }

    /**
     * HTTP endpoint for HL7 ORU^R01 messages (e.g. from mock server or HL7 bridge).
     * Body is raw HL7 message; analyzer is identified from MSH segment.
     */
    @PostMapping("/analyzer/hl7")
    public void doPostHl7(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/plain;charset=UTF-8");
        HL7AnalyzerReader reader = (HL7AnalyzerReader) AnalyzerReaderFactory.getReaderFor("hl7");
        if (reader == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "HL7 reader not available");
            return;
        }
        // Pass bridge source-identification headers to reader for deterministic lookup
        setBridgeHeaders(reader, request);
        boolean read = reader.readStream(request.getInputStream());
        if (!read) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    reader.getError() != null ? reader.getError() : "HL7 read failed");
            return;
        }
        String userId = getSysUserId(request);
        if (userId == null) {
            userId = "1";
        }
        boolean success = reader.insertAnalyzerData(userId);
        if (success) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                reader.getError() != null ? reader.getError() : "HL7 insert failed");
    }

    @PostMapping("/analyzer/runAction")
    public ResponseEntity<String> runAnalyzerAction(@RequestParam String analyzerType,
            @RequestParam String actionName) {
        AnalyzerImporterPlugin analyzerPlugin = pluginAnalyzerService.getPluginByAnalyzerId(
                AnalyzerTestNameCache.getInstance().getAnalyzerIdForName(getAnalyzerNameFromType(analyzerType)));
        if (analyzerPlugin instanceof BidirectionalAnalyzer) {
            BidirectionalAnalyzer bidirectionalAnalyzer = (BidirectionalAnalyzer) analyzerPlugin;
            boolean success = bidirectionalAnalyzer.runLISAction(actionName, null);
            return success ? ResponseEntity.ok().build()
                    : ResponseEntity.internalServerError().body(MessageUtil.getMessage("analyzer.lisaction.failed"));
        }
        return ResponseEntity.badRequest().body(MessageUtil.getMessage("analyzer.lisaction.unsupported"));
    }

    protected String getAnalyzerNameFromType(String analyzerType) {
        String analyzer = null;
        if (!GenericValidator.isBlankOrNull(analyzerType)) {
            analyzer = AnalyzerTestNameCache.getInstance().getDBNameForActionName(analyzerType);
        }
        return analyzer;
    }

    /**
     * Extract bridge source-identification headers and pass to reader. The analyzer
     * bridge sends X-Source-Id (IP) and X-Source-Port headers that enable
     * deterministic analyzer lookup without regex pattern matching.
     *
     * <p>
     * <b>Security note:</b> These headers are trusted because /analyzer/astm and
     * /analyzer/hl7 are designed to receive traffic exclusively from the analyzer
     * bridge, which runs as trusted infrastructure on the internal Docker network.
     * In production, these endpoints should be firewalled to accept traffic only
     * from the bridge container's IP. The headers themselves are set by the bridge
     * from actual TCP connection metadata, not from external client input.
     * </p>
     */
    private void setBridgeHeaders(AnalyzerReader reader, HttpServletRequest request) {
        String sourceIp = request.getHeader("X-Source-Id");
        String sourcePort = request.getHeader("X-Source-Port");

        if (reader instanceof ASTMAnalyzerReader astmReader) {
            if (sourceIp != null && !sourceIp.trim().isEmpty()) {
                astmReader.setClientIpAddress(sourceIp.trim());
            }
            setPortOnReader(sourcePort, port -> astmReader.setClientPort(port));
        } else if (reader instanceof HL7AnalyzerReader hl7Reader) {
            if (sourceIp != null && !sourceIp.trim().isEmpty()) {
                hl7Reader.setClientIpAddress(sourceIp.trim());
            }
            setPortOnReader(sourcePort, port -> hl7Reader.setClientPort(port));
        }
    }

    private void setPortOnReader(String sourcePort, java.util.function.IntConsumer portSetter) {
        if (sourcePort != null && !sourcePort.trim().isEmpty()) {
            try {
                int port = Integer.parseInt(sourcePort.trim());
                if (port < 1 || port > 65535) {
                    LogEvent.logWarn(getClass().getSimpleName(), "setBridgeHeaders",
                            "X-Source-Port out of range (1-65535): " + port);
                    return;
                }
                portSetter.accept(port);
            } catch (NumberFormatException e) {
                LogEvent.logWarn(getClass().getSimpleName(), "setBridgeHeaders",
                        "Invalid X-Source-Port header value (not an integer)");
            }
        }
    }

    private String getSysUserId(HttpServletRequest request) {
        UserSessionData usd = (UserSessionData) request.getAttribute(USER_SESSION_DATA);
        if (usd == null) {
            return null;
        }
        return String.valueOf(usd.getSystemUserId());
    }

    // private String getSysUserId(String user, String password) {
    // LoginUser login = new LoginUser();
    // login.setLoginName(user);
    // login.setPassword(password);
    //
    // login = loginService.getValidatedLogin(user, password).orElse(null);
    //
    // if (login != null) {
    // SystemUser systemUser =
    // systemUserService.getDataForLoginUser(login.getLoginName());
    // return systemUser.getId();
    // }
    //
    // return "";
    // }
    //
    // private boolean userValid(String user, String password) {
    // LoginUser login = new LoginUser();
    // login.setLoginName(user);
    // login.setPassword(password);
    //
    // login = loginService.getValidatedLogin(user, password).orElse(null);
    //
    // if (login == null) {
    // return false;
    // } else {
    // return true;
    // }
    // }

    // private String streamToString(InputStream stream) throws IOException {
    // StringBuilder builder = new StringBuilder();
    // int len;
    // byte[] buffer = new byte[1024];
    // while ((len = stream.read(buffer, 0, buffer.length)) != -1) {
    // builder.append(new String(buffer, 0, len, StandardCharsets.UTF_8));
    // }
    // return builder.toString();
    // }

    // private String fieldStreamToString(InputStream stream) throws IOException {
    // StringBuilder builder = new StringBuilder((int) (FIELD_SIZE_MAX / 2));
    // int len;
    // byte[] buffer = new byte[32];
    // int totalFieldSize = 0;
    //
    // while ((len = stream.read(buffer, 0, buffer.length)) != -1) {
    // builder.append(new String(buffer, 0, len, StandardCharsets.UTF_8));
    // totalFieldSize += len;
    // if (totalFieldSize >= FIELD_SIZE_MAX) {
    // break;
    // }
    // }
    // return builder.toString();
    // }

}
