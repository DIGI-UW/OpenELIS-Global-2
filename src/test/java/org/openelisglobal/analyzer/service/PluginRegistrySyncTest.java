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

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.openelisglobal.plugin.AnalyzerImporterPlugin;

/**
 * Tests for PluginRegistryService sync logic: legacy analyzer linking (Gap 2).
 *
 * <p>
 * Gap 1 (orphan deactivation/reactivation) was removed in Phase 4: is_active
 * now means admin-enabled only. JAR availability is computed per-request via
 * pluginLoaded.
 */
@RunWith(MockitoJUnitRunner.class)
public class PluginRegistrySyncTest {

    @Mock
    private AnalyzerTypeService analyzerTypeService;

    @Mock
    private AnalyzerService analyzerService;

    @Mock
    private PluginAnalyzerService pluginAnalyzerService;

    /**
     * Concrete instance — getClass() returns FakeLoadedPlugin (final, can't mock).
     */
    private final AnalyzerImporterPlugin loadedPlugin = new FakeLoadedPlugin();

    @InjectMocks
    private PluginRegistryService service;

    // ── Gap 2: Legacy Linking ──────────────────────────────────────────────

    @Test
    public void linkLegacyAnalyzersToTypes_shouldLinkUnlinkedAnalyzer() {
        Analyzer unlinked = new Analyzer();
        unlinked.setId("10");
        unlinked.setName("Horiba ABX Pentra 60");
        unlinked.setActive(true);
        unlinked.setAnalyzerType(null); // not linked

        AnalyzerType matchingType = createType("1", "Horiba Pentra 60", FakeLoadedPlugin.class.getName(), true);

        when(analyzerService.getAll()).thenReturn(Collections.singletonList(unlinked));
        when(pluginAnalyzerService.getPluginByAnalyzerId("10")).thenReturn(loadedPlugin);
        when(analyzerTypeService.getByPluginClassName(FakeLoadedPlugin.class.getName()))
                .thenReturn(java.util.Optional.of(matchingType));

        service.linkLegacyAnalyzersToTypes();

        ArgumentCaptor<Analyzer> captor = ArgumentCaptor.forClass(Analyzer.class);
        verify(analyzerService).save(captor.capture());
        assertNotNull("Analyzer should be linked to type", captor.getValue().getAnalyzerType());
    }

    @Test
    public void linkLegacyAnalyzersToTypes_shouldSkipAlreadyLinkedAnalyzer() {
        AnalyzerType existingType = createType("1", "Some Type", "some.class", true);
        Analyzer linked = new Analyzer();
        linked.setId("10");
        linked.setName("Already Linked");
        linked.setActive(true);
        linked.setAnalyzerType(existingType);

        when(analyzerService.getAll()).thenReturn(Collections.singletonList(linked));

        service.linkLegacyAnalyzersToTypes();

        verify(analyzerService, never()).save(any(Analyzer.class));
    }

    @Test
    public void linkLegacyAnalyzersToTypes_shouldSkipAnalyzerWithNoPlugin() {
        Analyzer unlinked = new Analyzer();
        unlinked.setId("10");
        unlinked.setName("No Plugin");
        unlinked.setActive(true);
        unlinked.setAnalyzerType(null);

        when(analyzerService.getAll()).thenReturn(Collections.singletonList(unlinked));
        when(pluginAnalyzerService.getPluginByAnalyzerId("10")).thenReturn(null);

        service.linkLegacyAnalyzersToTypes();

        verify(analyzerService, never()).save(any(Analyzer.class));
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private AnalyzerType createType(String id, String name, String pluginClassName, boolean active) {
        AnalyzerType type = new AnalyzerType();
        type.setId(id);
        type.setName(name);
        type.setPluginClassName(pluginClassName);
        type.setActive(active);
        type.setSysUserId("1");
        return type;
    }

    /** Fake class to use as a plugin class name in tests. */
    static class FakeLoadedPlugin implements AnalyzerImporterPlugin {
        @Override
        public boolean connect() {
            return false;
        }

        @Override
        public boolean isTargetAnalyzer(List<String> lines) {
            return false;
        }

        @Override
        public org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter getAnalyzerLineInserter() {
            return null;
        }
    }
}
