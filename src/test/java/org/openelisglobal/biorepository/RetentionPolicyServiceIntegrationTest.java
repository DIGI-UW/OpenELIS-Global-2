package org.openelisglobal.biorepository;

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.biorepository.service.RetentionPolicyService;
import org.openelisglobal.biorepository.valueholder.RetentionPolicy;
import org.openelisglobal.biorepository.valueholder.RetentionPolicy.PeriodUnit;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for RetentionPolicyService.
 *
 * Tests cover: - Basic CRUD operations - Policy matching (project takes
 * precedence over sample type) - Expiry date calculation with various period
 * units - CSV import with edge cases - Period string parsing (with and without
 * units)
 */
public class RetentionPolicyServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private RetentionPolicyService retentionPolicyService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private SystemUserService systemUserService;

    private SystemUser testUser;
    private TypeOfSample testSampleType;
    private String testSysUserId;

    @Before
    public void setUp() {
        // Setup test user
        testUser = systemUserService.get("1");
        if (testUser == null) {
            testUser = new SystemUser();
            testUser.setLoginName("test_retention_user");
            testUser.setFirstName("Test");
            testUser.setLastName("Retention User");
            testUser.setIsActive("Y");
            testUser.setSysUserId("1");
            systemUserService.save(testUser);
        }
        testSysUserId = testUser.getId().toString();

        // Setup test sample type
        var sampleTypes = typeOfSampleService.getAll();
        if (!sampleTypes.isEmpty()) {
            testSampleType = sampleTypes.get(0);
        } else {
            testSampleType = new TypeOfSample();
            testSampleType.setDescription("Test Blood");
            testSampleType.setDomain("H");
            typeOfSampleService.save(testSampleType);
        }
    }

    // ========== PERIOD PARSING TESTS ==========

    @Test
    public void testParsePeriodString_YearsExplicit() {
        Object[] result = RetentionPolicy.parsePeriodString("5 years");

        assertEquals("Value should be 5", 5, result[0]);
        assertEquals("Unit should be YEARS", PeriodUnit.YEARS, result[1]);
    }

    @Test
    public void testParsePeriodString_YearSingular() {
        Object[] result = RetentionPolicy.parsePeriodString("1 year");

        assertEquals("Value should be 1", 1, result[0]);
        assertEquals("Unit should be YEARS", PeriodUnit.YEARS, result[1]);
    }

    @Test
    public void testParsePeriodString_MonthsExplicit() {
        Object[] result = RetentionPolicy.parsePeriodString("18 months");

        assertEquals("Value should be 18", 18, result[0]);
        assertEquals("Unit should be MONTHS", PeriodUnit.MONTHS, result[1]);
    }

    @Test
    public void testParsePeriodString_MonthSingular() {
        Object[] result = RetentionPolicy.parsePeriodString("1 month");

        assertEquals("Value should be 1", 1, result[0]);
        assertEquals("Unit should be MONTHS", PeriodUnit.MONTHS, result[1]);
    }

    @Test
    public void testParsePeriodString_DaysExplicit() {
        Object[] result = RetentionPolicy.parsePeriodString("30 days");

        assertEquals("Value should be 30", 30, result[0]);
        assertEquals("Unit should be DAYS", PeriodUnit.DAYS, result[1]);
    }

    @Test
    public void testParsePeriodString_DaySingular() {
        Object[] result = RetentionPolicy.parsePeriodString("1 day");

        assertEquals("Value should be 1", 1, result[0]);
        assertEquals("Unit should be DAYS", PeriodUnit.DAYS, result[1]);
    }

    @Test
    public void testParsePeriodString_NumberOnlyDefaultsToYears() {
        Object[] result = RetentionPolicy.parsePeriodString("5");

        assertEquals("Value should be 5", 5, result[0]);
        assertEquals("Unit should default to YEARS", PeriodUnit.YEARS, result[1]);
    }

    @Test
    public void testParsePeriodString_NumberOnlyWithSpaces() {
        Object[] result = RetentionPolicy.parsePeriodString("  10  ");

        assertEquals("Value should be 10", 10, result[0]);
        assertEquals("Unit should default to YEARS", PeriodUnit.YEARS, result[1]);
    }

    @Test
    public void testParsePeriodString_CaseInsensitive() {
        Object[] yearsUpper = RetentionPolicy.parsePeriodString("5 YEARS");
        Object[] monthsMixed = RetentionPolicy.parsePeriodString("6 Months");
        Object[] daysLower = RetentionPolicy.parsePeriodString("7 days");

        assertEquals("YEARS should be recognized", PeriodUnit.YEARS, yearsUpper[1]);
        assertEquals("Months should be recognized", PeriodUnit.MONTHS, monthsMixed[1]);
        assertEquals("days should be recognized", PeriodUnit.DAYS, daysLower[1]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParsePeriodString_EmptyString_ThrowsException() {
        RetentionPolicy.parsePeriodString("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParsePeriodString_NullString_ThrowsException() {
        RetentionPolicy.parsePeriodString(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParsePeriodString_InvalidNumber_ThrowsException() {
        RetentionPolicy.parsePeriodString("abc years");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParsePeriodString_UnknownUnit_ThrowsException() {
        RetentionPolicy.parsePeriodString("5 weeks");
    }

    // ========== PERIOD DISPLAY TESTS ==========

    @Test
    public void testGetPeriodDisplay_PluralYears() {
        RetentionPolicy policy = createTestPolicy("Test", 5, PeriodUnit.YEARS);

        assertEquals("5 years", policy.getPeriodDisplay());
    }

    @Test
    public void testGetPeriodDisplay_SingularYear() {
        RetentionPolicy policy = createTestPolicy("Test", 1, PeriodUnit.YEARS);

        assertEquals("1 year", policy.getPeriodDisplay());
    }

    @Test
    public void testGetPeriodDisplay_PluralMonths() {
        RetentionPolicy policy = createTestPolicy("Test", 18, PeriodUnit.MONTHS);

        assertEquals("18 months", policy.getPeriodDisplay());
    }

    @Test
    public void testGetPeriodDisplay_SingularMonth() {
        RetentionPolicy policy = createTestPolicy("Test", 1, PeriodUnit.MONTHS);

        assertEquals("1 month", policy.getPeriodDisplay());
    }

    @Test
    public void testGetPeriodDisplay_PluralDays() {
        RetentionPolicy policy = createTestPolicy("Test", 30, PeriodUnit.DAYS);

        assertEquals("30 days", policy.getPeriodDisplay());
    }

    @Test
    public void testGetPeriodDisplay_SingularDay() {
        RetentionPolicy policy = createTestPolicy("Test", 1, PeriodUnit.DAYS);

        assertEquals("1 day", policy.getPeriodDisplay());
    }

    // ========== EXPIRY DATE CALCULATION TESTS ==========

    @Test
    public void testCalculateExpiryDate_Years() {
        RetentionPolicy policy = createTestPolicy("Test", 5, PeriodUnit.YEARS);
        LocalDate startDate = LocalDate.of(2025, 1, 15);

        LocalDate expiryDate = policy.calculateExpiryDate(startDate);

        assertEquals("Expiry should be 5 years later", LocalDate.of(2030, 1, 15), expiryDate);
    }

    @Test
    public void testCalculateExpiryDate_Months() {
        RetentionPolicy policy = createTestPolicy("Test", 18, PeriodUnit.MONTHS);
        LocalDate startDate = LocalDate.of(2025, 6, 1);

        LocalDate expiryDate = policy.calculateExpiryDate(startDate);

        assertEquals("Expiry should be 18 months later", LocalDate.of(2026, 12, 1), expiryDate);
    }

    @Test
    public void testCalculateExpiryDate_Days() {
        RetentionPolicy policy = createTestPolicy("Test", 30, PeriodUnit.DAYS);
        LocalDate startDate = LocalDate.of(2025, 1, 1);

        LocalDate expiryDate = policy.calculateExpiryDate(startDate);

        assertEquals("Expiry should be 30 days later", LocalDate.of(2025, 1, 31), expiryDate);
    }

    @Test
    public void testCalculateExpiryDate_NullStartDate_ReturnsNull() {
        RetentionPolicy policy = createTestPolicy("Test", 5, PeriodUnit.YEARS);

        LocalDate expiryDate = policy.calculateExpiryDate(null);

        assertNull("Should return null for null start date", expiryDate);
    }

    @Test
    public void testCalculateExpiryDate_LeapYear() {
        RetentionPolicy policy = createTestPolicy("Test", 1, PeriodUnit.YEARS);
        // Feb 29, 2024 is a leap year date
        LocalDate startDate = LocalDate.of(2024, 2, 29);

        LocalDate expiryDate = policy.calculateExpiryDate(startDate);

        // 2025 is not a leap year, so Feb 29 becomes Feb 28
        assertEquals("Should handle leap year correctly", LocalDate.of(2025, 2, 28), expiryDate);
    }

    // ========== CRUD TESTS ==========

    @Test
    public void testCreateAndRetrievePolicy_SampleTypeOnly() {
        // Arrange
        String uniqueName = "Test Policy " + System.currentTimeMillis();
        RetentionPolicy policy = new RetentionPolicy();
        policy.setPolicyName(uniqueName);
        policy.setSampleTypeId(Integer.parseInt(testSampleType.getId()));
        policy.setSampleTypeName(testSampleType.getDescription());
        policy.setPeriodValue(5);
        policy.setPeriodUnit(PeriodUnit.YEARS);
        policy.setIsActive(true);
        policy.setSysUserId(testSysUserId);

        // Act
        RetentionPolicy saved = retentionPolicyService.save(policy);
        RetentionPolicy retrieved = retentionPolicyService.get(saved.getId());

        // Assert
        assertNotNull("Policy should have an ID", saved.getId());
        assertEquals("Policy name should match", uniqueName, retrieved.getPolicyName());
        assertEquals("Sample type ID should match", Integer.parseInt(testSampleType.getId()),
                (int) retrieved.getSampleTypeId());
        assertEquals("Period value should be 5", Integer.valueOf(5), retrieved.getPeriodValue());
        assertEquals("Period unit should be YEARS", PeriodUnit.YEARS, retrieved.getPeriodUnit());
        assertTrue("Policy should be active", retrieved.getIsActive());
        assertNull("Project ID should be null", retrieved.getProjectId());
    }

    @Test
    public void testCreateAndRetrievePolicy_ProjectOnly() {
        // Arrange
        String uniqueName = "Project Policy " + System.currentTimeMillis();
        RetentionPolicy policy = new RetentionPolicy();
        policy.setPolicyName(uniqueName);
        policy.setProjectId(999); // Fake project ID for testing
        policy.setProjectName("Test Project");
        policy.setPeriodValue(10);
        policy.setPeriodUnit(PeriodUnit.YEARS);
        policy.setIsActive(true);
        policy.setSysUserId(testSysUserId);

        // Act
        RetentionPolicy saved = retentionPolicyService.save(policy);
        RetentionPolicy retrieved = retentionPolicyService.get(saved.getId());

        // Assert
        assertNotNull("Policy should have an ID", saved.getId());
        assertEquals("Policy name should match", uniqueName, retrieved.getPolicyName());
        assertEquals("Project ID should match", Integer.valueOf(999), retrieved.getProjectId());
        assertEquals("Project name should match", "Test Project", retrieved.getProjectName());
        assertEquals("Period value should be 10", Integer.valueOf(10), retrieved.getPeriodValue());
        assertNull("Sample type ID should be null", retrieved.getSampleTypeId());
    }

    @Test
    public void testGetAllActive_ReturnsOnlyActivePolices() {
        // Arrange - Create active and inactive policies
        String activeName = "Active Policy " + System.currentTimeMillis();
        String inactiveName = "Inactive Policy " + System.currentTimeMillis();

        RetentionPolicy activePolicy = new RetentionPolicy();
        activePolicy.setPolicyName(activeName);
        activePolicy.setProjectId(888);
        activePolicy.setProjectName("Active Project");
        activePolicy.setPeriodValue(5);
        activePolicy.setPeriodUnit(PeriodUnit.YEARS);
        activePolicy.setIsActive(true);
        activePolicy.setSysUserId(testSysUserId);
        retentionPolicyService.save(activePolicy);

        RetentionPolicy inactivePolicy = new RetentionPolicy();
        inactivePolicy.setPolicyName(inactiveName);
        inactivePolicy.setProjectId(889);
        inactivePolicy.setProjectName("Inactive Project");
        inactivePolicy.setPeriodValue(3);
        inactivePolicy.setPeriodUnit(PeriodUnit.YEARS);
        inactivePolicy.setIsActive(false);
        inactivePolicy.setSysUserId(testSysUserId);
        retentionPolicyService.save(inactivePolicy);

        // Act
        List<RetentionPolicy> activePolicies = retentionPolicyService.getAllActive();

        // Assert
        assertTrue("Should find active policy",
                activePolicies.stream().anyMatch(p -> activeName.equals(p.getPolicyName())));
        assertFalse("Should NOT find inactive policy",
                activePolicies.stream().anyMatch(p -> inactiveName.equals(p.getPolicyName())));
        assertTrue("All returned policies should be active",
                activePolicies.stream().allMatch(p -> Boolean.TRUE.equals(p.getIsActive())));
    }

    @Test
    public void testDeactivate_SetsPolicyInactive() {
        // Arrange
        String policyName = "Deactivate Test " + System.currentTimeMillis();
        RetentionPolicy policy = new RetentionPolicy();
        policy.setPolicyName(policyName);
        policy.setProjectId(777);
        policy.setPeriodValue(5);
        policy.setPeriodUnit(PeriodUnit.YEARS);
        policy.setIsActive(true);
        policy.setSysUserId(testSysUserId);
        RetentionPolicy saved = retentionPolicyService.save(policy);

        // Verify initially active
        assertTrue("Policy should be initially active", saved.getIsActive());

        // Act
        retentionPolicyService.deactivate(saved.getId());

        // Assert
        RetentionPolicy deactivated = retentionPolicyService.get(saved.getId());
        assertFalse("Policy should be inactive after deactivation", deactivated.getIsActive());
    }

    // ========== POLICY MATCHING TESTS ==========

    @Test
    public void testFindApplicablePolicy_ProjectTakesPrecedence() {
        // Arrange - Create both project and sample type policies
        // Use unique IDs to avoid constraint violations between tests
        long timestamp = System.currentTimeMillis();
        Integer projectId = (int) (timestamp % 100000);
        Integer uniqueSampleTypeId = (int) ((timestamp + 1) % 100000) + 40000; // Unique sample type ID

        // Project-specific policy (10 years)
        RetentionPolicy projectPolicy = new RetentionPolicy();
        projectPolicy.setPolicyName("Project Policy " + timestamp);
        projectPolicy.setProjectId(projectId);
        projectPolicy.setProjectName("Test Project");
        projectPolicy.setPeriodValue(10);
        projectPolicy.setPeriodUnit(PeriodUnit.YEARS);
        projectPolicy.setIsActive(true);
        projectPolicy.setSysUserId(testSysUserId);
        retentionPolicyService.save(projectPolicy);

        // Sample type policy (5 years) - use unique sample type ID
        RetentionPolicy sampleTypePolicy = new RetentionPolicy();
        sampleTypePolicy.setPolicyName("Sample Type Policy " + timestamp);
        sampleTypePolicy.setSampleTypeId(uniqueSampleTypeId);
        sampleTypePolicy.setSampleTypeName("Test Sample Type");
        sampleTypePolicy.setPeriodValue(5);
        sampleTypePolicy.setPeriodUnit(PeriodUnit.YEARS);
        sampleTypePolicy.setIsActive(true);
        sampleTypePolicy.setSysUserId(testSysUserId);
        retentionPolicyService.save(sampleTypePolicy);

        // Act - Search with both project and sample type
        Optional<RetentionPolicy> found = retentionPolicyService.findApplicablePolicy(projectId, uniqueSampleTypeId);

        // Assert - Project policy should win
        assertTrue("Should find applicable policy", found.isPresent());
        assertEquals("Project policy should take precedence", Integer.valueOf(10), found.get().getPeriodValue());
        assertEquals("Found policy should be the project policy", projectId, found.get().getProjectId());
    }

    @Test
    public void testFindApplicablePolicy_FallbackToSampleType() {
        // Arrange - Create only sample type policy
        // Use unique IDs to avoid constraint violations between tests
        long timestamp = System.currentTimeMillis();
        Integer unknownProjectId = (int) (timestamp % 100000) + 50000; // Different project ID
        Integer uniqueSampleTypeId = (int) ((timestamp + 2) % 100000) + 41000; // Unique sample type ID

        // Sample type policy only
        RetentionPolicy sampleTypePolicy = new RetentionPolicy();
        sampleTypePolicy.setPolicyName("Fallback Sample Type Policy " + timestamp);
        sampleTypePolicy.setSampleTypeId(uniqueSampleTypeId);
        sampleTypePolicy.setSampleTypeName("Fallback Test Type");
        sampleTypePolicy.setPeriodValue(7);
        sampleTypePolicy.setPeriodUnit(PeriodUnit.YEARS);
        sampleTypePolicy.setIsActive(true);
        sampleTypePolicy.setSysUserId(testSysUserId);
        retentionPolicyService.save(sampleTypePolicy);

        // Act - Search with project that has no policy
        Optional<RetentionPolicy> found = retentionPolicyService.findApplicablePolicy(unknownProjectId,
                uniqueSampleTypeId);

        // Assert - Should fall back to sample type policy
        assertTrue("Should find sample type policy as fallback", found.isPresent());
        assertEquals("Should find sample type policy", Integer.valueOf(7), found.get().getPeriodValue());
        assertEquals("Found policy should be the sample type policy", uniqueSampleTypeId,
                found.get().getSampleTypeId());
    }

    @Test
    public void testFindApplicablePolicy_NoMatchingPolicy() {
        // Arrange - Use IDs that have no policies
        Integer unknownProjectId = 999888;
        Integer unknownSampleTypeId = 999777;

        // Act
        Optional<RetentionPolicy> found = retentionPolicyService.findApplicablePolicy(unknownProjectId,
                unknownSampleTypeId);

        // Assert
        assertFalse("Should not find any policy", found.isPresent());
    }

    @Test
    public void testFindApplicablePolicy_NullProjectId_UsesSampleType() {
        // Arrange - Use unique sample type ID to avoid constraint violations
        long timestamp = System.currentTimeMillis();
        Integer uniqueSampleTypeId = (int) ((timestamp + 3) % 100000) + 42000;

        RetentionPolicy sampleTypePolicy = new RetentionPolicy();
        sampleTypePolicy.setPolicyName("Null Project Test " + timestamp);
        sampleTypePolicy.setSampleTypeId(uniqueSampleTypeId);
        sampleTypePolicy.setSampleTypeName("Null Project Test Type");
        sampleTypePolicy.setPeriodValue(3);
        sampleTypePolicy.setPeriodUnit(PeriodUnit.MONTHS);
        sampleTypePolicy.setIsActive(true);
        sampleTypePolicy.setSysUserId(testSysUserId);
        retentionPolicyService.save(sampleTypePolicy);

        // Act - Pass null project ID
        Optional<RetentionPolicy> found = retentionPolicyService.findApplicablePolicy(null, uniqueSampleTypeId);

        // Assert
        assertTrue("Should find sample type policy when project is null", found.isPresent());
        assertEquals("Should find correct period", Integer.valueOf(3), found.get().getPeriodValue());
        assertEquals("Should find MONTHS unit", PeriodUnit.MONTHS, found.get().getPeriodUnit());
    }

    // ========== SERVICE-LEVEL EXPIRY CALCULATION TESTS ==========

    @Test
    public void testCalculateExpiryDate_Service_WithMatchingPolicy() {
        // Arrange
        long timestamp = System.currentTimeMillis();
        Integer projectId = (int) (timestamp % 100000) + 10000;

        RetentionPolicy policy = new RetentionPolicy();
        policy.setPolicyName("Expiry Calc Test " + timestamp);
        policy.setProjectId(projectId);
        policy.setPeriodValue(2);
        policy.setPeriodUnit(PeriodUnit.YEARS);
        policy.setIsActive(true);
        policy.setSysUserId(testSysUserId);
        retentionPolicyService.save(policy);

        LocalDate fromDate = LocalDate.of(2025, 6, 15);

        // Act
        LocalDate expiryDate = retentionPolicyService.calculateExpiryDate(projectId, null, fromDate);

        // Assert
        assertEquals("Expiry should be 2 years from start", LocalDate.of(2027, 6, 15), expiryDate);
    }

    @Test
    public void testCalculateExpiryDate_Service_NoMatchingPolicy() {
        // Act
        LocalDate expiryDate = retentionPolicyService.calculateExpiryDate(999666, 999555, LocalDate.now());

        // Assert
        assertNull("Should return null when no policy matches", expiryDate);
    }

    @Test
    public void testCalculateExpiryDate_Service_NullFromDate() {
        // Act
        LocalDate expiryDate = retentionPolicyService.calculateExpiryDate(123, 456, null);

        // Assert
        assertNull("Should return null for null from date", expiryDate);
    }

    // ========== CSV IMPORT TESTS ==========

    @Test
    public void testImportFromCsv_ValidCsv_SingleRow() {
        // Arrange
        String csv = "Policy Name,Project,Sample Type,Period\n" + "Test Import Policy " + System.currentTimeMillis()
                + ",,Test Type,5 years";

        // Act
        List<RetentionPolicy> imported = retentionPolicyService.importFromCsv(csv, testSysUserId);

        // Assert
        assertEquals("Should import 1 policy", 1, imported.size());
        RetentionPolicy policy = imported.get(0);
        assertTrue("Policy name should contain 'Test Import Policy'", policy.getPolicyName().contains("Test Import"));
        assertEquals("Period value should be 5", Integer.valueOf(5), policy.getPeriodValue());
        assertEquals("Period unit should be YEARS", PeriodUnit.YEARS, policy.getPeriodUnit());
        assertTrue("Policy should be active", policy.getIsActive());
    }

    @Test
    public void testImportFromCsv_ValidCsv_MultipleRows() {
        // Arrange
        long timestamp = System.currentTimeMillis();
        String csv = "Policy Name,Project,Sample Type,Period\n" + "Policy A " + timestamp + ",Project Alpha,,10 years\n"
                + "Policy B " + timestamp + ",,Blood,18 months\n" + "Policy C " + timestamp
                + ",Project Beta,DNA,30 days";

        // Act
        List<RetentionPolicy> imported = retentionPolicyService.importFromCsv(csv, testSysUserId);

        // Assert
        assertEquals("Should import 3 policies", 3, imported.size());

        // Verify each policy
        RetentionPolicy policyA = imported.stream().filter(p -> p.getPolicyName().contains("Policy A")).findFirst()
                .orElse(null);
        assertNotNull("Should find Policy A", policyA);
        assertEquals("Policy A should have 10 years", Integer.valueOf(10), policyA.getPeriodValue());
        assertEquals("Policy A should be YEARS", PeriodUnit.YEARS, policyA.getPeriodUnit());

        RetentionPolicy policyB = imported.stream().filter(p -> p.getPolicyName().contains("Policy B")).findFirst()
                .orElse(null);
        assertNotNull("Should find Policy B", policyB);
        assertEquals("Policy B should have 18 months", Integer.valueOf(18), policyB.getPeriodValue());
        assertEquals("Policy B should be MONTHS", PeriodUnit.MONTHS, policyB.getPeriodUnit());

        RetentionPolicy policyC = imported.stream().filter(p -> p.getPolicyName().contains("Policy C")).findFirst()
                .orElse(null);
        assertNotNull("Should find Policy C", policyC);
        assertEquals("Policy C should have 30 days", Integer.valueOf(30), policyC.getPeriodValue());
        assertEquals("Policy C should be DAYS", PeriodUnit.DAYS, policyC.getPeriodUnit());
    }

    @Test
    public void testImportFromCsv_PeriodWithoutUnit_DefaultsToYears() {
        // Arrange
        String csv = "Policy Name,Project,Sample Type,Period\n" + "Default Unit Policy " + System.currentTimeMillis()
                + ",Test Project,,5";

        // Act
        List<RetentionPolicy> imported = retentionPolicyService.importFromCsv(csv, testSysUserId);

        // Assert
        assertEquals("Should import 1 policy", 1, imported.size());
        assertEquals("Period should be 5", Integer.valueOf(5), imported.get(0).getPeriodValue());
        assertEquals("Unit should default to YEARS", PeriodUnit.YEARS, imported.get(0).getPeriodUnit());
    }

    @Test
    public void testImportFromCsv_QuotedValues() {
        // Arrange
        String csv = "Policy Name,Project,Sample Type,Period\n" + "\"Policy, With Comma " + System.currentTimeMillis()
                + "\",\"Project, Name\",,\"5 years\"";

        // Act
        List<RetentionPolicy> imported = retentionPolicyService.importFromCsv(csv, testSysUserId);

        // Assert
        assertEquals("Should import 1 policy", 1, imported.size());
        assertTrue("Policy name should contain comma", imported.get(0).getPolicyName().contains("Policy, With Comma"));
        assertEquals("Project name should contain comma", "Project, Name", imported.get(0).getProjectName());
    }

    @Test
    public void testImportFromCsv_EmptyLinesSkipped() {
        // Arrange
        String csv = "Policy Name,Project,Sample Type,Period\n" + "\n" + "Policy With Empty Lines "
                + System.currentTimeMillis() + ",Project,,5 years\n" + "\n";

        // Act
        List<RetentionPolicy> imported = retentionPolicyService.importFromCsv(csv, testSysUserId);

        // Assert
        assertEquals("Should import 1 policy, skipping empty lines", 1, imported.size());
    }

    @Test
    public void testImportFromCsv_DashTreatedAsEmpty() {
        // Arrange - Use dash (-) to indicate empty project
        String csv = "Policy Name,Project,Sample Type,Period\n" + "Dash Project Policy " + System.currentTimeMillis()
                + ",-,Sample Type,5 years";

        // Act
        List<RetentionPolicy> imported = retentionPolicyService.importFromCsv(csv, testSysUserId);

        // Assert
        assertEquals("Should import 1 policy", 1, imported.size());
        assertNull("Project ID should be null when dash used", imported.get(0).getProjectId());
        assertNull("Project name should be null when dash used", imported.get(0).getProjectName());
    }

    @Test(expected = RuntimeException.class)
    public void testImportFromCsv_MissingPolicyName_ThrowsException() {
        // Arrange
        String csv = "Policy Name,Project,Sample Type,Period\n" + ",Project Name,,5 years";

        // Act - Should throw exception
        retentionPolicyService.importFromCsv(csv, testSysUserId);
    }

    @Test(expected = RuntimeException.class)
    public void testImportFromCsv_MissingPeriod_ThrowsException() {
        // Arrange
        String csv = "Policy Name,Project,Sample Type,Period\n" + "Missing Period Policy,Project,,";

        // Act - Should throw exception
        retentionPolicyService.importFromCsv(csv, testSysUserId);
    }

    @Test(expected = RuntimeException.class)
    public void testImportFromCsv_InvalidPeriodFormat_ThrowsException() {
        // Arrange
        String csv = "Policy Name,Project,Sample Type,Period\n" + "Invalid Period Policy,Project,,invalid";

        // Act - Should throw exception
        retentionPolicyService.importFromCsv(csv, testSysUserId);
    }

    @Test(expected = RuntimeException.class)
    public void testImportFromCsv_NotEnoughHeaders_ThrowsException() {
        // Arrange - Only 3 columns instead of required 4
        // The IllegalArgumentException from validateHeaders is wrapped in
        // RuntimeException
        String csv = "Policy Name,Project,Sample Type\n" + "Bad Format,Project,Type";

        // Act - Should throw exception
        retentionPolicyService.importFromCsv(csv, testSysUserId);
    }

    // ========== EXISTS TESTS ==========

    @Test
    public void testExistsForProject_True() {
        // Arrange
        long timestamp = System.currentTimeMillis();
        Integer projectId = (int) (timestamp % 100000) + 20000;

        RetentionPolicy policy = new RetentionPolicy();
        policy.setPolicyName("Exists Test " + timestamp);
        policy.setProjectId(projectId);
        policy.setPeriodValue(5);
        policy.setPeriodUnit(PeriodUnit.YEARS);
        policy.setIsActive(true);
        policy.setSysUserId(testSysUserId);
        retentionPolicyService.save(policy);

        // Act & Assert
        assertTrue("Should return true for existing project", retentionPolicyService.existsForProject(projectId));
    }

    @Test
    public void testExistsForProject_False() {
        // Act & Assert
        assertFalse("Should return false for non-existing project", retentionPolicyService.existsForProject(999444));
    }

    @Test
    public void testExistsForSampleType_True() {
        // Arrange
        long timestamp = System.currentTimeMillis();
        Integer sampleTypeId = (int) (timestamp % 100000) + 30000;

        RetentionPolicy policy = new RetentionPolicy();
        policy.setPolicyName("Sample Type Exists Test " + timestamp);
        policy.setSampleTypeId(sampleTypeId);
        policy.setSampleTypeName("Test Type");
        // No project ID - this is a sample-type-only policy
        policy.setPeriodValue(5);
        policy.setPeriodUnit(PeriodUnit.YEARS);
        policy.setIsActive(true);
        policy.setSysUserId(testSysUserId);
        retentionPolicyService.save(policy);

        // Act & Assert
        assertTrue("Should return true for existing sample type",
                retentionPolicyService.existsForSampleType(sampleTypeId));
    }

    @Test
    public void testExistsForSampleType_False() {
        // Act & Assert
        assertFalse("Should return false for non-existing sample type",
                retentionPolicyService.existsForSampleType(999333));
    }

    // ========== POLICY TYPE TESTS ==========

    @Test
    public void testIsProjectPolicy() {
        RetentionPolicy projectPolicy = new RetentionPolicy();
        projectPolicy.setProjectId(123);

        RetentionPolicy sampleTypePolicy = new RetentionPolicy();
        sampleTypePolicy.setSampleTypeId(456);

        assertTrue("Should be project policy when projectId is set", projectPolicy.isProjectPolicy());
        assertFalse("Should not be project policy when projectId is null", sampleTypePolicy.isProjectPolicy());
    }

    @Test
    public void testIsSampleTypePolicy() {
        RetentionPolicy projectPolicy = new RetentionPolicy();
        projectPolicy.setProjectId(123);

        RetentionPolicy sampleTypePolicy = new RetentionPolicy();
        sampleTypePolicy.setSampleTypeId(456);

        assertFalse("Should not be sample type policy when sampleTypeId is null", projectPolicy.isSampleTypePolicy());
        assertTrue("Should be sample type policy when sampleTypeId is set", sampleTypePolicy.isSampleTypePolicy());
    }

    // ========== FIND BY PROJECT NAME TESTS ==========

    @Test
    public void testFindApplicablePolicyByProjectName_MatchesProjectName() {
        // Arrange - Create policy with project name
        long timestamp = System.currentTimeMillis();
        String projectName = "PROJ-MALARIA-" + timestamp;

        RetentionPolicy policy = new RetentionPolicy();
        policy.setPolicyName("Malaria Retention " + timestamp);
        policy.setProjectId((int) (timestamp % 100000) + 60000);
        policy.setProjectName(projectName);
        policy.setPeriodValue(10);
        policy.setPeriodUnit(PeriodUnit.YEARS);
        policy.setIsActive(true);
        policy.setSysUserId(testSysUserId);
        retentionPolicyService.save(policy);

        // Act - Find by project name (string)
        Optional<RetentionPolicy> found = retentionPolicyService.findApplicablePolicyByProjectName(projectName, null);

        // Assert
        assertTrue("Should find policy by project name", found.isPresent());
        assertEquals("Period value should match", Integer.valueOf(10), found.get().getPeriodValue());
        assertEquals("Project name should match", projectName, found.get().getProjectName());
    }

    @Test
    public void testFindApplicablePolicyByProjectName_CaseInsensitive() {
        // Arrange
        long timestamp = System.currentTimeMillis();
        String projectName = "PROJ-AMR-" + timestamp;

        RetentionPolicy policy = new RetentionPolicy();
        policy.setPolicyName("AMR Retention " + timestamp);
        policy.setProjectId((int) (timestamp % 100000) + 61000);
        policy.setProjectName(projectName);
        policy.setPeriodValue(15);
        policy.setPeriodUnit(PeriodUnit.YEARS);
        policy.setIsActive(true);
        policy.setSysUserId(testSysUserId);
        retentionPolicyService.save(policy);

        // Act - Find with lowercase project name
        Optional<RetentionPolicy> found = retentionPolicyService
                .findApplicablePolicyByProjectName(projectName.toLowerCase(), null);

        // Assert
        assertTrue("Should find policy case-insensitively", found.isPresent());
        assertEquals("Period value should match", Integer.valueOf(15), found.get().getPeriodValue());
    }

    @Test
    public void testFindApplicablePolicyByProjectName_FallbackToSampleType() {
        // Arrange - Create only sample type policy
        long timestamp = System.currentTimeMillis();
        Integer uniqueSampleTypeId = (int) ((timestamp + 4) % 100000) + 43000;

        RetentionPolicy sampleTypePolicy = new RetentionPolicy();
        sampleTypePolicy.setPolicyName("Sample Type Fallback " + timestamp);
        sampleTypePolicy.setSampleTypeId(uniqueSampleTypeId);
        sampleTypePolicy.setSampleTypeName("Fallback Test Type");
        sampleTypePolicy.setPeriodValue(5);
        sampleTypePolicy.setPeriodUnit(PeriodUnit.YEARS);
        sampleTypePolicy.setIsActive(true);
        sampleTypePolicy.setSysUserId(testSysUserId);
        retentionPolicyService.save(sampleTypePolicy);

        // Act - Search with non-existent project name
        Optional<RetentionPolicy> found = retentionPolicyService
                .findApplicablePolicyByProjectName("NON-EXISTENT-PROJECT", uniqueSampleTypeId);

        // Assert - Should fall back to sample type policy
        assertTrue("Should find sample type policy as fallback", found.isPresent());
        assertEquals("Period value should match sample type policy", Integer.valueOf(5), found.get().getPeriodValue());
    }

    @Test
    public void testFindApplicablePolicyByProjectName_NullProjectName_UsesSampleType() {
        // Arrange
        long timestamp = System.currentTimeMillis();
        Integer uniqueSampleTypeId = (int) ((timestamp + 5) % 100000) + 44000;

        RetentionPolicy sampleTypePolicy = new RetentionPolicy();
        sampleTypePolicy.setPolicyName("Null Project Name Test " + timestamp);
        sampleTypePolicy.setSampleTypeId(uniqueSampleTypeId);
        sampleTypePolicy.setSampleTypeName("Null Project Test Type");
        sampleTypePolicy.setPeriodValue(3);
        sampleTypePolicy.setPeriodUnit(PeriodUnit.MONTHS);
        sampleTypePolicy.setIsActive(true);
        sampleTypePolicy.setSysUserId(testSysUserId);
        retentionPolicyService.save(sampleTypePolicy);

        // Act - Pass null project name
        Optional<RetentionPolicy> found = retentionPolicyService.findApplicablePolicyByProjectName(null,
                uniqueSampleTypeId);

        // Assert
        assertTrue("Should find sample type policy when project name is null", found.isPresent());
        assertEquals("Period value should match", Integer.valueOf(3), found.get().getPeriodValue());
    }

    @Test
    public void testFindApplicablePolicyByProjectName_EmptyProjectName_UsesSampleType() {
        // Arrange
        long timestamp = System.currentTimeMillis();
        Integer uniqueSampleTypeId = (int) ((timestamp + 6) % 100000) + 45000;

        RetentionPolicy sampleTypePolicy = new RetentionPolicy();
        sampleTypePolicy.setPolicyName("Empty Project Name Test " + timestamp);
        sampleTypePolicy.setSampleTypeId(uniqueSampleTypeId);
        sampleTypePolicy.setSampleTypeName("Empty Project Test Type");
        sampleTypePolicy.setPeriodValue(6);
        sampleTypePolicy.setPeriodUnit(PeriodUnit.MONTHS);
        sampleTypePolicy.setIsActive(true);
        sampleTypePolicy.setSysUserId(testSysUserId);
        retentionPolicyService.save(sampleTypePolicy);

        // Act - Pass empty project name
        Optional<RetentionPolicy> found = retentionPolicyService.findApplicablePolicyByProjectName("",
                uniqueSampleTypeId);

        // Assert
        assertTrue("Should find sample type policy when project name is empty", found.isPresent());
        assertEquals("Period value should match", Integer.valueOf(6), found.get().getPeriodValue());
    }

    @Test
    public void testFindApplicablePolicyByProjectName_NoMatchingPolicy() {
        // Act
        Optional<RetentionPolicy> found = retentionPolicyService
                .findApplicablePolicyByProjectName("COMPLETELY-UNKNOWN-PROJECT", 999111);

        // Assert
        assertFalse("Should not find any policy", found.isPresent());
    }

    // ========== CALCULATE EXPIRY BY PROJECT NAME TESTS ==========

    @Test
    public void testCalculateExpiryDateByProjectName_WithMatchingPolicy() {
        // Arrange
        long timestamp = System.currentTimeMillis();
        String projectName = "PROJ-CALC-" + timestamp;

        RetentionPolicy policy = new RetentionPolicy();
        policy.setPolicyName("Calc Test " + timestamp);
        policy.setProjectId((int) (timestamp % 100000) + 62000);
        policy.setProjectName(projectName);
        policy.setPeriodValue(2);
        policy.setPeriodUnit(PeriodUnit.YEARS);
        policy.setIsActive(true);
        policy.setSysUserId(testSysUserId);
        retentionPolicyService.save(policy);

        LocalDate fromDate = LocalDate.of(2025, 6, 15);

        // Act
        LocalDate expiryDate = retentionPolicyService.calculateExpiryDateByProjectName(projectName, null, fromDate);

        // Assert
        assertEquals("Expiry should be 2 years from start", LocalDate.of(2027, 6, 15), expiryDate);
    }

    @Test
    public void testCalculateExpiryDateByProjectName_NoMatchingPolicy() {
        // Act
        LocalDate expiryDate = retentionPolicyService.calculateExpiryDateByProjectName("UNKNOWN-PROJECT", 999222,
                LocalDate.now());

        // Assert
        assertNull("Should return null when no policy matches", expiryDate);
    }

    @Test
    public void testCalculateExpiryDateByProjectName_NullFromDate() {
        // Act
        LocalDate expiryDate = retentionPolicyService.calculateExpiryDateByProjectName("ANY-PROJECT", 456, null);

        // Assert
        assertNull("Should return null for null from date", expiryDate);
    }

    // ========== HELPER METHODS ==========

    private RetentionPolicy createTestPolicy(String name, int periodValue, PeriodUnit periodUnit) {
        RetentionPolicy policy = new RetentionPolicy();
        policy.setPolicyName(name);
        policy.setPeriodValue(periodValue);
        policy.setPeriodUnit(periodUnit);
        policy.setIsActive(true);
        policy.setSysUserId(testSysUserId);
        return policy;
    }
}
