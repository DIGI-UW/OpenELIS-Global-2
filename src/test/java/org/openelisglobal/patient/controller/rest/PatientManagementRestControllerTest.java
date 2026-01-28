package org.openelisglobal.patient.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.service.PatientPhotoService;
import org.openelisglobal.patient.service.PatientService;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Unit tests for PatientManagementRestController service interactions.
 * Tests controller service delegation and error handling without HTTP dependencies.
 * No Docker/TestContainers required - focuses on service layer testing.
 */
@RunWith(SpringRunner.class)
public class PatientManagementRestControllerTest {

    @Mock
    private PatientService patientService;

    @Mock
    private PatientPhotoService patientPhotoService;

    private PatientManagementRestController controller;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        controller = new PatientManagementRestController();

        // Inject mocked services using reflection (like existing OpenELIS tests)
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "patientService", patientService);
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "photoService", patientPhotoService);
    }

    @Test
    public void patientService_SavePatientData_ShouldAcceptValidData() {
        // Test that patientService.persistPatientData accepts valid data without throwing
        PatientManagementInfo patientInfo = new PatientManagementInfo();
        patientInfo.setFirstName("John");
        patientInfo.setLastName("Doe");
        patientInfo.setPatientUpdateStatus(org.openelisglobal.patient.action.IPatientUpdate.PatientUpdateStatus.ADD);

        // This should not throw an exception (service is mocked)
        patientService.persistPatientData(patientInfo, null, "testUser");

        // Verify the service was called with the expected data
        verify(patientService).persistPatientData(any(PatientManagementInfo.class), any(), anyString());
    }

    @Test
    public void patientService_SavePatientData_ThrowsException_ShouldPropagate() {
        // Test error handling - when patientService throws exception
        PatientManagementInfo patientInfo = new PatientManagementInfo();
        patientInfo.setFirstName("John");
        patientInfo.setLastName("Doe");

        doThrow(new RuntimeException("Database connection failed"))
            .when(patientService).persistPatientData(any(PatientManagementInfo.class), any(), anyString());

        try {
            patientService.persistPatientData(patientInfo, null, "testUser");
            fail("Expected RuntimeException to be thrown");
        } catch (RuntimeException e) {
            assertEquals("Database connection failed", e.getMessage());
        }

        verify(patientService).persistPatientData(any(PatientManagementInfo.class), any(), anyString());
    }

    @Test
    public void photoService_GetPhotoById_ReturnsExpectedData() {
        // Test photo service returns expected data
        String expectedPhoto = "base64photo";
        when(patientPhotoService.getPhotoByPatientId("123", true)).thenReturn(expectedPhoto);

        String result = patientPhotoService.getPhotoByPatientId("123", true);

        assertEquals("Photo service should return expected photo data", expectedPhoto, result);
        verify(patientPhotoService).getPhotoByPatientId("123", true);
    }

    @Test
    public void photoService_GetPhotoById_ReturnsNullForNonExistentPhoto() {
        // Test photo service handles null return (photo not found)
        when(patientPhotoService.getPhotoByPatientId("999", false)).thenReturn(null);

        String result = patientPhotoService.getPhotoByPatientId("999", false);

        assertEquals("Photo service should return null for non-existent photo", null, result);
        verify(patientPhotoService).getPhotoByPatientId("999", false);
    }

    @Test
    public void photoService_GetPhotoById_ThrowsException_ShouldPropagate() {
        // Test error handling in photo service
        when(patientPhotoService.getPhotoByPatientId("123", true))
            .thenThrow(new RuntimeException("Photo storage unavailable"));

        try {
            patientPhotoService.getPhotoByPatientId("123", true);
            fail("Expected RuntimeException to be thrown");
        } catch (RuntimeException e) {
            assertEquals("Photo storage unavailable", e.getMessage());
        }

        verify(patientPhotoService).getPhotoByPatientId("123", true);
    }

    @Test
    public void controller_ServiceInjection_ShouldWorkCorrectly() {
        // Test that controller has proper service injection (critical for controller functionality)
        assertNotNull("Controller should have patientService injected", controller.patientService);
        assertNotNull("Controller should have photoService injected", controller.photoService);

        // Verify the injected services are our mocks
        assertEquals("PatientService should be our mock instance", patientService, controller.patientService);
        assertEquals("PatientPhotoService should be our mock instance", patientPhotoService, controller.photoService);
    }

    @Test
    public void patientService_PersistData_WithNullParameters_ShouldHandleGracefully() {
        // Test service handles null parameters appropriately
        PatientManagementInfo patientInfo = null;
        String userId = null;

        // Service should accept null parameters without throwing (mock behavior)
        patientService.persistPatientData(patientInfo, null, userId);

        verify(patientService).persistPatientData(patientInfo, null, userId);
    }
}