package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.InputStream;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismMatch;
import org.openelisglobal.microbiology.service.MicroWhonetOrganismConfigurationHandler;
import org.openelisglobal.microbiology.service.MicrobiologyReferenceService;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.springframework.beans.factory.annotation.Autowired;

/** Searches organisms loaded from rows of WHONET's Organisms.txt. */
public class MicroOrganismSearchIntegrationTest extends BaseWebContextSensitiveTest {
    @Autowired
    private MicrobiologyReferenceService reference;
    @Autowired
    private MicroWhonetOrganismConfigurationHandler whonet;
    @Autowired
    private MicroOrganismDAO organisms;

    @Before
    public void whonetOrganisms() throws Exception {
        executeDataSetWithStateManagement("testdata/microbiology/whonet-organism-load.xml");
        try (InputStream file = getClass().getClassLoader()
                .getResourceAsStream("testdata/microbiology/whonet-organisms.txt")) {
            whonet.processConfiguration(file, "Organisms.txt");
        }
    }

    @Test
    public void partOfANameFindsOrganismsAndThoseWhonetMarksCommonComeFirst() {
        assertEquals(List.of("Klebsiella pneumoniae ss. pneumoniae", "Klebsiella sp."),
                names(reference.searchOrganisms("KLEBS", 10)));
        assertEquals(List.of("Escherichia coli", "Escherichia coli O157:H7", "Klebsiella pneumoniae ss. pneumoniae",
                "Klebsiella sp.", "Kluyveromyces marxianus"), names(reference.searchOrganisms("", 10)));
    }

    @Test
    public void aWhonetCodeFindsItsOrganismAndAnExactCodeLeads() {
        assertEquals("Klebsiella sp.", names(reference.searchOrganisms("kl-", 10)).get(0));
        assertEquals(List.of("Klebsiella pneumoniae ss. pneumoniae"), names(reference.searchOrganisms("KPN", 10)));
        assertEquals(List.of("Escherichia coli O157:H7"), names(reference.searchOrganisms("15", 10)));
    }

    @Test
    public void anOlderNameFindsTheOrganismAndSaysWhichNameMatched() {
        MicroOrganismMatch match = reference.searchOrganisms("fragilis", 10).get(0);
        assertEquals("Kluyveromyces marxianus", match.organism().getDisplayName());
        assertEquals("Kluyveromyces fragilis", match.formerName());
    }

    @Test
    public void aReplacedCodeOrItsNameFindsTheOrganismThatReplacedIt() {
        for (String typed : List.of("cps", "kefyr")) {
            MicroOrganismMatch match = reference.searchOrganisms(typed, 10).get(0);
            assertEquals("Kluyveromyces marxianus", match.organism().getDisplayName());
            assertEquals("Candida kefyr", match.formerName());
        }
    }

    @Test
    public void anOrganismFoundByItsOwnNameOrCodeHasNoFormerName() {
        assertNull(reference.searchOrganisms("marxianus", 10).get(0).formerName());
        assertNull(reference.searchOrganisms("kma", 10).get(0).formerName());
        assertNull(reference.searchOrganisms("Klebsiella pneumoniae", 10).get(0).formerName());
    }

    @Test
    public void aLaboratorysOwnOrganismIsFoundAndAnInactiveOneIsNot() {
        organism("Zzlab local isolate", "Y");
        organism("Zzlab retired isolate", "N");
        assertEquals(List.of("Zzlab local isolate"), names(reference.searchOrganisms("zzlab", 10)));
    }

    @Test
    public void searchTreatsWildcardCharactersAsText() {
        assertEquals(List.of(), names(reference.searchOrganisms("%", 10)));
        assertEquals(List.of(), names(reference.searchOrganisms("k_e", 10)));
    }

    @Test
    public void searchReturnsNoMoreThanItsLimit() {
        assertEquals(List.of("Escherichia coli", "Escherichia coli O157:H7"), names(reference.searchOrganisms("", 2)));
    }

    private static List<String> names(List<MicroOrganismMatch> found) {
        return found.stream().map(match -> match.organism().getDisplayName()).toList();
    }

    private void organism(String name, String active) {
        var organism = new MicroOrganism();
        organism.setDisplayName(name);
        organism.setIsActive(active);
        organism.setSysUserId("1");
        organisms.insert(organism);
    }
}
