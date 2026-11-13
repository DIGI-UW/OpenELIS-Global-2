package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.configuration.service.CatalogImportServiceImpl;
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroWhonetAntibioticDAO;
import org.openelisglobal.microbiology.service.MicroWhonetAntibioticConfigurationHandler;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroWhonetAntibiotic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/** Loads rows cut unchanged from WHONET's Antibiotics.txt, release v26.3.26. */
public class MicroWhonetAntibioticLoadIntegrationTest extends BaseWebContextSensitiveTest {
    private static final String FILE = "testdata/microbiology/whonet-antibiotics.txt";

    @Autowired
    private MicroWhonetAntibioticConfigurationHandler handler;
    @Autowired
    private MicroAntibioticDAO antibiotics;
    @Autowired
    private MicroWhonetAntibioticDAO whonetTable;
    @Autowired
    private ApplicationContext context;

    @Before
    public void laboratoryAntibiotics() throws Exception {
        executeDataSetWithStateManagement("testdata/microbiology/whonet-antibiotic-load.xml");
    }

    private void load() throws Exception {
        try (InputStream file = getClass().getClassLoader().getResourceAsStream(FILE)) {
            handler.processConfiguration(file, "Antibiotics.txt");
        }
    }

    private MicroAntibiotic coded(String code) {
        return antibiotics.findByWhonetCodeIgnoreCase(code).orElseThrow();
    }

    @Test
    public void eachCodeIsOneAntibioticUnderWhonetsNameAndCode() throws Exception {
        load();

        assertEquals("Amoxicillin/Clavulanic acid", coded("AMC").getDisplayName());
        assertEquals("AMC", coded("AMC").getWhonetCode());
        assertEquals("Beta-lactam+Inhibitors", coded("AMC").getAntibioticClass());
        assertEquals("WHONET", coded("AMC").getReferenceSource());
        assertEquals("AMC", coded("AMC").getReferenceCode());
        assertEquals(1, antibiotics.getAll().stream().filter(a -> "AMC".equals(a.getWhonetCode())).count());
    }

    @Test
    public void aCodeWithSeveralRowsTakesTheNameAndClassOfItsFirstWhonetEntry() throws Exception {
        load();

        assertEquals("Sulfadimethoxine", coded("SDM").getDisplayName());
        assertEquals("Cephems-Oral", coded("CPD").getAntibioticClass());
    }

    @Test
    public void theTableIsHeldWholeWithOneRowPerPotency() throws Exception {
        load();

        List<MicroWhonetAntibiotic> rows = whonetTable.getAll();
        assertEquals(14, rows.size());
        MicroWhonetAntibiotic haemophilus = rows.stream().filter(row -> "2/1µg".equals(row.getPotency())).findFirst()
                .orElseThrow();
        assertEquals("AMC", haemophilus.getWhonetAbxCode());
        assertEquals("BSAC,EUCAST(Haemophilus)", haemophilus.getGuidelines());
        assertEquals("J01CR02", haemophilus.getAtcCode());
        assertEquals("306", haemophilus.getAbxNumber());
        assertEquals("Access", haemophilus.getWhoAware());
    }

    @Test
    public void aLaboratorysAntibioticKeepsItsRecordAndTakesWhonetsNameAndCode() throws Exception {
        load();

        MicroAntibiotic amikacin = coded("AMK");
        assertEquals("lab-amikacin", amikacin.getId());
        assertEquals("Lab note", amikacin.getNotes());
        assertEquals("Amikacin", amikacin.getDisplayName());
        assertEquals("AMK", amikacin.getWhonetCode());
        assertEquals("lab-uncoded-kanamycin", coded("KAN").getId());
    }

    @Test
    public void loadingTheSameFileAgainChangesNothing() throws Exception {
        load();
        assertEquals(2, handler.getLastSummary().getUpdated());
        assertEquals(12, handler.getLastSummary().getCreated());
        String amoxicillin = coded("AMC").getId();
        int count = antibiotics.getAll().size();

        load();

        assertEquals(amoxicillin, coded("AMC").getId());
        assertEquals(count, antibiotics.getAll().size());
        assertEquals(14, whonetTable.getAll().size());
        assertEquals(14, handler.getLastSummary().getCreated());
        assertEquals(0, handler.getLastSummary().getUpdated());
    }

    @Test
    public void aNameHeldByAnotherCodeFailsTheLoadAndLeavesTheTableAlone() throws Exception {
        load();
        String file = "WHONET_ABX_CODE\tANTIBIOTIC\tCLASS\tABX_NUMBER\r\n" + "ZZZ\tAmikacin\tAminoglycosides\t999\r\n";

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> handler.processConfiguration(new ByteArrayInputStream(file.getBytes(StandardCharsets.UTF_8)),
                        "Antibiotics.txt"));

        assertTrue(failure.getMessage().contains("AMK"));
        assertEquals(14, whonetTable.getAll().size());
        assertFalse(antibiotics.findByWhonetCodeIgnoreCase("ZZZ").isPresent());
    }

    @Test
    public void importCatalogDoesNotOfferTheTable() {
        CatalogImportServiceImpl catalogImport = new CatalogImportServiceImpl();
        context.getAutowireCapableBeanFactory().autowireBean(catalogImport);

        assertFalse(catalogImport.getImportableDomains().contains(handler.getDomainName()));
    }
}
