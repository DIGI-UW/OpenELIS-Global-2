package org.openelisglobal.resultlimit.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.siteinformation.service.SiteInformationDomainService;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.openelisglobal.siteinformation.valueholder.SiteInformationDomain;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.util.AopTestUtils;

public class ResultLimitServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ResultLimitService resultLimitService;

    @Autowired
    private TypeOfTestResultService typeOfTestResultService;

    @Autowired
    private SiteInformationService siteInformationService;

    @Autowired
    private SiteInformationDomainService siteInformationDomainService;

    private static class AgeRangeSnapshot {
        boolean existed;
        String id;
        String value;
        String nameKey;
        String instructionKey;
        String description;
        SiteInformationDomain domain;
    }

    private final Map<String, AgeRangeSnapshot> snapshottedAgeRanges = new HashMap<>();
    private boolean domainExisted;
    private String domainId;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/result-limit.xml");
        // OclInnitializerTest may have loaded type-of-testresult.xml which remaps
        // type IDs (e.g. Numeric -> id=1 rather than the canonical id=4). The fixture
        // row result_limits id=1 was written with test_result_type_id=4. Re-point
        // that FK to the actual Numeric type ID in the DB, then sync the service
        // cache. This mirrors the pattern used by AccessionValidationBulkReleaseTest.
        String numericTypeId = typeOfTestResultService.getTypeOfTestResultByType("N").getId();
        Assert.assertNotNull("Numeric result type must be seeded by database migrations", numericTypeId);
        jdbcTemplate.update("UPDATE clinlims.result_limits SET test_result_type_id = ? WHERE id = 1",
                Integer.valueOf(numericTypeId));
        ((ResultLimitServiceImpl) AopTestUtils.getTargetObject(resultLimitService)).initializeGlobalVariables();
        ensureReferenceTables("result_limits", "test");
        snapshotAndEnsureAgeRanges();
    }

    @After
    public void tearDown() throws Exception {
        restoreAgeRanges();
    }

    private void snapshotAndEnsureAgeRanges() {
        snapshottedAgeRanges.clear();
        SiteInformationDomain domain = siteInformationDomainService.getByName("resultAgeRange");
        if (domain == null) {
            domainExisted = false;
            domain = new SiteInformationDomain();
            domain.setName("resultAgeRange");
            domain.setDescription("The maximum age range in months");
            siteInformationDomainService.insert(domain);
        } else {
            domainExisted = true;
        }
        domainId = domain.getId();

        String[][] ageRangeDefs = { { "new born", "1", "max age of a new born", "site.info.result.age.newborn" },
                { "infant", "12", "max age of an infant", "site.info.result.age.infant" },
                { "young child", "60", "max age of a young child", "site.info.result.age.youngchild" },
                { "child", "168", "max age of a child", "site.info.result.age.child" },
                { "adult", "Infinity", "max age of an adult", "site.info.result.age.adult" } };

        for (String[] def : ageRangeDefs) {
            String name = def[0];
            String value = def[1];
            String desc = def[2];
            String nameKey = def[3];

            SiteInformation existing = siteInformationService.getSiteInformationByName(name);
            AgeRangeSnapshot snap = new AgeRangeSnapshot();
            if (existing != null) {
                snap.existed = true;
                snap.id = existing.getId();
                snap.value = existing.getValue();
                snap.nameKey = existing.getNameKey();
                snap.instructionKey = existing.getInstructionKey();
                snap.description = existing.getDescription();
                snap.domain = existing.getDomain();
                snapshottedAgeRanges.put(name, snap);

                existing.setValue(value);
                existing.setNameKey(nameKey);
                existing.setDomain(domain);
                existing.setValueType("text");
                existing.setInstructionKey("instrution.result.age");
                existing.setDescription(desc);
                existing.setSysUserId("1");
                siteInformationService.persistData(existing, false);
            } else {
                snap.existed = false;
                snapshottedAgeRanges.put(name, snap);

                SiteInformation newRow = new SiteInformation();
                newRow.setName(name);
                newRow.setValue(value);
                newRow.setNameKey(nameKey);
                newRow.setDomain(domain);
                newRow.setValueType("text");
                newRow.setInstructionKey("instrution.result.age");
                newRow.setDescription(desc);
                newRow.setSysUserId("1");
                siteInformationService.persistData(newRow, true);
            }
        }
    }

    private void restoreAgeRanges() {
        for (Map.Entry<String, AgeRangeSnapshot> entry : snapshottedAgeRanges.entrySet()) {
            String name = entry.getKey();
            AgeRangeSnapshot snap = entry.getValue();
            SiteInformation current = siteInformationService.getSiteInformationByName(name);
            if (snap.existed) {
                if (current != null) {
                    current.setValue(snap.value);
                    current.setNameKey(snap.nameKey);
                    current.setInstructionKey(snap.instructionKey);
                    current.setDescription(snap.description);
                    current.setDomain(snap.domain);
                    current.setSysUserId("1");
                    siteInformationService.persistData(current, false);
                }
            } else if (current != null) {
                siteInformationService.delete(current);
            }
        }
        if (!domainExisted && domainId != null) {
            SiteInformationDomain domain = siteInformationDomainService.get(domainId);
            if (domain != null) {
                siteInformationDomainService.delete(domain);
            }
        }
    }

    @Test
    public void getAllResultLimits_shouldReturnAllFixtureRows() {
        List<ResultLimit> limits = resultLimitService.getAllResultLimits();
        Assert.assertEquals(3, limits.size());
    }

    @Test
    public void getResultLimitById_shouldReturnMatchingRowDetails() {
        ResultLimit limit = resultLimitService.getResultLimitById("1");
        Assert.assertEquals("1", limit.getId());
        Assert.assertEquals("7001", limit.getTestId());
        Assert.assertEquals("M", limit.getGender());
        Assert.assertEquals(0.0, limit.getMinAge(), 0.001);
        Assert.assertEquals(18.0, limit.getMaxAge(), 0.001);
        Assert.assertEquals(4.0, limit.getLowNormal(), 0.001);
        Assert.assertEquals(10.0, limit.getHighNormal(), 0.001);
        Assert.assertEquals(2.0, limit.getLowValid(), 0.001);
        Assert.assertEquals(15.0, limit.getHighValid(), 0.001);
        Assert.assertEquals(true, limit.isAlwaysValidate());
    }

    @Test
    public void getResultLimits_byTestId_shouldReturnMatchingLimits() {
        List<ResultLimit> limits7002 = resultLimitService.getResultLimits("7002");
        Assert.assertEquals(2, limits7002.size());
        Assert.assertEquals("2", limits7002.get(0).getId());
        Assert.assertEquals("3", limits7002.get(1).getId());
    }

    @Test
    public void getAllResultLimitsForTest_shouldReturnLimitsForSingleTest() {
        List<ResultLimit> limits7001 = resultLimitService.getAllResultLimitsForTest("7001");
        Assert.assertEquals(1, limits7001.size());
        Assert.assertEquals("1", limits7001.get(0).getId());
        Assert.assertEquals("7001", limits7001.get(0).getTestId());
    }

    @Test
    public void getPredefinedAgeRanges_shouldReturnAgeRangeList() {
        Locale previousLocale = LocaleContextHolder.getLocale();
        try {
            LocaleContextHolder.setLocale(Locale.ENGLISH);
            List<IdValuePair> ageRanges = resultLimitService.getPredefinedAgeRanges();
            Assert.assertEquals("Should contain 5 sorted age ranges", 5, ageRanges.size());

            Assert.assertEquals("1", ageRanges.get(0).getId());
            Assert.assertEquals("Newborn", ageRanges.get(0).getValue());

            Assert.assertEquals("12", ageRanges.get(1).getId());
            Assert.assertEquals("Infant", ageRanges.get(1).getValue());

            Assert.assertEquals("60", ageRanges.get(2).getId());
            Assert.assertEquals("Young child", ageRanges.get(2).getValue());

            Assert.assertEquals("168", ageRanges.get(3).getId());
            Assert.assertEquals("Child", ageRanges.get(3).getValue());

            Assert.assertEquals("Infinity", ageRanges.get(4).getId());
            Assert.assertEquals("Adult", ageRanges.get(4).getValue());
        } finally {
            LocaleContextHolder.setLocale(previousLocale);
        }
    }

    @Test
    public void getDisplayNormalRange_shouldFormatRangeWithSignificantDigits() {
        Locale previousFormatLocale = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.US);
            String formattedRange = resultLimitService.getDisplayNormalRange(4.0, 10.0, "2", " - ");
            Assert.assertEquals("4.00 - 10.00", formattedRange);
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, previousFormatLocale);
        }
    }

    @Test
    public void getDisplayAgeRange_shouldFormatAgeRangeCorrectly() {
        ResultLimit limit = resultLimitService.getResultLimitById("1");
        String ageRangeDisplay = resultLimitService.getDisplayAgeRange(limit, " to ");
        Assert.assertTrue("Display should contain min age 0", ageRangeDisplay.contains("0"));
        Assert.assertTrue("Display should contain max age 18", ageRangeDisplay.contains("18"));
    }

    @Test
    public void saveRangesForTest_shouldInsertNewLimitForTest() {
        String numericTypeId = typeOfTestResultService.getTypeOfTestResultByType("N").getId();

        ResultLimit newLimit = new ResultLimit();
        newLimit.setTestId("7001");
        newLimit.setResultTypeId(numericTypeId);
        newLimit.setMinAge(19.0);
        newLimit.setMaxAge(65.0);
        newLimit.setGender("M");
        newLimit.setLowNormal(5.0);
        newLimit.setHighNormal(11.0);
        newLimit.setLowValid(2.5);
        newLimit.setHighValid(16.0);
        newLimit.setSysUserId("1");

        List<ResultLimit> existing = resultLimitService.getAllResultLimitsForTest("7001");
        List<ResultLimit> desired = new ArrayList<>(existing);
        desired.add(newLimit);

        resultLimitService.saveRangesForTest("7001", desired, "1");

        List<ResultLimit> savedLimits = resultLimitService.getAllResultLimitsForTest("7001");
        Assert.assertEquals(2, savedLimits.size());

        ResultLimit inserted = savedLimits.stream()
                .filter(l -> Double.valueOf(19.0).equals(l.getMinAge()) && Double.valueOf(65.0).equals(l.getMaxAge()))
                .findFirst().orElse(null);
        Assert.assertNotNull("Newly inserted 19–65 range must be present", inserted);
        Assert.assertNotNull("Persisted limit must have an ID", inserted.getId());
        Assert.assertEquals("7001", inserted.getTestId());
        Assert.assertEquals(numericTypeId, inserted.getResultTypeId());
        Assert.assertEquals("M", inserted.getGender());
        Assert.assertEquals(19.0, inserted.getMinAge(), 0.001);
        Assert.assertEquals(65.0, inserted.getMaxAge(), 0.001);
        Assert.assertEquals(5.0, inserted.getLowNormal(), 0.001);
        Assert.assertEquals(11.0, inserted.getHighNormal(), 0.001);
        Assert.assertEquals(2.5, inserted.getLowValid(), 0.001);
        Assert.assertEquals(16.0, inserted.getHighValid(), 0.001);
    }
}
