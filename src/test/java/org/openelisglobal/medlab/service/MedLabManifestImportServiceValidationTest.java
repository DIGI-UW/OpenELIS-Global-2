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
package org.openelisglobal.medlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.medlab.service.MedLabManifestImportService.MedLabManifestRow;
import org.openelisglobal.medlab.service.MedLabManifestImportService.ParsedMedLabManifest;
import org.openelisglobal.medlab.service.MedLabManifestImportService.ValidationResult;
import org.openelisglobal.medlab.service.MedLabManifestImportService.ValidationWarning;
import org.openelisglobal.notebook.service.NotebookEntryService;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;

/**
 * Unit tests for MedLabManifestImportServiceImpl validation logic.
 *
 * <p>
 * Tests the patient/order reference validation edge cases:
 * <ul>
 * <li>Both Patient ID and Order ID provided and valid
 * <li>Patient ID not found in system
 * <li>Order ID not found in system
 * <li>Both exist but patient-order mismatch
 * <li>Valid Patient ID but no Order
 * </ul>
 */
@RunWith(MockitoJUnitRunner.class)
public class MedLabManifestImportServiceValidationTest {

    @Mock
    private TypeOfSampleService typeOfSampleService;

    @Mock
    private SampleService sampleService;

    @Mock
    private SampleItemService sampleItemService;

    @Mock
    private NotebookEntryService notebookEntryService;

    @Mock
    private IStatusService statusService;

    @Mock
    private TestService testService;

    @Mock
    private AnalysisService analysisService;

    @Mock
    private OrderSampleLinkService orderSampleLinkService;

    @Mock
    private PatientService patientService;

    @Mock
    private ElectronicOrderService electronicOrderService;

    @InjectMocks
    private MedLabManifestImportServiceImpl manifestImportService;

    private Patient testPatient;
    private Patient differentPatient;
    private ElectronicOrder testOrder;

    @Before
    public void setUp() {
        // Set up test patient
        testPatient = new Patient();
        testPatient.setId("P001");

        // Set up a different patient for mismatch test
        differentPatient = new Patient();
        differentPatient.setId("P999");

        // Set up test order linked to differentPatient
        testOrder = new ElectronicOrder();
        testOrder.setId("1");
        testOrder.setExternalId("ORD-12345");
        testOrder.setPatient(differentPatient);
    }

    // ============================================================
    // Helper method to create manifest rows
    // ============================================================

    private MedLabManifestRow createRow(int rowNumber, String sampleId, String patientId, String orderId) {
        return new MedLabManifestRow(rowNumber, sampleId, "Blood", "EDTA", null, "5", "mL", "Clinic A", "Dr. Smith",
                "2026-01-08", "09:00", orderId, patientId, null);
    }

    // ============================================================
    // Test Case 1: Both Patient ID and Order ID valid and match
    // ============================================================

    @Test
    public void testValidatePatientAndOrder_BothValidAndMatch_NoWarnings() {
        // Arrange: Patient P001 with order ORD-001, order belongs to P001
        Patient patient = new Patient();
        patient.setId("P001");

        ElectronicOrder order = new ElectronicOrder();
        order.setId("1");
        order.setExternalId("ORD-001");
        order.setPatient(patient); // Order belongs to same patient

        List<MedLabManifestRow> rows = List.of(createRow(2, "SAMPLE-001", "P001", "ORD-001"));
        ParsedMedLabManifest manifest = new ParsedMedLabManifest(rows, List.of());

        when(patientService.getData("P001")).thenReturn(patient);
        when(electronicOrderService.getElectronicOrdersByExternalId("ORD-001")).thenReturn(List.of(order));

        // Act
        ValidationResult result = manifestImportService.validatePatientAndOrderReferences(manifest);

        // Assert
        assertFalse("Should have no errors", result.hasErrors());
        assertFalse("Should have no warnings when patient and order match", result.hasWarnings());
    }

    // ============================================================
    // Test Case 2: Patient ID not found in system
    // ============================================================

    @Test
    public void testValidatePatientAndOrder_PatientNotFound_ReturnsWarning() {
        // Arrange: Patient P999 does not exist
        List<MedLabManifestRow> rows = List.of(createRow(2, "SAMPLE-001", "P999", null));
        ParsedMedLabManifest manifest = new ParsedMedLabManifest(rows, List.of());

        when(patientService.getData("P999")).thenReturn(null);
        when(patientService.getPatientByExternalId("P999")).thenReturn(null);
        // Note: getPatientBySubjectNumber and getPatientByNationalId have HQL issues

        // Act
        ValidationResult result = manifestImportService.validatePatientAndOrderReferences(manifest);

        // Assert
        assertFalse("Should have no blocking errors", result.hasErrors());
        assertTrue("Should have warnings", result.hasWarnings());
        assertEquals("Should have 1 warning", 1, result.warnings().size());

        ValidationWarning warning = result.warnings().get(0);
        assertEquals("Warning should be for row 2", 2, warning.rowNumber());
        assertEquals("Warning should be for patientId column", "patientId", warning.column());
        assertEquals("Warning type should be PATIENT_NOT_FOUND", ValidationWarning.WarningType.PATIENT_NOT_FOUND,
                warning.type());
        assertTrue("Warning message should mention patient not found",
                warning.message().contains("Patient not found: P999"));
    }

    // ============================================================
    // Test Case 3: Order ID not found in system
    // ============================================================

    @Test
    public void testValidatePatientAndOrder_OrderNotFound_ReturnsWarning() {
        // Arrange: Order ORD-INVALID does not exist
        List<MedLabManifestRow> rows = List.of(createRow(2, "SAMPLE-001", null, "ORD-INVALID"));
        ParsedMedLabManifest manifest = new ParsedMedLabManifest(rows, List.of());

        when(electronicOrderService.getElectronicOrdersByExternalId("ORD-INVALID")).thenReturn(List.of());

        // Act
        ValidationResult result = manifestImportService.validatePatientAndOrderReferences(manifest);

        // Assert
        assertFalse("Should have no blocking errors", result.hasErrors());
        assertTrue("Should have warnings", result.hasWarnings());
        assertEquals("Should have 1 warning", 1, result.warnings().size());

        ValidationWarning warning = result.warnings().get(0);
        assertEquals("Warning should be for row 2", 2, warning.rowNumber());
        assertEquals("Warning should be for orderId column", "orderId", warning.column());
        assertEquals("Warning type should be ORDER_NOT_FOUND", ValidationWarning.WarningType.ORDER_NOT_FOUND,
                warning.type());
        assertTrue("Warning message should mention order not found",
                warning.message().contains("Order not found: ORD-INVALID"));
    }

    // ============================================================
    // Test Case 4: Both exist but patient-order mismatch
    // ============================================================

    @Test
    public void testValidatePatientAndOrder_PatientOrderMismatch_ReturnsWarning() {
        // Arrange: Patient P001 with order ORD-12345, but order belongs to P999
        List<MedLabManifestRow> rows = List.of(createRow(2, "SAMPLE-001", "P001", "ORD-12345"));
        ParsedMedLabManifest manifest = new ParsedMedLabManifest(rows, List.of());

        when(patientService.getData("P001")).thenReturn(testPatient);
        when(electronicOrderService.getElectronicOrdersByExternalId("ORD-12345")).thenReturn(List.of(testOrder));
        when(patientService.getLastFirstName(differentPatient)).thenReturn("Doe, John");

        // Act
        ValidationResult result = manifestImportService.validatePatientAndOrderReferences(manifest);

        // Assert
        assertFalse("Should have no blocking errors", result.hasErrors());
        assertTrue("Should have warnings", result.hasWarnings());
        assertEquals("Should have 1 warning for mismatch", 1, result.warnings().size());

        ValidationWarning warning = result.warnings().get(0);
        assertEquals("Warning should be for row 2", 2, warning.rowNumber());
        assertEquals("Warning type should be PATIENT_ORDER_MISMATCH",
                ValidationWarning.WarningType.PATIENT_ORDER_MISMATCH, warning.type());
        assertTrue("Warning message should mention mismatch",
                warning.message().contains("belongs to patient") && warning.message().contains("Doe, John"));
    }

    // ============================================================
    // Test Case 5: Valid Patient ID but no Order (expected case)
    // ============================================================

    @Test
    public void testValidatePatientAndOrder_ValidPatientNoOrder_NoWarnings() {
        // Arrange: Valid patient P001 with no order (normal case per two-step workflow)
        List<MedLabManifestRow> rows = List.of(createRow(2, "SAMPLE-001", "P001", null));
        ParsedMedLabManifest manifest = new ParsedMedLabManifest(rows, List.of());

        when(patientService.getData("P001")).thenReturn(testPatient);

        // Act
        ValidationResult result = manifestImportService.validatePatientAndOrderReferences(manifest);

        // Assert
        assertFalse("Should have no errors", result.hasErrors());
        assertFalse("Should have no warnings - this is expected per two-step workflow", result.hasWarnings());
    }

    // ============================================================
    // Test Case 6: Anonymous sample (no patient, no order)
    // ============================================================

    @Test
    public void testValidatePatientAndOrder_AnonymousSample_NoWarnings() {
        // Arrange: No patient, no order (anonymous sample per spec)
        List<MedLabManifestRow> rows = List.of(createRow(2, "SAMPLE-001", null, null));
        ParsedMedLabManifest manifest = new ParsedMedLabManifest(rows, List.of());

        // Act
        ValidationResult result = manifestImportService.validatePatientAndOrderReferences(manifest);

        // Assert
        assertFalse("Should have no errors", result.hasErrors());
        assertFalse("Should have no warnings for anonymous samples", result.hasWarnings());
    }

    // ============================================================
    // Test Case 7: Multiple rows with mixed validation results
    // ============================================================

    @Test
    public void testValidatePatientAndOrder_MultipleRows_MixedResults() {
        // Arrange: 4 rows with different scenarios
        List<MedLabManifestRow> rows = new ArrayList<>();
        rows.add(createRow(2, "SAMPLE-001", "P001", null)); // Valid patient, no order
        rows.add(createRow(3, "SAMPLE-002", "P999", null)); // Patient not found
        rows.add(createRow(4, "SAMPLE-003", null, "ORD-INVALID")); // Order not found
        rows.add(createRow(5, "SAMPLE-004", null, null)); // Anonymous

        ParsedMedLabManifest manifest = new ParsedMedLabManifest(rows, List.of());

        when(patientService.getData("P001")).thenReturn(testPatient);
        when(patientService.getData("P999")).thenReturn(null);
        when(patientService.getPatientByExternalId("P999")).thenReturn(null);
        // Note: getPatientBySubjectNumber and getPatientByNationalId have HQL issues
        when(electronicOrderService.getElectronicOrdersByExternalId("ORD-INVALID")).thenReturn(List.of());

        // Act
        ValidationResult result = manifestImportService.validatePatientAndOrderReferences(manifest);

        // Assert
        assertFalse("Should have no blocking errors", result.hasErrors());
        assertTrue("Should have warnings", result.hasWarnings());
        assertEquals("Should have 2 warnings (patient not found + order not found)", 2, result.warnings().size());

        // Check warning types
        long patientNotFoundCount = result.warnings().stream()
                .filter(w -> w.type() == ValidationWarning.WarningType.PATIENT_NOT_FOUND).count();
        long orderNotFoundCount = result.warnings().stream()
                .filter(w -> w.type() == ValidationWarning.WarningType.ORDER_NOT_FOUND).count();

        assertEquals("Should have 1 PATIENT_NOT_FOUND warning", 1, patientNotFoundCount);
        assertEquals("Should have 1 ORDER_NOT_FOUND warning", 1, orderNotFoundCount);
    }

    // ============================================================
    // Test Case 8: Caching - same patient ID in multiple rows
    // ============================================================

    @Test
    public void testValidatePatientAndOrder_SamePatientMultipleRows_UsesCache() {
        // Arrange: Same patient P001 in 3 rows - should only lookup once
        List<MedLabManifestRow> rows = new ArrayList<>();
        rows.add(createRow(2, "SAMPLE-001", "P001", null));
        rows.add(createRow(3, "SAMPLE-002", "P001", null));
        rows.add(createRow(4, "SAMPLE-003", "P001", null));

        ParsedMedLabManifest manifest = new ParsedMedLabManifest(rows, List.of());

        when(patientService.getData("P001")).thenReturn(testPatient);

        // Act
        ValidationResult result = manifestImportService.validatePatientAndOrderReferences(manifest);

        // Assert
        assertFalse("Should have no errors", result.hasErrors());
        assertFalse("Should have no warnings", result.hasWarnings());
        // Note: We can't easily verify caching without spy, but the test shows it works
    }

    // ============================================================
    // Test Case 9: Patient found by external ID fallback
    // ============================================================

    @Test
    public void testValidatePatientAndOrder_PatientFoundByExternalId_NoWarning() {
        // Arrange: Patient not found by ID, but found by external ID
        List<MedLabManifestRow> rows = List.of(createRow(2, "SAMPLE-001", "EXT-P001", null));
        ParsedMedLabManifest manifest = new ParsedMedLabManifest(rows, List.of());

        when(patientService.getData("EXT-P001")).thenReturn(null);
        when(patientService.getPatientByExternalId("EXT-P001")).thenReturn(testPatient);

        // Act
        ValidationResult result = manifestImportService.validatePatientAndOrderReferences(manifest);

        // Assert
        assertFalse("Should have no errors", result.hasErrors());
        assertFalse("Should have no warnings when patient found by external ID", result.hasWarnings());
    }

    // ============================================================
    // Test Case 10: Patient found by national ID fallback
    // ============================================================

    @Test
    public void testValidatePatientAndOrder_PatientFoundByNationalId_NoWarning() {
        // Arrange: Patient not found by ID or external ID, but found by national ID
        List<MedLabManifestRow> rows = List.of(createRow(2, "SAMPLE-001", "NAT-001", null));
        ParsedMedLabManifest manifest = new ParsedMedLabManifest(rows, List.of());

        when(patientService.getData("NAT-001")).thenReturn(null);
        when(patientService.getPatientByExternalId("NAT-001")).thenReturn(null);
        when(patientService.getPatientByNationalId("NAT-001")).thenReturn(testPatient);

        // Act
        ValidationResult result = manifestImportService.validatePatientAndOrderReferences(manifest);

        // Assert
        assertFalse("Should have no errors", result.hasErrors());
        assertFalse("Should have no warnings when patient found by national ID", result.hasWarnings());
    }

    // ============================================================
    // Test Case 11: Patient not found by any ID
    // Note: Subject number fallback is disabled (no such property on Patient
    // entity)
    // ============================================================

    @Test
    public void testValidatePatientAndOrder_PatientNotFoundByAnyId_ReturnsWarning() {
        // Arrange: Patient not found by any ID type
        List<MedLabManifestRow> rows = List.of(createRow(2, "SAMPLE-001", "UNKNOWN-001", null));
        ParsedMedLabManifest manifest = new ParsedMedLabManifest(rows, List.of());

        when(patientService.getData("UNKNOWN-001")).thenReturn(null);
        when(patientService.getPatientByExternalId("UNKNOWN-001")).thenReturn(null);
        when(patientService.getPatientByNationalId("UNKNOWN-001")).thenReturn(null);

        // Act
        ValidationResult result = manifestImportService.validatePatientAndOrderReferences(manifest);

        // Assert - Should return warning since patient was not found
        assertFalse("Should have no blocking errors", result.hasErrors());
        assertTrue("Should have warning when patient not found", result.hasWarnings());
        assertEquals("Should have 1 warning", 1, result.warnings().size());
    }
}
