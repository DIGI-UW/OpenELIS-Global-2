package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.dao.MicroWhonetOrganismDAO;
import org.openelisglobal.microbiology.form.MicroOrganismAdminForm;
import org.openelisglobal.microbiology.service.MicroWhonetOrganismConfigurationHandler;
import org.openelisglobal.microbiology.service.MicrobiologyReferenceAdminService;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.openelisglobal.microbiology.valueholder.MicroWhonetOrganism;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/** Loads rows cut unchanged from WHONET's Organisms.txt, release v26.3.26. */
public class MicroWhonetOrganismLoadIntegrationTest extends BaseWebContextSensitiveTest {
    private static final String FILE = "testdata/microbiology/whonet-organisms.txt";

    @Autowired
    private MicroWhonetOrganismConfigurationHandler handler;
    @Autowired
    private MicroOrganismDAO organisms;
    @Autowired
    private MicroWhonetOrganismDAO whonetTable;
    @Autowired
    private MicrobiologyReferenceAdminService admin;
    @Autowired
    private ApplicationContext context;

    @Before
    public void laboratoryOrganisms() throws Exception {
        executeDataSetWithStateManagement("testdata/microbiology/whonet-organism-load.xml");
    }

    private void load() throws Exception {
        try (InputStream file = getClass().getClassLoader().getResourceAsStream(FILE)) {
            handler.processConfiguration(file, "Organisms.txt");
        }
    }

    private MicroOrganism coded(String code) {
        return organisms.findByWhonetCodeIgnoreCase(code).orElseThrow();
    }

    @Test
    public void everyCurrentEntryIsAnOrganismUnderWhonetsNameAndCode() throws Exception {
        load();

        assertEquals("Klebsiella pneumoniae ss. pneumoniae", coded("kpn").getDisplayName());
        assertEquals("Klebsiella sp.", coded("kl-").getDisplayName());
        assertEquals("Escherichia coli O157:H7", coded("157").getDisplayName());
        assertEquals("Kluyveromyces marxianus", coded("kma").getDisplayName());
        assertEquals("kpn", coded("kpn").getWhonetCode());
        assertEquals("WHONET", coded("kl-").getReferenceSource());
        assertEquals("kl-", coded("kl-").getReferenceCode());
    }

    @Test
    public void theTableIsHeldWholeWithOlderNamesAndReplacedCodes() throws Exception {
        load();

        List<MicroWhonetOrganism> rows = whonetTable.getAll();
        assertEquals(9, rows.size());
        assertTrue(rows.stream().anyMatch(row -> "kpn".equals(row.getWhonetOrgCode())
                && "Klebsiella pneumoniae".equals(row.getOrganism()) && "O".equals(row.getTaxonomicStatus())));
        MicroWhonetOrganism retired = rows.stream().filter(row -> "Candida pseudotropicalis".equals(row.getOrganism()))
                .findFirst().orElseThrow();
        assertEquals("kma", retired.getReplacedBy());
        assertEquals("CAN", retired.getGenusCode());
        assertEquals("3265006", retired.getSctCode());
        MicroWhonetOrganism klebsiella = rows.stream()
                .filter(row -> "Klebsiella pneumoniae ss. pneumoniae".equals(row.getOrganism())).findFirst()
                .orElseThrow();
        assertEquals("X", klebsiella.getCommon());
        assertEquals("EBC", klebsiella.getFamilyCode());
    }

    @Test
    public void aCodeWithNoCurrentEntryIsNotAnOrganism() throws Exception {
        load();

        assertFalse(organisms.findByWhonetCodeIgnoreCase("cps").isPresent());
        assertFalse(organisms.findByDisplayNameIgnoreCase("Candida kefyr").isPresent());
    }

    @Test
    public void aLaboratorysOrganismKeepsItsRecordAndTakesWhonetsNameAndCode() throws Exception {
        load();

        MicroOrganism klebsiella = coded("kpn");
        assertEquals("lab-klebsiella", klebsiella.getId());
        assertEquals("Lab note", klebsiella.getNotes());
        assertEquals("Klebsiella pneumoniae ss. pneumoniae", klebsiella.getDisplayName());
        assertEquals("lab-uncoded-ecoli", coded("eco").getId());
    }

    @Test
    public void anOrganismWhoseWhonetCodeHasADashIsSavedFromTheAdminForm() throws Exception {
        load();
        MicroOrganismAdminForm form = admin.getOrganism(coded("kl-").getId());
        form.organismGroup = "Enterobacterales";
        form.initialSignificance = "USUALLY";

        admin.saveOrganism(form.id, form, "1");

        assertEquals("kl-", coded("kl-").getWhonetCode());
        assertEquals("Enterobacterales", coded("kl-").getOrganismGroup());
    }

    @Test
    public void aWhonetOrganismIsSavedWithNoGroupOrSignificance() throws Exception {
        load();
        MicroOrganismAdminForm form = admin.getOrganism(coded("kl-").getId());
        form.notes = "Seen in wound swabs";

        admin.saveOrganism(form.id, form, "1");

        assertEquals("Seen in wound swabs", coded("kl-").getNotes());
        assertNull(coded("kl-").getOrganismGroup());
        assertNull(coded("kl-").getInitialSignificance());
    }

    @Test
    public void emptyingTheGroupAndSignificanceClearsThem() throws Exception {
        load();
        MicroOrganismAdminForm form = admin.getOrganism(coded("kl-").getId());
        form.organismGroup = "Enterobacterales";
        form.initialSignificance = "USUALLY";
        admin.saveOrganism(form.id, form, "1");
        form.organismGroup = "";
        form.initialSignificance = "";

        admin.saveOrganism(form.id, form, "1");

        assertNull(coded("kl-").getOrganismGroup());
        assertNull(coded("kl-").getInitialSignificance());
    }

    @Test
    public void aBlankNameOrWhonetCodeIsRefusedUnderTheLabelOnTheForm() {
        MicroOrganismAdminForm form = admin.getOrganism("lab-klebsiella");
        form.displayName = " ";
        assertEquals("Name is required", refusal(form));

        form.displayName = "Klebsiella pneumoniae";
        form.whonetCode = "";
        assertEquals("WHONET code is required", refusal(form));
    }

    @Test
    public void anUpperCaseWhonetCodeIsLoweredSoTheRefusalDoesNotAskForLowercase() {
        MicroOrganismAdminForm form = admin.getOrganism("lab-klebsiella");
        form.whonetCode = "a";
        assertEquals("Organism WHONET code must be 3-10 letters, digits, + or -", refusal(form));

        form.whonetCode = "KPN";
        admin.saveOrganism(form.id, form, "1");
        assertEquals("kpn", organisms.get("lab-klebsiella").orElseThrow().getWhonetCode());
    }

    @Test
    public void initialSignificanceIsOneOfTheFormsChoices() {
        MicroOrganismAdminForm form = admin.getOrganism("lab-klebsiella");
        form.initialSignificance = "BANANA";
        assertEquals("Initial significance must be USUALLY, POSSIBLE or RARE", refusal(form));

        form.initialSignificance = " rare ";
        admin.saveOrganism(form.id, form, "1");
        assertEquals("RARE", organisms.get("lab-klebsiella").orElseThrow().getInitialSignificance());
    }

    @Test
    public void aValueLongerThanItsColumnIsRefusedNamingTheField() {
        MicroOrganismAdminForm form = admin.getOrganism("lab-klebsiella");
        form.organismGroup = "x".repeat(101);
        assertEquals("Organism group is longer than 100 characters", refusal(form));
        form.organismGroup = null;
        form.shortName = "x".repeat(101);
        assertEquals("Short name is longer than 100 characters", refusal(form));
        form.shortName = null;
        form.oclCode = "x".repeat(101);
        assertEquals("OCL code is longer than 100 characters", refusal(form));
        form.oclCode = null;
        form.gramStain = "x".repeat(21);
        assertEquals("Gram stain is longer than 20 characters", refusal(form));
        form.gramStain = null;
        form.displayName = "x".repeat(256);
        assertEquals("Name is longer than 255 characters", refusal(form));
    }

    private String refusal(MicroOrganismAdminForm form) {
        return assertThrows(IllegalArgumentException.class, () -> admin.saveOrganism(form.id, form, "1")).getMessage();
    }

    @Test
    public void loadingTheSameFileAgainChangesNothing() throws Exception {
        load();
        assertEquals(2, handler.getLastSummary().getUpdated());
        assertEquals(7, handler.getLastSummary().getCreated());
        String klebsiella = coded("kl-").getId();
        int count = organisms.getAll().size();

        load();

        assertEquals(klebsiella, coded("kl-").getId());
        assertEquals(count, organisms.getAll().size());
        assertEquals(9, whonetTable.getAll().size());
        assertEquals(9, handler.getLastSummary().getCreated());
        assertEquals(0, handler.getLastSummary().getUpdated());
        assertEquals(0, handler.getLastSummary().getSkipped());
    }

    @Test
    public void aNameHeldByAnotherCodeFailsTheLoadAndLeavesTheTableAlone() throws Exception {
        load();
        String file = "WHONET_ORG_CODE\tREPLACED_BY\tORGANISM\tTAXONOMIC_STATUS\r\n" + "zzz\t\tKlebsiella sp.\tC\r\n";

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> handler.processConfiguration(new ByteArrayInputStream(file.getBytes(StandardCharsets.UTF_8)),
                        "Organisms.txt"));

        assertTrue(failure.getMessage().contains("kl-"));
        assertEquals(9, whonetTable.getAll().size());
        assertFalse(organisms.findByWhonetCodeIgnoreCase("zzz").isPresent());
    }

    @Test
    public void importCatalogDoesNotOfferTheTable() {
        CatalogImportServiceImpl catalogImport = new CatalogImportServiceImpl();
        context.getAutowireCapableBeanFactory().autowireBean(catalogImport);

        assertFalse(catalogImport.getImportableDomains().contains(handler.getDomainName()));
    }
}
