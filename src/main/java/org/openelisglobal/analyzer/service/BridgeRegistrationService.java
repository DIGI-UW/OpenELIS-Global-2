package org.openelisglobal.analyzer.service;

import java.time.Duration;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Bridge registration client for analyzer transport metadata. */
@Service
public class BridgeRegistrationService {

    private static final String CLASS_NAME = "BridgeRegistrationService";

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @Value("${analyzer.bridge.url:}")
    private String bridgeBaseUrl;

    @Autowired
    private BridgeHttpClient bridgeHttpClient;

    // Optional — null in older deployments without QC rule support; service
    // exists in current codebase. Autowired to avoid threading qcRules through
    // every registerFile caller.
    @Autowired(required = false)
    private AnalyzerQcRuleService analyzerQcRuleService;

    // Optional — null in older deployments without control-lot support.
    // Used to publish active lot inventory to the bridge so the bridge can
    // disambiguate in-message lot encodings (ASTM Q-segment, FILE sample
    // names containing the lot number).
    @Autowired(required = false)
    private org.openelisglobal.qc.service.QCControlLotService qcControlLotService;

    // The analyzer's test_code ↔ LOINC mapping, pushed so the bridge can
    // translate both directions (inbound code→LOINC, outbound LOINC→code) and
    // OE2 stays analyzer-agnostic. Sourced from AnalyzerTestMapping (the lab's
    // configured per-analyzer mapping, seeded from the profile) joined to
    // Test.loinc — the same LOINC OE2 binds inbound results by.
    @Autowired(required = false)
    private org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService analyzerTestMappingService;

    @Autowired(required = false)
    private org.openelisglobal.test.service.TestService testService;

    /** Register a TCP analyzer (ASTM/HL7) with the bridge. */
    public boolean registerTcp(String oeAnalyzerId, String name, String ip, Integer port, String protocol,
            String identifierPattern) {
        if (!isBridgeConfigured()) {
            return false;
        }

        try {
            java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("oeAnalyzerId", oeAnalyzerId);
            payload.put("sourceId", ip);
            payload.put("name", name);
            payload.put("protocol", protocol != null ? protocol : "ASTM");
            // The same regex OE uses to identify an inbound message by its sender
            // (Analyzer.identifierPattern). Letting the bridge corroborate the
            // connection-source-IP identity against this authoritative pattern —
            // rather than a name-substring heuristic — keeps the two identity
            // signals consistent (e.g. ASTM sender "GENEXPERT^GeneXpert^4.6.0").
            if (identifierPattern != null && !identifierPattern.isBlank()) {
                payload.put("identifierPattern", identifierPattern);
            }
            attachQcRules(payload, oeAnalyzerId);
            attachControlLots(payload, oeAnalyzerId);
            attachTestCodeLoinc(payload, oeAnalyzerId);
            String json = objectMapper.writeValueAsString(payload);
            return callRegister(json, oeAnalyzerId);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            LogEvent.logError(CLASS_NAME, "registerTcp", "Failed to build registration JSON: " + e.getMessage());
            return false;
        }
    }

    /**
     * Register a FILE analyzer with the bridge, including all config needed for
     * file parsing (column mappings, format, delimiter, skipRows).
     */
    public boolean registerFile(String oeAnalyzerId, String name, String watchDir, String filePattern,
            java.util.Map<String, String> columnMappings, String fileFormat, String delimiter, Integer skipRows,
            java.util.List<String> testMappings) {
        if (!isBridgeConfigured()) {
            return false;
        }

        try {
            java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("oeAnalyzerId", oeAnalyzerId);
            payload.put("sourceId", watchDir);
            payload.put("name", name);
            payload.put("protocol", "FILE");
            payload.put("filePattern", filePattern != null ? filePattern : "");
            if (columnMappings != null && !columnMappings.isEmpty()) {
                payload.put("columnMappings", columnMappings);
            }
            if (fileFormat != null && !fileFormat.isBlank()) {
                payload.put("fileFormat", fileFormat);
            }
            if (delimiter != null && !delimiter.isEmpty()) {
                payload.put("delimiter", delimiter);
            }
            if (skipRows != null && skipRows > 0) {
                payload.put("skipRows", skipRows);
            }
            if (testMappings != null && !testMappings.isEmpty()) {
                payload.put("testMappings", testMappings);
            }
            attachQcRules(payload, oeAnalyzerId);
            attachControlLots(payload, oeAnalyzerId);
            attachTestCodeLoinc(payload, oeAnalyzerId);
            String json = objectMapper.writeValueAsString(payload);
            return callRegister(json, oeAnalyzerId);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            LogEvent.logError(CLASS_NAME, "registerFile", "Failed to build registration JSON: " + e.getMessage());
            return false;
        }
    }

    /**
     * Fetch the bridge's view of registered analyzers (v5 §4.1 two-way sync). Used
     * for drift detection — the webapp is the config authority, but the bridge's
     * local state should mirror it. Any mismatch is a sync bug to investigate.
     *
     * @return list of {id, name, protocol, sourceId, mappedTestCodes, ...} maps or
     *         an empty list if the bridge is unreachable / unconfigured.
     */
    @SuppressWarnings("unchecked")
    public java.util.List<java.util.Map<String, Object>> fetchBridgeState() {
        if (!isBridgeConfigured()) {
            return java.util.Collections.emptyList();
        }
        try {
            String endpoint = bridgeBaseUrl.replaceAll("/+$", "") + "/api/analyzers";
            BridgeHttpClient.BridgeResponse response = bridgeHttpClient.get(endpoint, Duration.ofSeconds(10));
            if (response.status != 200) {
                LogEvent.logWarn(CLASS_NAME, "fetchBridgeState",
                        "Bridge GET /api/analyzers returned " + response.status);
                return java.util.Collections.emptyList();
            }
            // Response shape: {"<sourceId>": {id, name, expectedProtocol, mappedTestCodes,
            // ...}, ...}
            java.util.Map<String, Object> raw = objectMapper.readValue(response.body, java.util.Map.class);
            java.util.List<java.util.Map<String, Object>> flat = new java.util.ArrayList<>();
            for (java.util.Map.Entry<String, Object> e : raw.entrySet()) {
                if (e.getValue() instanceof java.util.Map) {
                    java.util.Map<String, Object> entry = new java.util.LinkedHashMap<>(
                            (java.util.Map<String, Object>) e.getValue());
                    entry.put("sourceId", e.getKey());
                    flat.add(entry);
                }
            }
            return flat;
        } catch (Exception e) {
            LogEvent.logWarn(CLASS_NAME, "fetchBridgeState", "Failed to fetch bridge state: " + e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Full-state reconciliation with the bridge (v5 §4.2 two-way sync). The webapp
     * is the config authority. This call sends the webapp's full list of analyzer
     * registration payloads to the bridge, which deletes any entries not in the
     * list and adds/updates the ones that are. Handles:
     *
     * <ul>
     * <li>Bridge restarted after webapp's per-analyzer push loop
     * <li>Bridge has a stale entry from an analyzer the webapp deleted
     * <li>Partial registration failure during webapp boot
     * </ul>
     *
     * Call once on webapp boot after the per-analyzer registration loop.
     *
     * @param payloads list of registration maps (same shape as POST /register
     *                 bodies). Caller builds them from Analyzer entities.
     * @return true if the sync call succeeded (2xx response), false otherwise
     */
    public boolean syncAll(java.util.List<java.util.Map<String, Object>> payloads) {
        if (!isBridgeConfigured()) {
            return false;
        }
        try {
            String json = objectMapper.writeValueAsString(payloads);
            String endpoint = bridgeBaseUrl.replaceAll("/+$", "") + "/api/analyzers/sync";
            BridgeHttpClient.BridgeResponse response = bridgeHttpClient.put(endpoint, json, Duration.ofSeconds(30));
            if (response.isSuccess()) {
                LogEvent.logInfo(CLASS_NAME, "syncAll",
                        "Bridge sync reconciled " + payloads.size() + " analyzers: " + response.body);
                return true;
            } else {
                LogEvent.logWarn(CLASS_NAME, "syncAll",
                        "Bridge PUT /api/analyzers/sync returned " + response.status + ": " + response.body);
                return false;
            }
        } catch (Exception e) {
            LogEvent.logWarn(CLASS_NAME, "syncAll", "Failed to sync analyzer list with bridge: " + e.getMessage());
            return false;
        }
    }

    /** Unregister an analyzer from the bridge. */
    public boolean unregister(String oeAnalyzerId) {
        if (!isBridgeConfigured()) {
            return false;
        }

        try {
            String endpoint = bridgeBaseUrl.replaceAll("/+$", "") + "/api/analyzers/" + oeAnalyzerId;
            BridgeHttpClient.BridgeResponse response = bridgeHttpClient.delete(endpoint, Duration.ofSeconds(10));
            if (response.status == 200) {
                LogEvent.logInfo(CLASS_NAME, "unregister", "Unregistered analyzer " + oeAnalyzerId + " from bridge");
                return true;
            } else {
                LogEvent.logWarn(CLASS_NAME, "unregister",
                        "Bridge unregister returned " + response.status + " for analyzer " + oeAnalyzerId);
                return false;
            }
        } catch (Exception e) {
            LogEvent.logWarn(CLASS_NAME, "unregister",
                    "Failed to unregister analyzer " + oeAnalyzerId + " from bridge: " + e.getMessage());
            return false;
        }
    }

    private boolean callRegister(String json, String oeAnalyzerId) {
        try {
            String endpoint = bridgeBaseUrl.replaceAll("/+$", "") + "/api/analyzers/register";
            BridgeHttpClient.BridgeResponse response = bridgeHttpClient.post(endpoint, json, Duration.ofSeconds(10));
            if (response.status == 200) {
                LogEvent.logInfo(CLASS_NAME, "callRegister", "Registered analyzer " + oeAnalyzerId + " with bridge");
                return true;
            } else {
                LogEvent.logWarn(CLASS_NAME, "callRegister",
                        "Bridge register returned " + response.status + ": " + response.body);
                return false;
            }
        } catch (Exception e) {
            LogEvent.logWarn(CLASS_NAME, "callRegister",
                    "Failed to register analyzer " + oeAnalyzerId + " with bridge: " + e.getMessage());
            return false;
        }
    }

    private boolean isBridgeConfigured() {
        if (bridgeBaseUrl == null || bridgeBaseUrl.isBlank()) {
            LogEvent.logDebug(CLASS_NAME, "isBridgeConfigured",
                    "No analyzer.bridge.url configured — skipping bridge registration");
            return false;
        }
        return true;
    }

    /**
     * Attach the analyzer's active QC classification rules (e.g.
     * {@code SPECIMEN_ID_PREFIX QC} for HL7, {@code FIELD_EQUALS O.12 Q} for ASTM,
     * {@code SPECIMEN_ID_PREFIX LPC/HPC} for FILE) so the bridge can classify QC vs
     * patient samples without falling back to its hardcoded default prefix list.
     *
     * <p>
     * Package-private so {@link AnalyzerBridgeStartupRegistrar}'s full-state sync
     * can reuse the same payload shape — without this the sync would push entries
     * with no qcRules / controlLots and the bridge would lose its classification +
     * lot inventory on every restart.
     */
    /**
     * Attach the analyzer's {@code test_code → LOINC} map so the bridge can
     * translate inbound results (code→LOINC) and outbound orders (LOINC→code).
     * Built from the lab's configured {@code AnalyzerTestMapping} rows (analyzer
     * test code → OE2 testId) joined to {@code Test.loinc} — the same LOINC OE2
     * resolves inbound results by. Always attaches (possibly empty) so a sync
     * payload can clear stale bridge mappings.
     */
    void attachTestCodeLoinc(java.util.Map<String, Object> payload, String oeAnalyzerId) {
        java.util.Map<String, String> codeToLoinc = new java.util.LinkedHashMap<>();
        if (analyzerTestMappingService != null && testService != null) {
            for (org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping m : analyzerTestMappingService
                    .getAllForAnalyzer(oeAnalyzerId)) {
                String code = m.getAnalyzerTestName();
                String testId = m.getTestId();
                if (code == null || code.isBlank() || testId == null) {
                    continue;
                }
                try {
                    org.openelisglobal.test.valueholder.Test test = testService.get(testId);
                    String loinc = test != null ? test.getLoinc() : null;
                    if (loinc != null && !loinc.isBlank()) {
                        codeToLoinc.put(code, loinc);
                    }
                } catch (Exception e) {
                    LogEvent.logWarn(CLASS_NAME, "attachTestCodeLoinc",
                            "Could not resolve LOINC for testId " + testId + ": " + e.getMessage());
                }
            }
        }
        payload.put("testCodeLoinc", codeToLoinc);
    }

    void attachQcRules(java.util.Map<String, Object> payload, String oeAnalyzerId) {
        if (analyzerQcRuleService == null) {
            return;
        }
        // Always attach `qcRules` (empty list when no active rules) so a sync
        // payload can distinguish "no rules — clear bridge state" from
        // "field absent — leave bridge state alone". Mirrors attachControlLots.
        java.util.List<java.util.Map<String, Object>> qcRulesPayload = new java.util.ArrayList<>();
        java.util.List<QcRuleDto> qcRules = analyzerQcRuleService.getActiveRuleDtosForAnalyzer(oeAnalyzerId);
        if (qcRules != null) {
            for (QcRuleDto r : qcRules) {
                java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("ruleType", r.ruleType());
                if (r.targetField() != null) {
                    m.put("targetField", r.targetField());
                }
                m.put("operand", r.operand());
                qcRulesPayload.add(m);
            }
        }
        payload.put("qcRules", qcRulesPayload);
    }

    /**
     * Attach the analyzer's active control lots so the bridge can disambiguate lot
     * encodings carried in inbound messages — FILE sample names whose sampleId
     * contains the lot number, ASTM Q-segment field 3 components. Without this the
     * bridge has no way to surface lot identity, and OE's Tier 1 lot match falls
     * through to Tier 2/3 (controlLevel match or single-active-lot fallback).
     */
    void attachControlLots(java.util.Map<String, Object> payload, String oeAnalyzerId) {
        // Always attach `controlLots` (empty list when no data) so the bridge
        // contract is stable — a missing key would let downstream sync logic
        // treat "absent" as "do not change", which conflicts with the
        // intended "no active lots, clear them" semantics.
        java.util.List<java.util.Map<String, Object>> lotsPayload = new java.util.ArrayList<>();
        if (qcControlLotService != null) {
            // oeAnalyzerId is a String matching Analyzer.id / QCControlLot.instrumentId
            // typing — pass through, no parsing needed.
            if (oeAnalyzerId != null && !oeAnalyzerId.isBlank()) {
                java.util.List<org.openelisglobal.qc.valueholder.QCControlLot> lots = qcControlLotService
                        .getActiveControlLotsByInstrument(oeAnalyzerId);
                if (lots != null) {
                    for (org.openelisglobal.qc.valueholder.QCControlLot lot : lots) {
                        if (lot.getLotNumber() == null || lot.getLotNumber().isBlank()) {
                            continue;
                        }
                        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                        m.put("lotNumber", lot.getLotNumber());
                        if (lot.getControlLevel() != null && !lot.getControlLevel().isBlank()) {
                            m.put("controlLevel", lot.getControlLevel());
                        }
                        if (lot.getTestId() != null) {
                            m.put("testId", lot.getTestId());
                        }
                        lotsPayload.add(m);
                    }
                }
            }
        }
        payload.put("controlLots", lotsPayload);
    }

}
