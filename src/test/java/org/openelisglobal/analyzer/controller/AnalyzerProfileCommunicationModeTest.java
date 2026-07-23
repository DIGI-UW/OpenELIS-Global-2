package org.openelisglobal.analyzer.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Map;
import org.junit.Test;
import org.openelisglobal.analyzer.valueholder.CommunicationMode;

/**
 * A profile is the source of truth for a profile-created analyzer's
 * communication mode. The create path reads it via
 * {@link AnalyzerRestController#communicationModeFromProfile} and applies it
 * when the form didn't set one — without this, profile-created analyzers
 * default to a non-dispatchable mode and never appear in the LIS-initiated
 * dispatch UI.
 */
public class AnalyzerProfileCommunicationModeTest {

    @Test
    public void readsCommunicationModeFromProfileBlock() {
        Map<String, Object> profile = Map.of("communication", Map.of("mode", "BOTH"));
        assertEquals(CommunicationMode.BOTH, AnalyzerRestController.communicationModeFromProfile(profile));
    }

    @Test
    public void readsLegacyTopLevelCommunicationMode() {
        Map<String, Object> profile = Map.of("communication_mode", "LIS_INITIATED");
        assertEquals(CommunicationMode.LIS_INITIATED, AnalyzerRestController.communicationModeFromProfile(profile));
    }

    @Test
    public void nullWhenProfileDeclaresNoCommunicationMode() {
        assertNull(AnalyzerRestController.communicationModeFromProfile(Map.of("protocol", Map.of("name", "ASTM"))));
    }

    @Test
    public void nullWhenModeUnrecognized() {
        Map<String, Object> profile = Map.of("communication", Map.of("mode", "NONSENSE"));
        assertNull(AnalyzerRestController.communicationModeFromProfile(profile));
    }
}
