package org.openelisglobal.coldstorage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.coldstorage.service.ThresholdProfileService;
import org.openelisglobal.coldstorage.valueholder.FreezerThresholdProfile;
import org.openelisglobal.coldstorage.valueholder.ThresholdProfile;
import org.springframework.beans.factory.annotation.Autowired;

public class ThresholdProfileServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    ThresholdProfileService thresholdProfileService;

    @Before
    public void setup() throws Exception {
        // Load user data first (required for created_by foreign key)
        executeDataSetWithStateManagement("testdata/user-role.xml");
        executeDataSetWithStateManagement("testdata/threshold_profile.xml");
    }

    @Test
    public void verifyTestData() {
        List<ThresholdProfile> profiles = thresholdProfileService.listProfiles();

        assertNotNull("Profile list should not be null", profiles);
        assertFalse("Profile list should not be empty", profiles.isEmpty());

        profiles.forEach(profile -> {
            assertNotNull("Profile ID should not be null", profile.getId());
            assertNotNull("Profile name should not be null", profile.getName());
        });
    }

    @Test
    public void listProfiles_shouldReturnAllProfiles() {
        List<ThresholdProfile> profiles = thresholdProfileService.listProfiles();

        assertNotNull("Profiles list should not be null", profiles);
        assertEquals("Should have 2 profiles", 2, profiles.size());
    }

    @Test
    public void createProfile_shouldCreateNewProfile() {
        ThresholdProfile newProfile = new ThresholdProfile();
        newProfile.setName("Refrigerator Profile");
        newProfile.setDescription("Standard refrigerator thresholds");
        newProfile.setWarningMin(new BigDecimal("2.0"));
        newProfile.setWarningMax(new BigDecimal("6.0"));
        newProfile.setCriticalMin(new BigDecimal("0.0"));
        newProfile.setCriticalMax(new BigDecimal("8.0"));
        newProfile.setMinExcursionMinutes(15);

        ThresholdProfile createdProfile = thresholdProfileService.createProfile(newProfile, "admin");

        assertNotNull("Created profile should not be null", createdProfile);
        assertNotNull("Created profile should have ID", createdProfile.getId());
        assertEquals("Refrigerator Profile", createdProfile.getName());
        assertEquals("Standard refrigerator thresholds", createdProfile.getDescription());
        assertEquals(0, new BigDecimal("2.0").compareTo(createdProfile.getWarningMin()));
        assertEquals(0, new BigDecimal("6.0").compareTo(createdProfile.getWarningMax()));
    }

    @Test
    public void createProfile_shouldCreateProfileWithHumidityThresholds() {
        ThresholdProfile newProfile = new ThresholdProfile();
        newProfile.setName("Humidity Controlled Profile");
        newProfile.setDescription("Profile with humidity controls");
        newProfile.setWarningMin(new BigDecimal("-82.0"));
        newProfile.setWarningMax(new BigDecimal("-78.0"));
        newProfile.setHumidityWarningMin(new BigDecimal("40.0"));
        newProfile.setHumidityWarningMax(new BigDecimal("60.0"));

        ThresholdProfile createdProfile = thresholdProfileService.createProfile(newProfile, "admin");

        assertNotNull("Created profile should not be null", createdProfile);
        assertEquals(0, new BigDecimal("40.0").compareTo(createdProfile.getHumidityWarningMin()));
        assertEquals(0, new BigDecimal("60.0").compareTo(createdProfile.getHumidityWarningMax()));
    }

    @Test
    public void assignProfile_shouldAssignProfileToFreezer() {
        Long freezerId = 100L;
        Long profileId = 101L;
        OffsetDateTime effectiveStart = OffsetDateTime.now();
        OffsetDateTime effectiveEnd = null;
        boolean isDefault = true;

        FreezerThresholdProfile assignment = thresholdProfileService.assignProfile(freezerId, profileId, effectiveStart,
                effectiveEnd, isDefault);

        assertNotNull("Assignment should not be null", assignment);
        assertNotNull("Assignment should have ID", assignment.getId());
        assertEquals("Freezer ID should match", freezerId, assignment.getFreezer().getId());
        assertEquals("Profile ID should match", profileId, assignment.getThresholdProfile().getId());
        assertTrue("Should be default profile", assignment.getIsDefault());
    }

    @Test
    public void assignProfile_shouldAssignProfileWithEffectiveDates() {
        Long freezerId = 101L;
        Long profileId = 100L;
        OffsetDateTime effectiveStart = OffsetDateTime.now();
        OffsetDateTime effectiveEnd = OffsetDateTime.now().plusMonths(6);
        boolean isDefault = false;

        FreezerThresholdProfile assignment = thresholdProfileService.assignProfile(freezerId, profileId, effectiveStart,
                effectiveEnd, isDefault);

        assertNotNull("Assignment should not be null", assignment);
        assertNotNull("Effective start should be set", assignment.getEffectiveStart());
        assertNotNull("Effective end should be set", assignment.getEffectiveEnd());
        assertFalse("Should not be default profile", assignment.getIsDefault());
    }

    @Test
    public void assignProfile_shouldAssignMultipleProfilesToSameFreezer() {
        Long freezerId = 100L;
        Long profileId1 = 100L;
        Long profileId2 = 101L;

        OffsetDateTime start1 = OffsetDateTime.now().minusMonths(3);
        OffsetDateTime end1 = OffsetDateTime.now();
        OffsetDateTime start2 = OffsetDateTime.now();

        FreezerThresholdProfile assignment1 = thresholdProfileService.assignProfile(freezerId, profileId1, start1, end1,
                false);

        FreezerThresholdProfile assignment2 = thresholdProfileService.assignProfile(freezerId, profileId2, start2, null,
                true);

        assertNotNull("First assignment should not be null", assignment1);
        assertNotNull("Second assignment should not be null", assignment2);
        assertFalse("First assignment should not be default", assignment1.getIsDefault());
        assertTrue("Second assignment should be default", assignment2.getIsDefault());
    }

    @Test
    public void createProfile_shouldCreateProfileWithAllThresholdTypes() {
        ThresholdProfile newProfile = new ThresholdProfile();
        newProfile.setName("Complete Threshold Profile");
        newProfile.setDescription("Profile with all threshold types");
        newProfile.setWarningMin(new BigDecimal("-82.0"));
        newProfile.setWarningMax(new BigDecimal("-78.0"));
        newProfile.setCriticalMin(new BigDecimal("-85.0"));
        newProfile.setCriticalMax(new BigDecimal("-75.0"));
        newProfile.setHumidityWarningMin(new BigDecimal("30.0"));
        newProfile.setHumidityWarningMax(new BigDecimal("70.0"));
        newProfile.setMinExcursionMinutes(5);
        newProfile.setMaxDurationMinutes(60);

        ThresholdProfile createdProfile = thresholdProfileService.createProfile(newProfile, "admin");

        assertNotNull("Created profile should not be null", createdProfile);
        assertEquals(0, new BigDecimal("-82.0").compareTo(createdProfile.getWarningMin()));
        assertEquals(0, new BigDecimal("-78.0").compareTo(createdProfile.getWarningMax()));
        assertEquals(0, new BigDecimal("-85.0").compareTo(createdProfile.getCriticalMin()));
        assertEquals(0, new BigDecimal("-75.0").compareTo(createdProfile.getCriticalMax()));
        assertEquals(0, new BigDecimal("30.0").compareTo(createdProfile.getHumidityWarningMin()));
        assertEquals(0, new BigDecimal("70.0").compareTo(createdProfile.getHumidityWarningMax()));
        assertEquals(Integer.valueOf(5), createdProfile.getMinExcursionMinutes());
        assertEquals(Integer.valueOf(60), createdProfile.getMaxDurationMinutes());
    }
}
