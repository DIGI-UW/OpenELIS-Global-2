package org.openelisglobal.containertype.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.containertype.form.ContainerTypeForm;
import org.openelisglobal.containertype.form.ContainerTypeView;
import org.openelisglobal.containertype.form.SampleTypeContainersView;
import org.openelisglobal.containertype.form.TestContainersForm;
import org.openelisglobal.containertype.form.TestContainersView;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * OGC-1611 P2 (FR-G5 to G7): a test names its expected containers, one
 * Preferred and any number of Alternate, a container yielding another sample
 * type can be used as the test's own, and a sample type has a default
 * container.
 */
@Transactional
public class TestContainerServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TestContainerService testContainerService;
    @Autowired
    private ContainerTypeService containerTypeService;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private MicrobiologyTestFixtures fixtures;
    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private TestService testService;

    private JdbcTemplate jdbc;
    private String serumId;
    private String plasmaId;
    private String serumName;
    private String plasmaName;
    private String creatinine;
    private String glucose;
    private String waterTest;
    private ContainerTypeView sst;
    private ContainerTypeView redTop;
    private ContainerTypeView pst;
    private ContainerTypeView envBottle;
    private ContainerTypeView retired;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        TypeOfSample serum = fixtures.createTypeOfSample();
        TypeOfSample plasma = fixtures.createTypeOfSample();
        serumId = serum.getId();
        plasmaId = plasma.getId();
        serumName = serum.getLocalizedName();
        plasmaName = plasma.getLocalizedName();
        creatinine = test("CLINICAL", serumId);
        glucose = test("CLINICAL", serumId);
        waterTest = test("ENVIRONMENTAL", serumId);
        sst = container("QA1611-SST", "QA1611 SST", List.of("CLINICAL"), serumId);
        redTop = container("QA1611-RED", "QA1611 Red top", List.of("CLINICAL"), serumId);
        pst = container("QA1611-PST", "QA1611 Li-hep PST", List.of("CLINICAL"), plasmaId);
        envBottle = container("QA1611-BOTTLE", "QA1611 Bottle", List.of("ENVIRONMENTAL"), serumId);
        retired = container("QA1611-OLD", "QA1611 Old tube", List.of("CLINICAL"), serumId);
        containerTypeService.setActive(retired.id(), false, "1");
    }

    @After
    public void forgetTheSampleTypeLinks() {
        typeOfSampleService.clearCache();
    }

    @Test
    public void aTestNamesItsPreferredAndAlternateContainersWithCountsAndVolumes() {
        TestContainersView saved = testContainerService.save(creatinine, new TestContainersForm(
                List.of(row(sst, "PREFERRED", 2, "1.5", "mL", null), row(redTop, "alternate", null, null, null, null)),
                true), "1");

        assertTrue(saved.needsOwnContainer());
        assertEquals(List.of("QA1611-SST", "QA1611-RED"), saved.containers().stream().map(r -> r.code()).toList());
        TestContainersView.Row preferred = saved.containers().get(0);
        assertEquals("PREFERRED", preferred.role());
        assertEquals(2, preferred.count());
        assertEquals(0, new BigDecimal("1.5").compareTo(preferred.minVolume()));
        assertEquals("mL", preferred.volumeUnit());
        TestContainersView.Row alternate = saved.containers().get(1);
        assertEquals("ALTERNATE", alternate.role());
        assertEquals("a count defaults to one", 1, alternate.count());
        assertNull(alternate.minVolume());
        assertEquals(List.of(serumName), saved.primarySampleTypes().stream().map(IdValuePair::getValue).toList());
        assertTrue(saved.secondarySampleTypes().isEmpty());

        TestContainersView read = testContainerService.forTest(creatinine).orElseThrow();
        assertEquals("what was saved reads back", saved.toString(), read.toString());
    }

    @Test
    public void aContainerYieldingAnotherSampleTypeIsAnAlternateUsedAsTheTestsOwn() {
        TestContainersView saved = testContainerService.save(creatinine, new TestContainersForm(
                List.of(row(sst, "PREFERRED", 1, null, null, null), row(pst, "ALTERNATE", 1, null, null, serumId)),
                false), "1");

        TestContainersView.Row usedAs = saved.containers().get(1);
        assertEquals(serumId, usedAs.usedAsSampleTypeId());
        assertEquals(serumName, usedAs.usedAsSampleTypeName());
        assertEquals(List.of(plasmaName), usedAs.yields().stream().map(IdValuePair::getValue).toList());
        TestContainersView.SecondarySampleType secondary = saved.secondarySampleTypes().get(0);
        assertEquals(plasmaName, secondary.sampleTypeName());
        assertEquals("QA1611 Li-hep PST", secondary.containerName());
        assertEquals(serumName, secondary.usedAsSampleTypeName());

        SampleTypeContainersView plasma = testContainerService.forSampleType(plasmaId).orElseThrow();
        assertTrue("Plasma is primary for no test", plasma.primaryForTests().isEmpty());
        assertEquals(List.of(nameOf(creatinine)),
                plasma.secondaryForTests().stream().map(SampleTypeContainersView.SecondaryTest::testName).toList());
        assertEquals("QA1611 Li-hep PST", plasma.secondaryForTests().get(0).containerName());
        SampleTypeContainersView serum = testContainerService.forSampleType(serumId).orElseThrow();
        assertEquals(3, serum.primaryForTests().size());
        assertTrue(serum.secondaryForTests().isEmpty());
    }

    @Test
    public void containersTheRulesForbidAreRefusedWithTheRowAndNothingIsWritten() {
        testContainerService.save(creatinine,
                new TestContainersForm(List.of(row(sst, "PREFERRED", 1, null, null, null)), false), "1");

        assertRefused("role", "preferredRequired", null, row(sst, "ALTERNATE", 1, null, null, null));
        assertRefused("role", "onePreferred", null, row(sst, "PREFERRED", 1, null, null, null),
                row(redTop, "PREFERRED", 1, null, null, null));
        assertRefused("containerTypeId", "containerDuplicate", 1, row(sst, "PREFERRED", 1, null, null, null),
                row(sst, "ALTERNATE", 1, null, null, null));
        assertRefused("containerTypeId", "containerSampleType", 1, row(sst, "PREFERRED", 1, null, null, null),
                row(pst, "ALTERNATE", 1, null, null, null));
        assertRefused("role", "usedAsPreferred", 0, row(pst, "PREFERRED", 1, null, null, serumId));
        assertRefused("usedAsSampleTypeId", "usedAsNotTestType", 1, row(sst, "PREFERRED", 1, null, null, null),
                row(pst, "ALTERNATE", 1, null, null, plasmaId));
        assertRefused("usedAsSampleTypeId", "usedAsNotNeeded", 1, row(sst, "PREFERRED", 1, null, null, null),
                row(redTop, "ALTERNATE", 1, null, null, serumId));
        assertRefused("containerTypeId", "containerDomain", 0, row(envBottle, "PREFERRED", 1, null, null, null));
        assertRefused("containerTypeId", "containerInactive", 1, row(sst, "PREFERRED", 1, null, null, null),
                row(retired, "ALTERNATE", 1, null, null, null));
        assertRefused("count", "countInvalid", 0, row(sst, "PREFERRED", 0, null, null, null));
        assertRefused("minVolume", "volumeInvalid", 0, row(sst, "PREFERRED", 1, "0", "mL", null));
        assertRefused("volumeUnit", "unitRequired", 0, row(sst, "PREFERRED", 1, "2", " ", null));
        assertRefused("role", "roleInvalid", 0, row(sst, "BEST", 1, null, null, null));

        assertEquals("every refusal left the saved list alone", List.of("QA1611-SST"), testContainerService
                .forTest(creatinine).orElseThrow().containers().stream().map(r -> r.code()).toList());
    }

    @Test
    public void aContainerDeactivatedAfterItWasListedStaysOnTheTestAndNamesItsTests() {
        testContainerService.save(glucose, new TestContainersForm(
                List.of(row(sst, "PREFERRED", 1, null, null, null), row(redTop, "ALTERNATE", 1, null, null, null)),
                false), "1");
        containerTypeService.setActive(redTop.id(), false, "1");

        TestContainersView kept = testContainerService.save(glucose, new TestContainersForm(
                List.of(row(sst, "PREFERRED", 1, null, null, null), row(redTop, "ALTERNATE", 3, null, null, null)),
                false), "1");

        assertFalse(kept.containers().get(1).active());
        assertEquals(3, kept.containers().get(1).count());
        assertTrue("a deactivated container is not offered again",
                kept.yieldingChoices().stream().noneMatch(choice -> choice.id().equals(redTop.id())));
        assertEquals(List.of(nameOf(glucose)), containerTypeService.usage(redTop.id()).orElseThrow().tests());
        assertEquals("the list's Used by counts the tests too", 1,
                containerTypeService.get(redTop.id()).orElseThrow().testsUsing());
        assertEquals(0, containerTypeService.get(pst.id()).orElseThrow().testsUsing());
    }

    @Test
    public void theChoicesAreTheTestsDomainsContainersYieldingItsSampleTypeFirst() {
        TestContainersView view = testContainerService.forTest(creatinine).orElseThrow();

        List<Long> yielding = view.yieldingChoices().stream().map(ContainerTypeView::id).toList();
        List<Long> other = view.otherChoices().stream().map(ContainerTypeView::id).toList();
        assertTrue(yielding.containsAll(List.of(sst.id(), redTop.id())));
        assertFalse(yielding.contains(pst.id()));
        assertTrue(other.contains(pst.id()));
        assertFalse("another domain's container is not offered", yielding.contains(envBottle.id()));
        assertFalse(yielding.contains(retired.id()));
        assertEquals(List.of(envBottle.id()), testContainerService.forTest(waterTest).orElseThrow().yieldingChoices()
                .stream().map(ContainerTypeView::id).filter(id -> id.equals(envBottle.id())).toList());
    }

    @Test
    public void aSampleTypesDefaultContainerMustYieldIt() {
        SampleTypeContainersView set = testContainerService.setDefaultContainer(serumId, sst.id());
        assertEquals(sst.id(), set.defaultContainerTypeId());
        assertTrue(set.containerChoices().stream().anyMatch(choice -> choice.id().equals(redTop.id())));
        assertFalse(set.containerChoices().stream().anyMatch(choice -> choice.id().equals(pst.id())));

        TestContainersRefusedException wrongType = assertThrows(TestContainersRefusedException.class,
                () -> testContainerService.setDefaultContainer(serumId, pst.id()));
        assertEquals("defaultContainerSampleType", wrongType.getCode());
        TestContainersRefusedException inactive = assertThrows(TestContainersRefusedException.class,
                () -> testContainerService.setDefaultContainer(serumId, retired.id()));
        assertEquals("containerInactive", inactive.getCode());
        assertEquals(sst.id(), testContainerService.forSampleType(serumId).orElseThrow().defaultContainerTypeId());

        assertNull(testContainerService.setDefaultContainer(serumId, null).defaultContainerTypeId());
    }

    private void assertRefused(String field, String code, Integer rowIndex, TestContainersForm.Row... rows) {
        TestContainersRefusedException refusal = assertThrows(TestContainersRefusedException.class,
                () -> testContainerService.save(creatinine, new TestContainersForm(List.of(rows), false), "1"));
        assertEquals(code, refusal.getCode());
        assertEquals(field, refusal.getField());
        assertEquals(rowIndex, refusal.getRow());
    }

    private static TestContainersForm.Row row(ContainerTypeView container, String role, Integer count, String minVolume,
            String unit, String usedAs) {
        return new TestContainersForm.Row(container.id(), role, count,
                minVolume == null ? null : new BigDecimal(minVolume), unit, usedAs);
    }

    private ContainerTypeView container(String code, String name, List<String> domains, String sampleTypeId) {
        return containerTypeService.save(null, new ContainerTypeForm(code, name, null, null, null, null, null, null,
                null, null, new ArrayList<>(domains), List.of(sampleTypeId)), "1");
    }

    private String test(String domain, String sampleTypeId) {
        org.openelisglobal.test.valueholder.Test created = fixtures.createCatalogTest();
        created.setDomain(domain);
        testService.update(created);
        String testId = created.getId();
        jdbc.update("INSERT INTO clinlims.sampletype_test (id, sample_type_id, test_id) VALUES"
                + " (nextval('clinlims.sample_type_test_seq'), ?::numeric, ?::numeric)", sampleTypeId, testId);
        typeOfSampleService.clearCache();
        return testId;
    }

    private String nameOf(String testId) {
        return TestServiceImpl.getLocalizedTestNameWithType(testId);
    }
}
