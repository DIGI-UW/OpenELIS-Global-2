package org.openelisglobal.containertype.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.containertype.form.ContainerProposalForm;
import org.openelisglobal.containertype.form.ContainerProposalView;
import org.openelisglobal.containertype.form.ContainerTypeForm;
import org.openelisglobal.containertype.form.ContainerTypeView;
import org.openelisglobal.containertype.form.TestContainersForm;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * OGC-1611 P4 (FR-B24, B25, G7, G8): the containers proposed for a sample from
 * its tests' expected containers, and which container a sample may be saved in.
 */
@Transactional
public class ContainerProposalServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ContainerProposalService proposalService;
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
        serumId = fixtures.createTypeOfSample().getId();
        plasmaId = fixtures.createTypeOfSample().getId();
        sst = container("QA1611P4-SST", "QA1611P4 SST", List.of("CLINICAL"), serumId);
        redTop = container("QA1611P4-RED", "QA1611P4 Red top", List.of("CLINICAL"), serumId);
        pst = container("QA1611P4-PST", "QA1611P4 Li-hep PST", List.of("CLINICAL"), plasmaId);
        envBottle = container("QA1611P4-BOTTLE", "QA1611P4 Bottle", List.of("ENVIRONMENTAL"), serumId);
        retired = container("QA1611P4-OLD", "QA1611P4 Old tube", List.of("CLINICAL"), serumId);
        containerTypeService.setActive(retired.id(), false, "1");
    }

    @After
    public void forgetTheSampleTypeLinks() {
        typeOfSampleService.clearCache();
    }

    @Test
    public void testsSharingAPreferredContainerAreDrawnInOneWithTheLargestCountWhateverTheirOrder() {
        String creatinine = test(serumId, false, row(sst, "PREFERRED", 1), row(redTop, "ALTERNATE", 1));
        String glucose = test(serumId, false, row(sst, "PREFERRED", 2));

        ContainerProposalView view = proposeOne(serumId, glucose, creatinine);

        assertEquals(1, view.proposed().size());
        ContainerProposalView.Proposed only = view.proposed().get(0);
        assertEquals(sst.id(), only.containerTypeId());
        assertEquals(2, only.count());
        assertEquals(List.of(glucose, creatinine), only.testIds());
        assertTrue(view.unproposedTestIds().isEmpty());
        assertTrue(view.containerRequired());
        assertEquals("the sample key is echoed", "s1", view.key());
    }

    @Test
    public void aTestNeedingItsOwnContainerIsDrawnApartButSharesWithinItsPanel() {
        String shared = test(serumId, false, row(sst, "PREFERRED", 1));
        String ownA = test(serumId, true, row(sst, "PREFERRED", 1));
        String ownB = test(serumId, true, row(sst, "PREFERRED", 1));
        String ownAlone = test(serumId, true, row(sst, "PREFERRED", 1));

        String panelId = panel(ownA, ownB);

        ContainerProposalView view = propose(serumId, List.of(ownA, ownB, ownAlone, shared), List.of(panelId));

        assertEquals("shared, the panel's own and the lone test's own", 3, view.proposed().size());
        assertEquals(List.of(shared), view.proposed().get(0).testIds());
        assertEquals(List.of(ownA, ownB), view.proposed().get(1).testIds());
        assertEquals(List.of(panelId), view.proposed().get(1).panelIds());
        assertTrue(view.proposed().get(0).panelIds().isEmpty());
        assertEquals(List.of(ownAlone), view.proposed().get(2).testIds());
        assertTrue(view.proposed().stream().allMatch(proposed -> proposed.containerTypeId().equals(sst.id())));
        assertEquals("the shared container comes first whatever the test order", List.of(false, true, true),
                view.proposed().stream().map(ContainerProposalView.Proposed::ownContainer).toList());
    }

    @Test
    public void aTestMovesToItsAlternateWhenAnotherTestIsDrawingIt() {
        String onSst = test(serumId, false, row(sst, "PREFERRED", 1));
        String prefersRed = test(serumId, false, row(redTop, "PREFERRED", 1), row(sst, "ALTERNATE", 1));

        ContainerProposalView view = proposeOne(serumId, onSst, prefersRed);

        assertEquals(1, view.proposed().size());
        assertEquals(sst.id(), view.proposed().get(0).containerTypeId());
        assertEquals(List.of(onSst, prefersRed), view.proposed().get(0).testIds());

        ContainerProposalView alone = proposeOne(serumId, prefersRed);
        assertEquals("alone it keeps its preferred container", redTop.id(), alone.proposed().get(0).containerTypeId());
    }

    @Test
    public void aTestWithoutExpectedContainersGetsTheSampleTypesDefaultOrNothing() {
        String bare = test(serumId, false);

        ContainerProposalView none = proposeOne(serumId, bare);
        assertTrue(none.proposed().isEmpty());
        assertEquals(List.of(bare), none.unproposedTestIds());
        assertFalse("nothing proposed, so no container is required", none.containerRequired());

        testContainerService.setDefaultContainer(serumId, redTop.id());
        ContainerProposalView defaulted = proposeOne(serumId, bare);
        assertEquals(redTop.id(), defaulted.proposed().get(0).containerTypeId());
        assertEquals(1, defaulted.proposed().get(0).count());
        assertTrue(defaulted.unproposedTestIds().isEmpty());
    }

    @Test
    public void inactiveOtherTypeAndUsedAsContainersAreNeverProposed() {
        String creatinine = test(serumId, false, row(sst, "PREFERRED", 1), row(redTop, "ALTERNATE", 1));
        String potassium = test(plasmaId, false, row(pst, "PREFERRED", 1));
        String usedAs = test(serumId, false, row(sst, "PREFERRED", 1));
        testContainerService
                .save(usedAs,
                        new TestContainersForm(
                                List.of(row(sst, "PREFERRED", 1),
                                        new TestContainersForm.Row(pst.id(), "ALTERNATE", 1, null, null, serumId)),
                                false),
                        "1");
        containerTypeService.setActive(sst.id(), false, "1");

        ContainerProposalView serum = proposeOne(serumId, creatinine, potassium, usedAs);

        assertEquals("the retired preferred falls back to the active alternate", 1, serum.proposed().size());
        assertEquals(redTop.id(), serum.proposed().get(0).containerTypeId());
        assertEquals(List.of(potassium, usedAs), serum.unproposedTestIds());

        ContainerProposalView plasma = proposeOne(plasmaId, usedAs);
        assertTrue("a used-as row is not a container for the type it yields", plasma.proposed().isEmpty());
    }

    @Test
    public void aTestTheCatalogDoesNotHaveIsLeftOut() {
        String creatinine = test(serumId, false, row(sst, "PREFERRED", 1));

        ContainerProposalView view = proposeOne(serumId, "999999999", creatinine, "not-a-test");

        assertEquals(List.of(creatinine), view.proposed().get(0).testIds());
        assertTrue(view.unproposedTestIds().isEmpty());
    }

    @Test
    public void theChoicesAreTheActiveClinicalContainersYieldingTheSampleType() {
        ContainerProposalView view = proposeOne(serumId);

        assertEquals(List.of(redTop.id(), sst.id()),
                view.choices().stream().map(ContainerProposalView.Choice::id).toList());
        assertFalse(view.choices().stream().anyMatch(choice -> choice.id().equals(envBottle.id())));
        assertEquals(List.of(pst.id()),
                proposeOne(plasmaId).choices().stream().map(ContainerProposalView.Choice::id).toList());
    }

    @Test
    public void aSampleMayBeSavedOnlyInAContainerYieldingItsTypeAndStillInUse() {
        assertTrue(proposalService.canHold(sst.id(), serumId, null));
        assertFalse("PST yields plasma", proposalService.canHold(pst.id(), serumId, null));
        assertFalse("retired", proposalService.canHold(retired.id(), serumId, null));
        assertTrue("a sample keeps the retired container it already had",
                proposalService.canHold(retired.id(), serumId, retired.id()));
        assertFalse("an unknown container", proposalService.canHold(-1L, serumId, null));
    }

    private ContainerProposalView proposeOne(String sampleTypeId, String... testIds) {
        return propose(sampleTypeId, List.of(testIds), List.of());
    }

    private ContainerProposalView propose(String sampleTypeId, List<String> testIds, List<String> panelIds) {
        List<ContainerProposalView> views = proposalService.propose(new ContainerProposalForm(
                List.of(new ContainerProposalForm.Sample("s1", sampleTypeId, testIds, panelIds))));
        assertEquals(1, views.size());
        return views.get(0);
    }

    private String panel(String... testIds) {
        Long localizationId = jdbc.queryForObject("SELECT nextval('clinlims.localization_seq')", Long.class);
        jdbc.update("INSERT INTO clinlims.localization (id, description) VALUES (?, 'QA1611P4 panel')", localizationId);
        Long panelId = jdbc.queryForObject("SELECT nextval('clinlims.panel_seq')", Long.class);
        jdbc.update(
                "INSERT INTO clinlims.panel (id, name, description, is_active, name_localization_id)"
                        + " VALUES (?, ?, 'QA1611P4 panel', 'Y', ?)",
                panelId, "QA1611P4 panel " + panelId, localizationId);
        for (String testId : testIds) {
            jdbc.update("INSERT INTO clinlims.panel_item (id, panel_id, test_id)"
                    + " VALUES (nextval('clinlims.panel_item_seq'), ?, ?)", panelId, Long.valueOf(testId));
        }
        return String.valueOf(panelId);
    }

    private static TestContainersForm.Row row(ContainerTypeView container, String role, int count) {
        return new TestContainersForm.Row(container.id(), role, count, null, null, null);
    }

    private ContainerTypeView container(String code, String name, List<String> domains, String sampleTypeId) {
        return containerTypeService.save(null, new ContainerTypeForm(code, name, null, null, null, null, null, null,
                null, null, new ArrayList<>(domains), List.of(sampleTypeId)), "1");
    }

    private String test(String sampleTypeId, boolean needsOwnContainer, TestContainersForm.Row... rows) {
        org.openelisglobal.test.valueholder.Test created = fixtures.createCatalogTest();
        created.setDomain("CLINICAL");
        testService.update(created);
        String testId = created.getId();
        jdbc.update("INSERT INTO clinlims.sampletype_test (id, sample_type_id, test_id) VALUES"
                + " (nextval('clinlims.sample_type_test_seq'), ?::numeric, ?::numeric)", sampleTypeId, testId);
        typeOfSampleService.clearCache();
        if (rows.length > 0 || needsOwnContainer) {
            testContainerService.save(testId, new TestContainersForm(List.of(rows), needsOwnContainer), "1");
        }
        return testId;
    }
}
