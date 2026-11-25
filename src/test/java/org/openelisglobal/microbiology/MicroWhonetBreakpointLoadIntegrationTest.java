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
import java.util.function.Predicate;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.configuration.service.CatalogImportServiceImpl;
import org.openelisglobal.microbiology.dao.MicroWhonetBreakpointDAO;
import org.openelisglobal.microbiology.service.MicroWhonetBreakpointConfigurationHandler;
import org.openelisglobal.microbiology.valueholder.MicroWhonetBreakpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/** Loads rows cut unchanged from WHONET's Breakpoints.txt, release v26.3.26. */
public class MicroWhonetBreakpointLoadIntegrationTest extends BaseWebContextSensitiveTest {
    private static final String FILE = "testdata/microbiology/whonet-breakpoints.txt";

    @Autowired
    private MicroWhonetBreakpointConfigurationHandler handler;
    @Autowired
    private MicroWhonetBreakpointDAO whonetTable;
    @Autowired
    private ApplicationContext context;

    private void load() throws Exception {
        try (InputStream file = getClass().getClassLoader().getResourceAsStream(FILE)) {
            handler.processConfiguration(file, "Breakpoints.txt");
        }
    }

    private MicroWhonetBreakpoint row(Predicate<MicroWhonetBreakpoint> match) {
        return whonetTable.getAll().stream().filter(match).findFirst().orElseThrow();
    }

    @Test
    public void theTableIsHeldWholeWithEveryColumnAsWritten() throws Exception {
        load();

        assertEquals(15, whonetTable.getAll().size());
        MicroWhonetBreakpoint family = row(r -> "CIP_NM".equals(r.getWhonetTest()));
        assertEquals("CLSI", family.getGuidelines());
        assertEquals("2026", family.getYear());
        assertEquals("MIC", family.getTestMethod());
        assertNull(family.getPotency());
        assertEquals("EBC", family.getOrganismCode());
        assertEquals("FAMILY_CODE", family.getOrganismCodeType());
        assertEquals("Human", family.getBreakpointType());
        assertEquals("Human", family.getHost());
        assertNull(family.getSiteOfInfection());
        assertEquals("Table 2A-1", family.getReferenceTable());
        assertNull(family.getReferenceSequence());
        assertEquals("CIP", family.getWhonetAbxCode());
        assertEquals("1", family.getR());
        assertEquals("0.5", family.getI());
        assertNull(family.getSdd());
        assertEquals("0.25", family.getS());
        assertNull(family.getEcvEcoff());
        assertNull(family.getEcvEcoffTentative());
        assertNull(family.getDateEntered());
        assertEquals("2024-05-22", family.getDateModified());
        assertNull(family.getComments());

        MicroWhonetBreakpoint dosed = row(r -> "CPT_ND30".equals(r.getWhonetTest()));
        assertEquals("30µg", dosed.getPotency());
        assertEquals("20-24", dosed.getSdd());
        assertEquals("17-19", row(r -> "CTX_ED5".equals(r.getWhonetTest())).getI());
        assertEquals("Non-meningitis", row(r -> "AMC_NM".equals(r.getWhonetTest())).getSiteOfInfection());
        assertEquals("Cattle", row(r -> "FLR_EM".equals(r.getWhonetTest())).getHost());
        assertEquals("ANAEROBE+SUBKINGDOM_CODE", row(r -> "PEN_EM".equals(r.getWhonetTest())).getOrganismCodeType());
        assertEquals("ALL", row(r -> "FLU_EM".equals(r.getWhonetTest())).getOrganismCodeType());
        MicroWhonetBreakpoint ecoff = row(r -> "AMB_EM".equals(r.getWhonetTest()));
        assertEquals("ECOFF", ecoff.getBreakpointType());
        assertEquals("1", ecoff.getEcvEcoff());
        assertNull(ecoff.getHost());
        assertEquals("X", row(r -> "18".equals(r.getEcvEcoff())).getEcvEcoffTentative());
        assertEquals("Excluded from Enterobacterales.", row(r -> "MFX_ED5".equals(r.getWhonetTest())).getComments());
        MicroWhonetBreakpoint blocked = row(r -> "CHL_ED30".equals(r.getWhonetTest()));
        assertNull(blocked.getS());
        assertNull(blocked.getR());
    }

    @Test
    public void rowsWithoutAnOrganismCodeTypeOrAntibioticAreKept() throws Exception {
        load();

        assertNull(row(r -> "AMC_EM".equals(r.getWhonetTest())).getOrganismCodeType());
        MicroWhonetBreakpoint tuberculosis = row(r -> "mtu".equals(r.getOrganismCode()));
        assertNull(tuberculosis.getWhonetAbxCode());
        assertNull(tuberculosis.getWhonetTest());
        assertEquals("M.tuberculosis", tuberculosis.getReferenceTable());
    }

    @Test
    public void loadingTheFileAgainReplacesTheTable() throws Exception {
        load();
        load();

        assertEquals(15, whonetTable.getAll().size());
        assertEquals(15, handler.getLastSummary().getCreated());
        assertEquals(0, handler.getLastSummary().getUpdated());
    }

    @Test
    public void aRowWithoutGuidelinesFailsTheLoadNamingItsLineAndLeavesTheTableAlone() throws Exception {
        load();
        String file = "GUIDELINES\tYEAR\tTEST_METHOD\tORGANISM_CODE\tBREAKPOINT_TYPE\r\n"
                + "CLSI\t2099\tMIC\tEBC\tHuman\r\n" + "\t2099\tMIC\tEBC\tHuman\r\n";

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> handler.processConfiguration(new ByteArrayInputStream(file.getBytes(StandardCharsets.UTF_8)),
                        "Breakpoints.txt"));

        assertTrue(failure.getMessage().contains("GUIDELINES"));
        assertTrue(failure.getMessage().contains("line 3"));
        List<MicroWhonetBreakpoint> rows = whonetTable.getAll();
        assertEquals(15, rows.size());
        assertFalse(rows.stream().anyMatch(r -> "2099".equals(r.getYear())));
    }

    @Test
    public void importCatalogDoesNotOfferTheTable() {
        CatalogImportServiceImpl catalogImport = new CatalogImportServiceImpl();
        context.getAutowireCapableBeanFactory().autowireBean(catalogImport);

        assertFalse(catalogImport.getImportableDomains().contains(handler.getDomainName()));
    }
}
