package org.openelisglobal.microbiology;

import static org.junit.Assert.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.hibernate.Hibernate;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.inventory.service.*;
import org.openelisglobal.inventory.valueholder.*;
import org.openelisglobal.inventory.valueholder.InventoryEnums.*;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.form.*;
import org.openelisglobal.microbiology.service.*;
import org.openelisglobal.microbiology.valueholder.*;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;

/**
 * Real migrations, ORM, service authorization, and history; browser layout is
 * covered separately.
 */
@org.springframework.transaction.annotation.Transactional
public class MicroCultureIntegrationTest extends BaseWebContextSensitiveTest {
    @Autowired
    private MicrobiologyTestFixtures fixtures;
    @Autowired
    private MicroCultureService service;
    @Autowired
    private MicroCultureDAO cultures;
    @Autowired
    private MicroCaseDAO cases;
    @Autowired
    private MicroCaseMembershipService membership;
    @Autowired
    private MicroCaseResultService results;
    @Autowired
    private org.openelisglobal.typeofsample.service.TypeOfSampleTestService sampleTests;
    @Autowired
    private org.openelisglobal.sampleitem.service.SampleItemService sampleItems;
    @Autowired
    private org.openelisglobal.test.service.TestSectionService units;
    @Autowired
    private InventoryItemService items;
    @Autowired
    private InventoryLotService lots;
    @Autowired
    private DictionaryService dictionaries;
    @Autowired
    private SystemUserService users;
    @Autowired
    private RoleService roles;
    @PersistenceContext
    private EntityManager em;
    private MicroCase microCase;
    private SampleItem specimen;
    private InventoryItem medium;
    private InventoryLot lot;
    private String actor, outsider;
    private Timestamp received;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/role.xml");
        executeDataSetWithStateManagement("testdata/system-user.xml");
        var unit = fixtures.createLabUnit();
        actor = writer(unit.getId());
        outsider = writer(fixtures.createLabUnit().getId());
        specimen = fixtures.createSampleWithSampleItem("M4");
        specimen.setTypeOfSample(fixtures.getOrCreateActiveSampleType());
        received = Timestamp.from(Instant.now().minusSeconds(172800));
        specimen.setReceivedDate(received);
        specimen.getSample().setReceivedTimestamp(received);
        em.merge(specimen.getSample());
        em.merge(specimen);
        microCase = new MicroCase();
        microCase.setSampleId(specimen.getSample().getId());
        microCase.setSampleItemId(specimen.getId());
        microCase.setSampleTypeId(specimen.getTypeOfSample().getId());
        microCase.setLabUnitId(unit.getId());
        microCase.setCreatedBy(actor);
        cases.insert(microCase);
        membership.addSample(microCase.getId(), specimen.getId(), actor);
        medium = new InventoryItem();
        medium.setCode("M4" + UUID.randomUUID());
        medium.setName("M4 medium");
        medium.setItemType(ItemType.REAGENT);
        medium.setUnits("plates");
        medium.setFhirUuid(UUID.randomUUID());
        medium.setMicrobiologyMedium(true);
        medium.setTrackLots(true);
        medium.setSysUserId(actor);
        items.insert(medium);
        lot = new InventoryLot();
        lot.setFhirUuid(UUID.randomUUID());
        lot.setInventoryItem(medium);
        lot.setLotNumber("M4" + UUID.randomUUID());
        lot.setInitialQuantity(100.0);
        lot.setCurrentQuantity(100.0);
        lot.setStatus(LotStatus.ACTIVE);
        lot.setQcStatus(QCStatus.PASSED);
        lot.setExpirationDate(Timestamp.from(Instant.now().plusSeconds(864000)));
        lot.setSysUserId(actor);
        lots.insert(lot);
        em.flush();
    }

    private String writer(String unit) {
        var u = new SystemUser();
        u.setLoginName("M4" + UUID.randomUUID().toString().substring(0, 12));
        u.setFirstName("Culture" + UUID.randomUUID().toString().substring(0, 8));
        u.setLastName("Writer");
        u.setIsActive("Y");
        u.setIsEmployee("Y");
        u.setSysUserId("1");
        String id = users.insert(u);
        var assignment = new org.openelisglobal.userrole.valueholder.UserLabUnitRoles();
        assignment.setId(Integer.valueOf(id));
        var map = new org.openelisglobal.userrole.valueholder.LabUnitRoleMap();
        map.setLabUnit(unit);
        map.setRoles(Set.of(roles.getRoleByName(Constants.ROLE_RESULTS).getId()));
        assignment.setLabUnitRoleMap(Set.of(map));
        em.persist(assignment);
        return id;
    }

    private String code(String category, int index) {
        return dictionaries.getActiveSortedEntriesByCategoryName(category).get(index).getId();
    }

    private MicroCultureRequestForm request() {
        var r = new MicroCultureRequestForm();
        r.sourceSampleItemId = specimen.getId();
        r.containerIdentifier = "PLATE-" + UUID.randomUUID().toString().substring(0, 8);
        r.mediumItemId = medium.getId();
        r.lotId = lot.getId();
        r.atmosphereId = code("Culture atmosphere", 0);
        r.duration = new BigDecimal("48");
        r.durationUnit = "HOURS";
        r.temperature = new BigDecimal("35");
        r.checkIntervalHours = new BigDecimal("8");
        r.loopVolume = BigDecimal.ONE;
        r.inoculatedAt = Timestamp.from(Instant.now().minusSeconds(3600));
        return r;
    }

    private MicroCultureForm create() {
        return service.inoculate(microCase.getId(), request(), actor).get(0);
    }

    private List<MicroCultureForm> reload() {
        em.flush();
        em.clear();
        return service.getRows(microCase.getId());
    }

    private void rejects(Runnable operation, String message) {
        try {
            operation.run();
            fail("Expected " + message);
        } catch (IllegalArgumentException e) {
            assertEquals(message, e.getMessage());
        }
    }

    @Test
    public void persistsCultureTreeAndLotWithoutConsumingStock() {
        var parent = create();
        var child = request();
        child.parentId = parent.id;
        child.subculturePurpose = "ACTIVE_SCREENING";
        service.inoculate(microCase.getId(), child, actor);
        var loaded = reload();
        assertEquals(2, loaded.size());
        assertEquals(parent.id, loaded.get(1).parentId);
        assertEquals("ACTIVE_SCREENING", loaded.get(1).subculturePurpose);
        assertEquals(specimen.getId(), loaded.get(1).sourceSampleItemId);
        assertEquals(lot.getId(), loaded.get(1).lotId);
        assertEquals(new BigDecimal("35.00"), loaded.get(1).temperature);
        assertEquals(100.0, lots.get(lot.getId()).getCurrentQuantity(), 0.0);
    }

    @Test
    public void readingsAppendAndLockTheOriginalClock() {
        var row = create();
        var edit = new MicroCultureRequestForm();
        edit.inoculatedAt = received;
        service.act(microCase.getId(), row.id, "inoculated-time", edit, actor);
        var read = new MicroCultureRequestForm();
        read.readingId = code("Culture reading", 0);
        read.quantityId = code("Culture quantity", 0);
        read.note = "first";
        service.act(microCase.getId(), row.id, "reading", read, actor);
        read.note = "second";
        service.act(microCase.getId(), row.id, "reading", read, actor);
        rejects(() -> service.act(microCase.getId(), row.id, "inoculated-time", edit, actor),
                "MICROBIOLOGY_CULTURE_FIRST_READING_LOCK");
        var loaded = reload().get(0);
        assertEquals(received, loaded.inoculatedAt);
        assertEquals(2, loaded.readings.size());
        assertEquals("first", loaded.readings.get(0).note());
        assertEquals("second", loaded.readings.get(1).note());
        assertNotEquals(loaded.readings.get(0).id(), loaded.readings.get(1).id());
    }

    @Test
    public void extensionsAddTimeWithoutRestartingIncubulation() {
        var row = create();
        var r = new MicroCultureRequestForm();
        r.extendBy = new BigDecimal("12");
        r.unit = "HOURS";
        r.reasonId = code("Extend incubation reason", 0);
        service.act(microCase.getId(), row.id, "extension", r, actor);
        service.act(microCase.getId(), row.id, "extension", r, actor);
        var loaded = reload().get(0);
        assertEquals(2, loaded.extensions.size());
        assertEquals(row.inoculatedAt, loaded.inoculatedAt);
        assertEquals(72 * 3600000L, loaded.incubationEnds.getTime() - loaded.inoculatedAt.getTime());
    }

    @Test
    public void rejectsPrecisionThatTheDatabaseCannotRetain() {
        var r = request();
        r.duration = new BigDecimal("0.001");
        rejects(() -> service.inoculate(microCase.getId(), r, actor), "MICROBIOLOGY_CULTURE_INVALID_DURATION");
        r.duration = new BigDecimal("1.2300");
        r.checkIntervalHours = new BigDecimal("0.001");
        rejects(() -> service.inoculate(microCase.getId(), r, actor), "MICROBIOLOGY_CULTURE_INVALID_DURATION");
        r.checkIntervalHours = BigDecimal.ONE;
        var row = service.inoculate(microCase.getId(), r, actor).get(0);
        var ext = new MicroCultureRequestForm();
        ext.extendBy = new BigDecimal("0.001");
        ext.unit = "HOURS";
        ext.reasonId = code("Extend incubation reason", 0);
        rejects(() -> service.act(microCase.getId(), row.id, "extension", ext, actor),
                "MICROBIOLOGY_CULTURE_INVALID_DURATION");
        var loaded = reload().get(0);
        assertEquals(new BigDecimal("1.23"), loaded.duration);
        assertTrue(loaded.extensions.isEmpty());
    }

    @Test
    public void emptyLotIsNeitherOfferedNorAccepted() {
        lot.setCurrentQuantity(0.0);
        lots.update(lot);
        em.flush();
        assertTrue(service.getOptions(microCase.getId()).media.stream().filter(m -> m.id().equals(medium.getId()))
                .findFirst().orElseThrow().lots().isEmpty());
        rejects(() -> service.inoculate(microCase.getId(), request(), actor), "MICROBIOLOGY_CULTURE_LOT");
        assertTrue(reload().isEmpty());
    }

    @Test
    public void trackedMediaRequiresUsableMatchingLot() {
        var r = request();
        r.notTracked = true;
        r.lotId = null;
        rejects(() -> service.inoculate(microCase.getId(), r, actor), "MICROBIOLOGY_CULTURE_TRACKED_MEDIA_REQUIRED");
        r.notTracked = false;
        rejects(() -> service.inoculate(microCase.getId(), r, actor), "MICROBIOLOGY_CULTURE_LOT");
        r.lotId = lot.getId();
        lot.setQcStatus(QCStatus.PENDING);
        lots.update(lot);
        rejects(() -> service.inoculate(microCase.getId(), r, actor), "MICROBIOLOGY_CULTURE_LOT");
        lot.setQcStatus(QCStatus.PASSED);
        lot.setExpirationDate(received);
        lots.update(lot);
        rejects(() -> service.inoculate(microCase.getId(), r, actor), "MICROBIOLOGY_CULTURE_LOT");
    }

    @Test
    public void labUnitCanRequireTrackedMediaForAnOtherwiseUntrackedMedium() {
        medium.setTrackLots(false);
        items.update(medium);
        var unit = em.find(org.openelisglobal.test.valueholder.TestSection.class, microCase.getLabUnitId());
        unit.setRequireTrackedMedia(true);
        em.flush();
        var r = request();
        r.notTracked = true;
        r.lotId = null;
        rejects(() -> service.inoculate(microCase.getId(), r, actor), "MICROBIOLOGY_CULTURE_TRACKED_MEDIA_REQUIRED");
    }

    @Test
    public void timeAndRequiredEnvironmentBoundaries() {
        var r = request();
        r.inoculatedAt = new Timestamp(received.getTime() - 1);
        rejects(() -> service.inoculate(microCase.getId(), r, actor), "MICROBIOLOGY_CULTURE_INOCULATED_TIME");
        r.inoculatedAt = Timestamp.from(Instant.now().plusSeconds(60));
        rejects(() -> service.inoculate(microCase.getId(), r, actor), "MICROBIOLOGY_CULTURE_INOCULATED_TIME");
        r.inoculatedAt = received;
        r.atmosphereId = null;
        rejects(() -> service.inoculate(microCase.getId(), r, actor), "MICROBIOLOGY_CULTURE_CODE");
        r.atmosphereId = code("Culture atmosphere", 0);
        var row = service.inoculate(microCase.getId(), r, actor).get(0);
        var positive = new MicroCultureRequestForm();
        positive.positiveAt = new Timestamp(received.getTime() - 1);
        rejects(() -> service.act(microCase.getId(), row.id, "positive-time", positive, actor),
                "MICROBIOLOGY_CULTURE_POSITIVE_TIME");
        positive.positiveAt = Timestamp.from(Instant.now().plusSeconds(60));
        rejects(() -> service.act(microCase.getId(), row.id, "positive-time", positive, actor),
                "MICROBIOLOGY_CULTURE_POSITIVE_TIME");
        positive.positiveAt = received;
        service.act(microCase.getId(), row.id, "positive-time", positive, actor);
        assertEquals(new BigDecimal("0.00"), reload().get(0).timeToPositivityHours);
    }

    @Test
    public void proposalNeedsConfirmationAndOutcomesStayOnTheirOwnRows() {
        var parent = create();
        var r = request();
        r.parentId = parent.id;
        r.subculturePurpose = "CLINICAL_DIAGNOSTIC";
        var child = service.inoculate(microCase.getId(), r, actor).get(1);
        var signal = new MicroCultureRequestForm();
        signal.signal = "Instrument negative after cycle";
        service.act(microCase.getId(), parent.id, "negative-proposal", signal, actor);
        var loaded = reload();
        assertNull(loaded.get(0).outcome);
        assertNull(loaded.get(0).proposals.get(0).confirmedAt());
        var outcome = new MicroCultureRequestForm();
        outcome.outcome = "NO_GROWTH";
        outcome.proposalId = loaded.get(0).proposals.get(0).id();
        service.act(microCase.getId(), parent.id, "outcome", outcome, actor);
        outcome = new MicroCultureRequestForm();
        outcome.outcome = "GROWTH";
        service.act(microCase.getId(), child.id, "outcome", outcome, actor);
        loaded = reload();
        assertEquals("NO_GROWTH", loaded.get(0).outcome);
        assertEquals("GROWTH", loaded.get(1).outcome);
        assertEquals(actor, loaded.get(0).proposals.get(0).confirmedBy());
        assertNotNull(loaded.get(0).proposals.get(0).confirmedAt());
    }

    @Test
    public void eagerCollectionsAreInitializedBeforeLeavingTheDao() {
        create();
        em.flush();
        em.clear();
        var row = cultures.getRows(microCase.getId()).get(0);
        assertTrue(Hibernate.isInitialized(row.getReadings()));
        assertTrue(Hibernate.isInitialized(row.getExtensions()));
        assertTrue(Hibernate.isInitialized(row.getProposals()));
    }

    @Test
    public void outsidersAndFinalReleasedCasesCannotCreateOrChangeCultures() {
        try {
            service.inoculate(microCase.getId(), request(), outsider);
            fail();
        } catch (AccessDeniedException expected) {
        }
        var row = create();
        var deniedReading = new MicroCultureRequestForm();
        deniedReading.readingId = code("Culture reading", 0);
        try {
            service.act(microCase.getId(), row.id, "reading", deniedReading, outsider);
            fail();
        } catch (AccessDeniedException expected) {
        }
        assertTrue(reload().get(0).readings.isEmpty());
        microCase = cases.get(microCase.getId()).orElseThrow();
        microCase.setStage("FINAL_RELEASED");
        cases.update(microCase);
        var r = new MicroCultureRequestForm();
        r.readingId = code("Culture reading", 0);
        try {
            service.act(microCase.getId(), row.id, "reading", r, actor);
            fail();
        } catch (MicroCaseLockedException expected) {
        }
        assertTrue(reload().get(0).readings.isEmpty());
    }

    @Test
    public void caseMemberAliquotCanCarryItsOwnCultureAndTest() {
        var aliquot = new SampleItem();
        aliquot.setSample(specimen.getSample());
        aliquot.setParentSampleItem(specimen);
        aliquot.setTypeOfSample(specimen.getTypeOfSample());
        aliquot.setReceivedDate(received);
        aliquot.setSortOrder("2");
        aliquot.setStatusId(specimen.getStatusId());
        aliquot.setSysUserId(actor);
        sampleItems.insert(aliquot);
        var r = request();
        r.sourceSampleItemId = aliquot.getId();
        var culture = service.inoculate(microCase.getId(), r, actor).get(0);
        var catalog = fixtures.createCatalogTest();
        catalog.setOrderable(true);
        em.merge(catalog);
        var allowed = new org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest();
        allowed.setTypeOfSampleId(specimen.getTypeOfSample().getId());
        allowed.setTestId(catalog.getId());
        allowed.setSysUserId(actor);
        sampleTests.insert(allowed);
        var select = new MicroCaseAddTestsForm();
        select.placement = "CULTURE";
        select.cultureId = culture.id;
        select.sampleItemId = aliquot.getId();
        select.testIds = List.of(catalog.getId());
        var added = results.addTests(microCase.getId(), select, actor);
        em.flush();
        em.clear();
        assertEquals(culture.id, added.get(0).cultureId);
        assertEquals("CULTURE", added.get(0).placement);
        assertEquals(aliquot.getId(), service.getRows(microCase.getId()).get(0).sourceSampleItemId);
        assertEquals(2, service.getOptions(microCase.getId()).sources.size());
    }

    @Test
    public void foreignSourcesAreRejected() {
        var other = fixtures.createSampleWithSampleItem("OTHER");
        var r = request();
        r.sourceSampleItemId = other.getId();
        rejects(() -> service.inoculate(microCase.getId(), r, actor), "MICROBIOLOGY_CULTURE_SOURCE");
        assertTrue(reload().isEmpty());
    }

    @Test
    public void lateGrowthReopensOnlyItsOwnRow() {
        var row = create();
        var r = new MicroCultureRequestForm();
        r.outcome = "NO_GROWTH";
        service.act(microCase.getId(), row.id, "outcome", r, actor);
        r.outcome = "GROWTH";
        service.act(microCase.getId(), row.id, "outcome", r, actor);
        assertEquals("GROWTH", reload().get(0).outcome);
        assertNotNull(service.getRows(microCase.getId()).get(0).positiveAt);
        var extension = new MicroCultureRequestForm();
        extension.extendBy = new BigDecimal("12");
        extension.unit = "HOURS";
        extension.reasonId = code("Extend incubation reason", 0);
        service.act(microCase.getId(), row.id, "extension", extension, actor);
        var reading = new MicroCultureRequestForm();
        reading.readingId = code("Culture reading", 0);
        service.act(microCase.getId(), row.id, "reading", reading, actor);
        var loaded = reload().get(0);
        assertEquals("GROWTH", loaded.outcome);
        assertEquals(1, loaded.extensions.size());
        assertEquals(1, loaded.readings.size());
    }

    @Test
    public void rejectsLoopAndTemperaturePrecisionWithoutRoundingStoredValues() {
        var r = request();
        r.loopVolume = new BigDecimal("0.001");
        rejects(() -> service.inoculate(microCase.getId(), r, actor), "MICROBIOLOGY_CULTURE_LOOP_VOLUME");
        r.loopVolume = new BigDecimal("100000000");
        rejects(() -> service.inoculate(microCase.getId(), r, actor), "MICROBIOLOGY_CULTURE_LOOP_VOLUME");
        r.loopVolume = new BigDecimal("1.2300");
        r.temperature = new BigDecimal("35.001");
        rejects(() -> service.inoculate(microCase.getId(), r, actor), "MICROBIOLOGY_CULTURE_TEMPERATURE");
        r.temperature = new BigDecimal("35.25");
        service.inoculate(microCase.getId(), r, actor);
        var loaded = reload().get(0);
        assertEquals(new BigDecimal("1.23"), loaded.loopVolume);
        assertEquals(new BigDecimal("35.25"), loaded.temperature);
    }

    @Test
    public void configuredGramOptionsExposeOnlyItsCatalogSampleTypes() {
        var catalog = fixtures.createCatalogTest();
        var unit = units.get(microCase.getLabUnitId());
        catalog.setTestSection(unit);
        catalog.setOrderable(true);
        em.merge(catalog);
        unit.setGramStainTestId(catalog.getId());
        unit.setSysUserId(actor);
        units.update(unit);
        var allowed = new org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest();
        allowed.setTypeOfSampleId(specimen.getTypeOfSample().getId());
        allowed.setTestId(catalog.getId());
        allowed.setSysUserId(actor);
        sampleTests.insert(allowed);
        em.flush();
        em.clear();
        var options = service.getOptions(microCase.getId());
        assertEquals(catalog.getId(), options.gramStainTest.getId());
        assertEquals(List.of(specimen.getTypeOfSample().getId()), options.gramStainSampleTypeIds);
    }
}
