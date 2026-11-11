package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.service.MicrobiologyReferenceService;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class MicroOrganismSearchIntegrationTest extends BaseWebContextSensitiveTest {
    @Autowired
    private MicrobiologyReferenceService reference;
    @Autowired
    private MicroOrganismDAO organisms;

    @Before
    public void organisms() {
        organism("Zzsearch rare species", null, null, "2", "Y");
        organism("Zzsearch common species", null, null, "1", "Y");
        organism("Zzsearch unranked species", null, null, null, "Y");
        organism("Zzsearch retired species", null, null, "1", "N");
        organism("Zzsearch", "zzq", "Zzgenus sp.", "1.5", "Y");
    }

    @Test
    public void commonOrganismsComeFirstAndInactiveOnesAreLeftOut() {
        assertEquals(
                List.of("Zzsearch common species", "Zzsearch", "Zzsearch rare species", "Zzsearch unranked species"),
                names(reference.searchOrganisms("zzSEARCH", 10)));
    }

    @Test
    public void whonetCodeOrNameFindsTheOrganismAndAnExactCodeLeads() {
        organism("Zzq lookalike", null, null, "1", "Y");
        assertEquals(List.of("Zzsearch", "Zzq lookalike"), names(reference.searchOrganisms("zzq", 10)));
        assertEquals(List.of("Zzsearch"), names(reference.searchOrganisms("zzgenus", 10)));
    }

    @Test
    public void amongEquallyCommonOrganismsThoseWithAWhonetCodeComeFirst() {
        organism("Zzcoded alpha", null, null, "1", "Y");
        organism("Zzcoded omega", "zzo", "Zzcoded omega", "1", "Y");
        assertEquals(List.of("Zzcoded omega", "Zzcoded alpha"), names(reference.searchOrganisms("zzcoded", 10)));
    }

    @Test
    public void searchReturnsNoMoreThanItsLimit() {
        assertEquals(List.of("Zzsearch common species", "Zzsearch"), names(reference.searchOrganisms("zzsearch", 2)));
    }

    private static List<String> names(List<MicroOrganism> found) {
        return found.stream().map(MicroOrganism::getDisplayName).toList();
    }

    private void organism(String name, String whonetCode, String whonetName, String prevalence, String active) {
        var organism = new MicroOrganism();
        organism.setDisplayName(name);
        organism.setWhonetCode(whonetCode);
        organism.setWhonetName(whonetName);
        organism.setPrevalence(prevalence == null ? null : new BigDecimal(prevalence));
        organism.setIsActive(active);
        organism.setSysUserId("1");
        organisms.insert(organism);
    }
}
