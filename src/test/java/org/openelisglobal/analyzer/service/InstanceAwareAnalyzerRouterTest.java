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
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.service.InstanceAwareAnalyzerRouter.RouteContext;
import org.openelisglobal.analyzer.service.InstanceAwareAnalyzerRouter.RouteResult;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerType;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.plugin.AnalyzerImporterPlugin;

/**
 * Unit tests for InstanceAwareAnalyzerRouter.
 *
 * <p>
 * Verifies the 2-stage routing architecture:
 * <ol>
 * <li>Stage 1: IP-based routing (analyzer.ip_address — merged model)
 * <li>Stage 2: Plugin routing (plugin.isTargetAnalyzer() for all plugins)
 * </ol>
 *
 * <p>
 * There is intentionally NO type-pattern matching stage. Generic plugins handle
 * their own DB-driven pattern matching inside isTargetAnalyzer().
 */
@RunWith(MockitoJUnitRunner.class)
public class InstanceAwareAnalyzerRouterTest {

    @Mock
    private AnalyzerTypeService analyzerTypeService;

    @Mock
    private AnalyzerService analyzerService;

    @Mock
    private PluginAnalyzerService pluginAnalyzerService;

    @Mock
    private AnalyzerImporterPlugin genericAstmPlugin;

    @Mock
    private AnalyzerImporterPlugin legacyPlugin;

    @InjectMocks
    private InstanceAwareAnalyzerRouter router;

    private Analyzer testAnalyzer;
    private AnalyzerType testType;

    @Before
    public void setUp() {
        testType = new AnalyzerType();
        testType.setId("1");
        testType.setName("Generic ASTM");
        testType.setIdentifierPattern("HORIBA.*H500");
        testType.setGenericPlugin(true);

        testAnalyzer = new Analyzer();
        testAnalyzer.setId("10");
        testAnalyzer.setName("Test ASTM Analyzer");
        testAnalyzer.setActive(true);
        testAnalyzer.setAnalyzerType(testType);
        testAnalyzer.setIpAddress("192.168.1.50");
    }

    // ── Stage 1: IP-based routing ──────────────────────────────────────────

    @Test
    public void route_shouldRouteByIp_whenIpMatches() {
        List<String> lines = Arrays.asList("H|\\^&|||HORIBA^H500^1.0");
        RouteContext ctx = new RouteContext("192.168.1.50", "HORIBA^H500^1.0", lines);

        when(analyzerService.getByIpAddress("192.168.1.50")).thenReturn(Optional.of(testAnalyzer));
        when(pluginAnalyzerService.getPluginByAnalyzerId("10")).thenReturn(genericAstmPlugin);

        RouteResult result = router.route(ctx);

        assertTrue(result.isSuccessful());
        assertEquals("IP_MATCH", result.getRouteMethod());
        assertEquals(testAnalyzer, result.getAnalyzer());
    }

    @Test
    public void route_shouldSkipIp_whenNoIpProvided() {
        List<String> lines = Arrays.asList("H|\\^&|||HORIBA^H500^1.0");
        RouteContext ctx = new RouteContext(null, "HORIBA^H500^1.0", lines);

        when(pluginAnalyzerService.getAnalyzerPlugins()).thenReturn(Collections.singletonList(genericAstmPlugin));
        when(genericAstmPlugin.isTargetAnalyzer(lines)).thenReturn(true);

        // Find analyzer for plugin via type lookup
        when(analyzerTypeService.getByPluginClassName(anyString())).thenReturn(Optional.of(testType));
        testType.setInstances(Collections.singletonList(testAnalyzer));

        RouteResult result = router.route(ctx);

        assertTrue(result.isSuccessful());
        assertEquals("PLUGIN_MATCH", result.getRouteMethod());
    }

    // ── Stage 2: Plugin routing (replaces old Stage 3) ─────────────────────

    @Test
    public void route_shouldRouteByPlugin_whenIpDoesNotMatch() {
        List<String> lines = Arrays.asList("H|\\^&|||ABX^PENTRA60");
        RouteContext ctx = new RouteContext("10.0.0.99", "ABX^PENTRA60", lines);

        // IP lookup returns nothing
        when(analyzerService.getByIpAddress("10.0.0.99")).thenReturn(Optional.empty());

        // Plugin iteration finds a match
        when(pluginAnalyzerService.getAnalyzerPlugins()).thenReturn(Collections.singletonList(legacyPlugin));
        when(legacyPlugin.isTargetAnalyzer(lines)).thenReturn(true);

        Analyzer legacyAnalyzer = new Analyzer();
        legacyAnalyzer.setId("20");
        legacyAnalyzer.setName("Horiba ABX Pentra 60");
        legacyAnalyzer.setActive(true);

        when(analyzerTypeService.getByPluginClassName(anyString())).thenReturn(Optional.empty());
        when(analyzerService.getAll()).thenReturn(Collections.singletonList(legacyAnalyzer));
        when(pluginAnalyzerService.getPluginByAnalyzerId("20")).thenReturn(legacyPlugin);

        RouteResult result = router.route(ctx);

        assertTrue(result.isSuccessful());
        assertEquals("PLUGIN_MATCH", result.getRouteMethod());
        assertEquals(legacyAnalyzer, result.getAnalyzer());
    }

    // ── No type-pattern stage ──────────────────────────────────────────────

    @Test
    public void route_shouldNeverUseFindMatchingType() {
        List<String> lines = Arrays.asList("H|\\^&|||HORIBA^H500^1.0");
        RouteContext ctx = new RouteContext(null, "HORIBA^H500^1.0", lines);

        when(pluginAnalyzerService.getAnalyzerPlugins()).thenReturn(Collections.emptyList());

        RouteResult result = router.route(ctx);

        // findMatchingType should never be called — Stage 2 (type pattern) is removed
        verify(analyzerTypeService, never()).findMatchingType(anyString());
        assertFalse(result.isSuccessful());
    }

    @Test
    public void route_shouldNeverReturnPatternMatch() {
        List<String> lines = Arrays.asList("H|\\^&|||HORIBA^H500^1.0");
        RouteContext ctx = new RouteContext("192.168.1.50", "HORIBA^H500^1.0", lines);

        when(analyzerService.getByIpAddress("192.168.1.50")).thenReturn(Optional.of(testAnalyzer));
        when(pluginAnalyzerService.getPluginByAnalyzerId("10")).thenReturn(genericAstmPlugin);

        RouteResult result = router.route(ctx);

        // PATTERN_MATCH route method should never appear — that stage is removed
        assertFalse("PATTERN_MATCH".equals(result.getRouteMethod()));
        assertFalse("PATTERN_NO_MATCH".equals(result.getRouteMethod()));
    }

    // ── No match ───────────────────────────────────────────────────────────

    @Test
    public void route_shouldReturnNoMatch_whenNothingMatches() {
        List<String> lines = Arrays.asList("H|\\^&|||UNKNOWN^DEVICE");
        RouteContext ctx = new RouteContext(null, "UNKNOWN^DEVICE", lines);

        when(pluginAnalyzerService.getAnalyzerPlugins()).thenReturn(Collections.singletonList(legacyPlugin));
        when(legacyPlugin.isTargetAnalyzer(lines)).thenReturn(false);

        RouteResult result = router.route(ctx);

        assertFalse(result.isSuccessful());
        assertEquals("NO_MATCH", result.getRouteMethod());
    }

    // ── Convenience method ─────────────────────────────────────────────────

    @Test
    public void route_convenienceMethod_shouldWorkWithoutIp() {
        List<String> lines = Arrays.asList("H|\\^&|||ABX^PENTRA60");

        Analyzer legacyAnalyzer = new Analyzer();
        legacyAnalyzer.setId("20");
        legacyAnalyzer.setName("Horiba ABX Pentra 60");
        legacyAnalyzer.setActive(true);

        when(pluginAnalyzerService.getAnalyzerPlugins()).thenReturn(Collections.singletonList(legacyPlugin));
        when(legacyPlugin.isTargetAnalyzer(lines)).thenReturn(true);
        when(analyzerTypeService.getByPluginClassName(anyString())).thenReturn(Optional.empty());
        when(analyzerService.getAll()).thenReturn(Collections.singletonList(legacyAnalyzer));
        when(pluginAnalyzerService.getPluginByAnalyzerId("20")).thenReturn(legacyPlugin);

        RouteResult result = router.route("ABX^PENTRA60", lines);

        assertTrue(result.isSuccessful());
        assertEquals("PLUGIN_MATCH", result.getRouteMethod());
    }

    // ── Plugin error handling ──────────────────────────────────────────────

    @Test
    public void route_shouldContinueToNextPlugin_whenPluginThrowsException() {
        List<String> lines = Arrays.asList("H|\\^&|||SOME^DEVICE");
        RouteContext ctx = new RouteContext(null, "SOME^DEVICE", lines);

        Analyzer workingAnalyzer = new Analyzer();
        workingAnalyzer.setId("30");
        workingAnalyzer.setName("Working Analyzer");
        workingAnalyzer.setActive(true);

        when(pluginAnalyzerService.getAnalyzerPlugins()).thenReturn(Arrays.asList(legacyPlugin, genericAstmPlugin));
        when(legacyPlugin.isTargetAnalyzer(lines)).thenThrow(new RuntimeException("plugin error"));
        when(genericAstmPlugin.isTargetAnalyzer(lines)).thenReturn(true);
        when(analyzerTypeService.getByPluginClassName(anyString())).thenReturn(Optional.empty());
        when(analyzerService.getAll()).thenReturn(Collections.singletonList(workingAnalyzer));
        when(pluginAnalyzerService.getPluginByAnalyzerId("30")).thenReturn(genericAstmPlugin);

        RouteResult result = router.route(ctx);

        assertTrue(result.isSuccessful());
        assertEquals("PLUGIN_MATCH", result.getRouteMethod());
    }
}
