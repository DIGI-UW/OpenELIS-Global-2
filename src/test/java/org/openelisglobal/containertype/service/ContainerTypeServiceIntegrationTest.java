package org.openelisglobal.containertype.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.configuration.service.ConfigurationLoadedEvent;
import org.openelisglobal.containertype.dao.ContainerTypeDAO;
import org.openelisglobal.containertype.form.ContainerTypeForm;
import org.openelisglobal.containertype.form.ContainerTypeView;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * OGC-1611 P1: the container types an install is seeded with, adding and
 * editing one, and deactivating one that samples still use.
 */
@Transactional
public class ContainerTypeServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ContainerTypeService containerTypeService;
    @Autowired
    private MicrobiologyTestFixtures fixtures;
    @Autowired
    private ContainerTypeDAO containerTypeDAO;
    @Autowired
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    private TypeOfSample sampleType;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        sampleType = fixtures.getOrCreateActiveSampleType();
    }

    @Test
    public void anInstallIsSeededWithTheClinicalContainersAndTheirCapColours() {
        List<ContainerTypeView> clinical = containerTypeService.list(null, "CLINICAL", null, true);

        assertTrue("Appendix A seeds 35 clinical container types", clinical.size() >= 35);
        ContainerTypeView edta = only(containerTypeService.list("K2EDTA tube 4 mL", null, null, true));
        assertEquals("K2EDTA", edta.additive());
        assertEquals("#B57EDC", edta.capColourHex());
        assertEquals("Lavender", edta.capColourName());
        assertEquals(0, new BigDecimal("4").compareTo(edta.nominalVolume()));
        assertEquals(List.of("CLINICAL"), edta.domains());
    }

    @Test
    public void aContainerTypeIsAddedWithItsDomainsAndTheSampleTypesItYields() {
        ContainerTypeView added = containerTypeService.save(null,
                form("qa-vial", "QA vial", List.of("environmental", "vector"), "#1e88e5"), "1");

        assertEquals("QA-VIAL", added.code());
        assertEquals("#1E88E5", added.capColourHex());
        assertEquals(List.of("ENVIRONMENTAL", "VECTOR"), added.domains());
        assertEquals(List.of(sampleType.getId()), added.sampleTypes().stream().map(pair -> pair.getId()).toList());
        assertEquals(1, containerTypeService.list("qa vial", "VECTOR", null, false).size());
        assertTrue(containerTypeService.list("qa vial", "CLINICAL", null, false).isEmpty());

        assertEquals("the column records who added it", Integer.valueOf(1),
                containerTypeDAO.get(added.id()).orElseThrow().getChangedBy());

        ContainerTypeView edited = containerTypeService.save(added.id(),
                form("QA-VIAL", "QA vial 10 mL", List.of("ENVIRONMENTAL"), null), "1");
        assertEquals("QA vial 10 mL", edited.name());
        assertEquals(List.of("ENVIRONMENTAL"), edited.domains());
    }

    @Test
    public void aContainerTypeTheFormCannotDescribeIsRefusedWithTheField() {
        containerTypeService.save(null, form("QA-ONE", "QA one", List.of("CLINICAL"), null), "1");

        assertRefused("name", "nameRequired", form("QA-TWO", " ", List.of("CLINICAL"), null));
        assertRefused("code", "codeTaken", form("qa-one", "QA two", List.of("CLINICAL"), null));
        assertRefused("code", "codeInvalid", form("has space", "QA two", List.of("CLINICAL"), null));
        assertRefused("domains", "domainRequired", form("QA-TWO", "QA two", List.of(), null));
        assertRefused("domains", "domainInvalid", form("QA-TWO", "QA two", List.of("HUMAN"), null));
        assertRefused("capColourHex", "colourInvalid", form("QA-TWO", "QA two", List.of("CLINICAL"), "red"));
    }

    @Test
    public void aDeactivatedContainerTypeIsKeptAndNamesTheSamplesStillUsingIt() {
        ContainerTypeView added = containerTypeService.save(null,
                form("QA-USED", "QA used tube", List.of("CLINICAL"), null), "1");
        SampleItem item = fixtures.createSampleWithSampleItem("CTU");
        jdbcTemplate.update("UPDATE clinlims.sample_item SET container = ? WHERE id = ?::numeric", "QA used tube",
                item.getId());

        assertEquals(1, containerTypeService.usage(added.id()).orElseThrow().samples());
        ContainerTypeView deactivated = containerTypeService.setActive(added.id(), false, "1").orElseThrow();

        assertFalse(deactivated.active());
        assertTrue(containerTypeService.list("QA used", null, null, false).isEmpty());
        assertEquals(1, containerTypeService.list("QA used", null, null, true).size());
        assertTrue(containerTypeService.setActive(added.id(), true, "1").orElseThrow().active());
    }

    private ContainerTypeForm form(String code, String name, List<String> domains, String hex) {
        return new ContainerTypeForm(code, name, "Other", "None", "Plastic", new BigDecimal("10"), "mL", hex, null,
                null, domains, List.of(sampleType.getId()));
    }

    private void assertRefused(String field, String code, ContainerTypeForm form) {
        ContainerTypeRefusedException refused = assertThrows(ContainerTypeRefusedException.class,
                () -> containerTypeService.save(null, form, "1"));
        assertEquals(field, refused.getField());
        assertEquals(code, refused.getCode());
    }

    private static ContainerTypeView only(List<ContainerTypeView> views) {
        assertEquals(1, views.size());
        return views.get(0);
    }

    @Test
    public void aValueLongerThanItsColumnIsRefusedOnItsField() {
        assertRefused("name", "tooLong", form("LONG-NAME", "N".repeat(121), List.of("CLINICAL"), null));
        ContainerTypeForm longColour = new ContainerTypeForm("LONG-COLOUR", "Long colour", null, null, null, null, null,
                null, "C".repeat(41), null, List.of("CLINICAL"), null);
        assertRefused("capColourName", "tooLong", longColour);
        ContainerTypeForm hugeVolume = new ContainerTypeForm("HUGE", "Huge", null, null, null,
                new BigDecimal("100000000"), "mL", null, null, null, List.of("CLINICAL"), null);
        assertRefused("nominalVolume", "volumeTooLarge", hugeVolume);
    }

    @Test
    public void theEnvironmentalContainerDictionaryIsImportedAfterConfigurationLoads() {
        jdbcTemplate.update("INSERT INTO clinlims.dictionary_category (id, name, description, lastupdated)"
                + " VALUES (9611, 'Sample Container', 'Sample Container', now())");
        jdbcTemplate.update(
                "INSERT INTO clinlims.dictionary (id, dictionary_category_id, dict_entry, is_active, lastupdated)"
                        + " VALUES (96111, 9611, 'Amber glass bottle 1 L', 'Y', now())");

        eventPublisher.publishEvent(new ConfigurationLoadedEvent("startup"));

        ContainerTypeView amber = only(
                containerTypeService.list("Amber glass bottle 1 L", "ENVIRONMENTAL", null, true));
        assertEquals("ENV-96111", amber.code());
        assertEquals("a second load adds nothing", 0, containerTypeService.importEnvironmentalContainers());
    }
}
