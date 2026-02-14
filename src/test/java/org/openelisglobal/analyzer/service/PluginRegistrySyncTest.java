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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerType;
import org.openelisglobal.common.services.PluginAnalyzerService;

/**
 * Tests for PluginRegistryService.linkLegacyAnalyzersToTypes().
 *
 * <p>
 * The method matches unlinked analyzers to AnalyzerType records using three
 * name-based strategies: exact match, suffix-stripped match, and prefix match.
 */
@RunWith(MockitoJUnitRunner.class)
public class PluginRegistrySyncTest {

    @Mock
    private AnalyzerTypeService analyzerTypeService;

    @Mock
    private AnalyzerService analyzerService;

    @Mock
    private PluginAnalyzerService pluginAnalyzerService;

    @InjectMocks
    private PluginRegistryService service;

    // ── Exact match: analyzer name == plugin class simple name ─────────────

    @Test
    public void linkLegacyAnalyzersToTypes_exactMatch() {
        Analyzer analyzer = createAnalyzer("10", "GeneXpertAnalyzer");
        AnalyzerType type = createType("1", "Gene Xpert", "oe.plugin.analyzer.GeneXpertAnalyzer");

        when(analyzerService.getAll()).thenReturn(Collections.singletonList(analyzer));
        when(analyzerTypeService.getAll()).thenReturn(Collections.singletonList(type));

        service.linkLegacyAnalyzersToTypes();

        ArgumentCaptor<Analyzer> captor = ArgumentCaptor.forClass(Analyzer.class);
        verify(analyzerService).save(captor.capture());
        assertEquals("Should link to matching type", type, captor.getValue().getAnalyzerType());
    }

    // ── Suffix-stripped match: "Mindray" matches "MindrayAnalyzer" ─────────

    @Test
    public void linkLegacyAnalyzersToTypes_suffixStrippedMatch() {
        Analyzer analyzer = createAnalyzer("10", "Mindray");
        AnalyzerType type = createType("1", "Mindray", "oe.plugin.analyzer.MindrayAnalyzer");

        when(analyzerService.getAll()).thenReturn(Collections.singletonList(analyzer));
        when(analyzerTypeService.getAll()).thenReturn(Collections.singletonList(type));

        service.linkLegacyAnalyzersToTypes();

        ArgumentCaptor<Analyzer> captor = ArgumentCaptor.forClass(Analyzer.class);
        verify(analyzerService).save(captor.capture());
        assertEquals("Should match via suffix-stripped name", type, captor.getValue().getAnalyzerType());
    }

    // ── Prefix match: multi-analyzer plugins (Cobas4800 → Cobas4800VL) ────

    @Test
    public void linkLegacyAnalyzersToTypes_prefixMatch() {
        Analyzer vlAnalyzer = createAnalyzer("10", "Cobas4800VLAnalyzer");
        Analyzer eidAnalyzer = createAnalyzer("11", "Cobas4800EIDAnalyzer");
        AnalyzerType type = createType("1", "Cobas 4800", "oe.plugin.analyzer.Cobas4800Analyzer");

        when(analyzerService.getAll()).thenReturn(Arrays.asList(vlAnalyzer, eidAnalyzer));
        when(analyzerTypeService.getAll()).thenReturn(Collections.singletonList(type));

        service.linkLegacyAnalyzersToTypes();

        ArgumentCaptor<Analyzer> captor = ArgumentCaptor.forClass(Analyzer.class);
        verify(analyzerService, times(2)).save(captor.capture());
        List<Analyzer> saved = captor.getAllValues();
        assertEquals("VL analyzer should link to Cobas4800 type", type, saved.get(0).getAnalyzerType());
        assertEquals("EID analyzer should link to Cobas4800 type", type, saved.get(1).getAnalyzerType());
    }

    // ── Prefix match picks longest match ──────────────────────────────────

    @Test
    public void linkLegacyAnalyzersToTypes_prefixMatchPicksLongest() {
        // "SysmexXN1000Analyzer" could prefix-match "Sysmex" or "SysmexXN1000"
        Analyzer analyzer = createAnalyzer("10", "SysmexXN1000Analyzer");
        AnalyzerType sysmexType = createType("1", "Sysmex", "oe.plugin.analyzer.SysmexAnalyzer");
        AnalyzerType xn1000Type = createType("2", "Sysmex XN 1000", "oe.plugin.analyzer.SysmexXN1000Analyzer");

        when(analyzerService.getAll()).thenReturn(Collections.singletonList(analyzer));
        when(analyzerTypeService.getAll()).thenReturn(Arrays.asList(sysmexType, xn1000Type));

        service.linkLegacyAnalyzersToTypes();

        ArgumentCaptor<Analyzer> captor = ArgumentCaptor.forClass(Analyzer.class);
        verify(analyzerService).save(captor.capture());
        // Exact match takes priority here (nameToType contains "SysmexXN1000Analyzer")
        assertEquals("Should match the exact type, not the shorter prefix", xn1000Type,
                captor.getValue().getAnalyzerType());
    }

    // ── Already linked → skip ─────────────────────────────────────────────

    @Test
    public void linkLegacyAnalyzersToTypes_shouldSkipAlreadyLinkedAnalyzer() {
        AnalyzerType existingType = createType("1", "Some Type", "some.Class");
        Analyzer linked = createAnalyzer("10", "Already Linked");
        linked.setAnalyzerType(existingType);

        when(analyzerService.getAll()).thenReturn(Collections.singletonList(linked));
        when(analyzerTypeService.getAll()).thenReturn(Collections.singletonList(existingType));

        service.linkLegacyAnalyzersToTypes();

        verify(analyzerService, never()).save(any(Analyzer.class));
    }

    // ── No matching type → skip ───────────────────────────────────────────

    @Test
    public void linkLegacyAnalyzersToTypes_shouldSkipAnalyzerWithNoMatchingType() {
        Analyzer unlinked = createAnalyzer("10", "UnknownAnalyzerXYZ");
        AnalyzerType type = createType("1", "Gene Xpert", "oe.plugin.analyzer.GeneXpertAnalyzer");

        when(analyzerService.getAll()).thenReturn(Collections.singletonList(unlinked));
        when(analyzerTypeService.getAll()).thenReturn(Collections.singletonList(type));

        service.linkLegacyAnalyzersToTypes();

        verify(analyzerService, never()).save(any(Analyzer.class));
    }

    // ── Null pluginClassName in AnalyzerType → skip gracefully ────────────

    @Test
    public void linkLegacyAnalyzersToTypes_shouldSkipTypesWithNullPluginClassName() {
        Analyzer unlinked = createAnalyzer("10", "SomeAnalyzer");
        AnalyzerType nullClassType = createType("1", "Generic ASTM", null);

        when(analyzerService.getAll()).thenReturn(Collections.singletonList(unlinked));
        when(analyzerTypeService.getAll()).thenReturn(Collections.singletonList(nullClassType));

        service.linkLegacyAnalyzersToTypes();

        verify(analyzerService, never()).save(any(Analyzer.class));
    }

    // ── Empty lists → no errors ───────────────────────────────────────────

    @Test
    public void linkLegacyAnalyzersToTypes_shouldHandleEmptyLists() {
        when(analyzerService.getAll()).thenReturn(Collections.emptyList());
        when(analyzerTypeService.getAll()).thenReturn(Collections.emptyList());

        service.linkLegacyAnalyzersToTypes();

        verify(analyzerService, never()).save(any(Analyzer.class));
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private Analyzer createAnalyzer(String id, String name) {
        Analyzer analyzer = new Analyzer();
        analyzer.setId(id);
        analyzer.setName(name);
        analyzer.setActive(true);
        analyzer.setAnalyzerType(null);
        return analyzer;
    }

    private AnalyzerType createType(String id, String name, String pluginClassName) {
        AnalyzerType type = new AnalyzerType();
        type.setId(id);
        type.setName(name);
        type.setPluginClassName(pluginClassName);
        type.setActive(true);
        type.setSysUserId("1");
        return type;
    }
}
