package org.openelisglobal.tb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.tb.service.TbSampleRegistrationService;
import org.openelisglobal.tb.valueholder.TbEnums.SpecimenType;
import org.openelisglobal.tb.valueholder.TbSampleRegistration;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for TB Sample Registration service. Tests ORM mappings and
 * basic CRUD operations.
 */
public class TbSampleRegistrationServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TbSampleRegistrationService tbSampleRegistrationService;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/tb-sample-registration.xml");
    }

    @Test
    public void verifyTestData() {
        List<TbSampleRegistration> registrations = tbSampleRegistrationService.getAll();

        assertNotNull("Registration list should not be null", registrations);
        assertFalse("Registration list should not be empty", registrations.isEmpty());

        registrations.forEach(reg -> {
            assertNotNull("Registration ID should not be null", reg.getId());
            assertNotNull("Specimen type should not be null", reg.getSpecimenType());
        });
    }

    @Test
    public void findBySampleItemId_shouldReturnRegistrationWhenExists() {
        String sampleItemId = "100";

        Optional<TbSampleRegistration> regOpt = tbSampleRegistrationService.findBySampleItemId(sampleItemId);

        assertTrue("Registration should be found", regOpt.isPresent());
        assertEquals("Specimen type should be SPUTUM", SpecimenType.SPUTUM, regOpt.get().getSpecimenType());
    }

    @Test
    public void findBySampleItemId_shouldReturnEmptyWhenNotExists() {
        String sampleItemId = "999999";

        Optional<TbSampleRegistration> regOpt = tbSampleRegistrationService.findBySampleItemId(sampleItemId);

        assertFalse("Registration should not be found", regOpt.isPresent());
    }

    @Test
    public void findByDocumentNumber_shouldReturnMatchingRegistrations() {
        String documentNumber = "TB-DOC-001";

        List<TbSampleRegistration> registrations = tbSampleRegistrationService.findByDocumentNumber(documentNumber);

        assertNotNull("Registrations list should not be null", registrations);
        assertFalse("Should find matching registrations", registrations.isEmpty());

        registrations.forEach(reg -> {
            assertEquals("Document number should match", documentNumber, reg.getDocumentNumber());
        });
    }

    @Test
    public void findByReferringFacility_shouldReturnMatchingRegistrations() {
        String facility = "Test Health Center";

        List<TbSampleRegistration> registrations = tbSampleRegistrationService.findByReferringFacility(facility);

        assertNotNull("Registrations list should not be null", registrations);
        assertFalse("Should find matching registrations", registrations.isEmpty());

        registrations.forEach(reg -> {
            assertEquals("Referring facility should match", facility, reg.getReferringFacility());
        });
    }

    @Test
    public void insert_shouldCreateNewRegistration() {
        TbSampleRegistration newReg = new TbSampleRegistration();
        newReg.setSpecimenType(SpecimenType.BODY_FLUID);
        newReg.setDocumentNumber("TB-DOC-NEW");
        newReg.setReferringFacility("New Facility");
        newReg.setSysUserId("1");

        Integer id = tbSampleRegistrationService.insert(newReg);

        assertNotNull("Inserted ID should not be null", id);

        TbSampleRegistration retrieved = tbSampleRegistrationService.get(id);
        assertNotNull("Retrieved registration should not be null", retrieved);
        assertEquals("Specimen type should match", SpecimenType.BODY_FLUID, retrieved.getSpecimenType());
        assertEquals("Document number should match", "TB-DOC-NEW", retrieved.getDocumentNumber());
    }

    @Test
    public void update_shouldModifyExistingRegistration() {
        // Get an existing registration
        List<TbSampleRegistration> registrations = tbSampleRegistrationService.getAll();
        assertFalse("Should have registrations to update", registrations.isEmpty());

        TbSampleRegistration reg = registrations.get(0);
        Integer originalId = reg.getId();
        String newFacility = "Updated Facility";

        reg.setReferringFacility(newFacility);
        reg.setSysUserId("1");

        TbSampleRegistration updated = tbSampleRegistrationService.update(reg);

        assertEquals("ID should not change", originalId, updated.getId());
        assertEquals("Referring facility should be updated", newFacility, updated.getReferringFacility());
    }
}
