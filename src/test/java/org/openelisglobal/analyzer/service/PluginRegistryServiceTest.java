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

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for PluginRegistryService.
 *
 * <p>
 * Tests the plugin name derivation and protocol detection logic without
 * requiring Spring context or database.
 */
public class PluginRegistryServiceTest {

    private PluginRegistryService service;

    @Before
    public void setUp() {
        service = new PluginRegistryService();
    }

    @Test
    public void testDerivePluginName_HoribaMicros60_ReturnsHumanReadable() {
        String result = service.derivePluginName("org.openelisglobal.plugins.HoribaMicros60Analyzer");
        assertEquals("Horiba Micros 60", result);
    }

    @Test
    public void testDerivePluginName_SysmexXNL_ReturnsHumanReadable() {
        String result = service.derivePluginName("org.openelisglobal.plugins.SysmexXNLAnalyzer");
        assertEquals("Sysmex XNL", result);
    }

    @Test
    public void testDerivePluginName_GenericASTM_ReturnsHumanReadable() {
        String result = service.derivePluginName("org.openelisglobal.plugins.GenericASTMAnalyzer");
        assertEquals("Generic ASTM", result);
    }

    @Test
    public void testDerivePluginName_GenericHL7_ReturnsHumanReadable() {
        // HL7 stays together (digit after uppercase) - it's an abbreviation
        String result = service.derivePluginName("org.openelisglobal.plugins.GenericHL7Analyzer");
        assertEquals("Generic HL7", result);
    }

    @Test
    public void testDerivePluginName_MindrayBC5380_ReturnsHumanReadable() {
        String result = service.derivePluginName("org.openelisglobal.plugins.MindrayBC5380Analyzer");
        assertEquals("Mindray BC 5380", result);
    }

    @Test
    public void testDerivePluginName_GeneXpert_ReturnsHumanReadable() {
        String result = service.derivePluginName("org.openelisglobal.plugins.GeneXpertAnalyzer");
        assertEquals("Gene Xpert", result);
    }

    @Test
    public void testDerivePluginName_HoribaPentra60_ReturnsHumanReadable() {
        String result = service.derivePluginName("org.openelisglobal.plugins.HoribaPentra60Analyzer");
        assertEquals("Horiba Pentra 60", result);
    }

    @Test
    public void testDerivePluginName_SysmexXN1000_ReturnsHumanReadable() {
        String result = service.derivePluginName("org.openelisglobal.plugins.SysmexXN1000Analyzer");
        assertEquals("Sysmex XN 1000", result);
    }

    @Test
    public void testDerivePluginName_QuantStudio3Importer_ReturnsHumanReadable() {
        String result = service.derivePluginName("org.openelisglobal.plugins.QuantStudio3Importer");
        assertEquals("Quant Studio 3", result);
    }

    @Test
    public void testDerivePluginName_CobasC311_ReturnsHumanReadable() {
        String result = service.derivePluginName("org.openelisglobal.plugins.CobasC311Analyzer");
        assertEquals("Cobas C 311", result);
    }

    @Test
    public void testDerivePluginName_ABXMicros60Plugin_ReturnsHumanReadable() {
        String result = service.derivePluginName("org.openelisglobal.plugins.ABXMicros60Plugin");
        assertEquals("ABX Micros 60", result);
    }

    @Test
    public void testDerivePluginName_SimpleClassName_ReturnsName() {
        String result = service.derivePluginName("org.test.TestAnalyzer");
        assertEquals("Test", result);
    }

    @Test
    public void testDerivePluginName_NestedPackage_ReturnsSimpleName() {
        String result = service.derivePluginName("org.openelisglobal.plugins.analyzer.genericastm.GenericASTMAnalyzer");
        assertEquals("Generic ASTM", result);
    }

    @Test
    public void testDetectProtocol_GenericASTM_ReturnsASTM() {
        String result = service.detectProtocol("org.openelisglobal.plugins.GenericASTMAnalyzer");
        assertEquals("ASTM", result);
    }

    @Test
    public void testDetectProtocol_GenericHL7_ReturnsHL7() {
        String result = service.detectProtocol("org.openelisglobal.plugins.GenericHL7Analyzer");
        assertEquals("HL7", result);
    }

    @Test
    public void testDetectProtocol_HoribaMicros60_ReturnsASTM() {
        String result = service.detectProtocol("org.openelisglobal.plugins.HoribaMicros60Analyzer");
        assertEquals("ASTM", result);
    }

    @Test
    public void testDetectProtocol_MindrayBC5380HL7_ReturnsHL7() {
        String result = service.detectProtocol("org.openelisglobal.plugins.MindrayBC5380HL7Analyzer");
        assertEquals("HL7", result);
    }

    @Test
    public void testDetectProtocol_QuantStudio3FileImporter_ReturnsFILE() {
        String result = service.detectProtocol("org.openelisglobal.plugins.QuantStudio3FileImporter");
        assertEquals("FILE", result);
    }

    @Test
    public void testDetectProtocol_GeneXpertCSVImporter_ReturnsFILE() {
        String result = service.detectProtocol("org.openelisglobal.plugins.GeneXpertCSVImporter");
        assertEquals("FILE", result);
    }

    @Test
    public void testDetectProtocol_SysmexXNL_ReturnsASTM() {
        String result = service.detectProtocol("org.openelisglobal.plugins.SysmexXNLAnalyzer");
        assertEquals("ASTM", result);
    }

    @Test
    public void testDetectProtocol_AmbiguousName_DefaultsToASTM() {
        // When the name doesn't clearly indicate protocol, default to ASTM
        String result = service.detectProtocol("org.test.SomeRandomAnalyzer");
        assertEquals("ASTM", result);
    }

    @Test
    public void testDetectProtocol_ASTMFileHybrid_ReturnsASTM() {
        // If name contains both "astm" and "file", prefer ASTM
        String result = service.detectProtocol("org.test.ASTMFileAnalyzer");
        assertEquals("ASTM", result);
    }

    @Test
    public void testDerivePluginName_MultipleConsecutiveNumbers_SpacesCorrectly() {
        String result = service.derivePluginName("org.test.Model1234ABCAnalyzer");
        assertEquals("Model 1234 ABC", result);
    }

    @Test
    public void testDerivePluginName_AllUppercase_NoExtraSpaces() {
        String result = service.derivePluginName("org.test.ABCAnalyzer");
        assertEquals("ABC", result);
    }

    @Test
    public void testDerivePluginName_StartsWithNumber_HandlesCorrectly() {
        // Edge case: name starting with number (unlikely but possible)
        String result = service.derivePluginName("org.test.100XAnalyzer");
        assertEquals("100 X", result);
    }
}
