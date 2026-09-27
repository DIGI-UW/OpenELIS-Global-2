package org.openelisglobal.vector.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.vector.valueholder.VectorSamplingSite;
import org.springframework.beans.factory.annotation.Autowired;

/** Tests for {@link VectorSamplingSiteService#search} and {@code getByCode}. */
public class VectorSamplingSiteServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private VectorSamplingSiteService vectorSamplingSiteService;

    private VectorSamplingSite insertSite(String code, String name, boolean active) {
        VectorSamplingSite site = new VectorSamplingSite();
        site.setCode(code);
        site.setName(name);
        site.setActive(active);
        site.setSource("LOCAL");
        site.setSysUserId("1");
        Integer id = vectorSamplingSiteService.insert(site);
        site.setId(id);
        return site;
    }

    @Test
    public void search_matchesByNameOrCode_caseInsensitive_activeOnly() {
        insertSite("WS-100", "Riverside Well", true);
        insertSite("WS-101", "Lakeside Well", true);
        insertSite("WS-102", "Riverside Trap", false);

        List<VectorSamplingSite> byName = vectorSamplingSiteService.search("riverside");
        assertEquals("search must match by name case-insensitively and exclude inactive sites", 1, byName.size());
        assertEquals("WS-100", byName.get(0).getCode());

        List<VectorSamplingSite> byCode = vectorSamplingSiteService.search("ws-101");
        assertEquals("search must match by code case-insensitively", 1, byCode.size());
        assertEquals("Lakeside Well", byCode.get(0).getName());
    }

    @Test
    public void search_noMatch_returnsEmptyList() {
        insertSite("WS-200", "Only Site", true);

        List<VectorSamplingSite> results = vectorSamplingSiteService.search("nonexistent-term");

        assertTrue("search should return an empty list, not null, when nothing matches", results.isEmpty());
    }

    @Test
    public void getByCode_existingCode_returnsSite() {
        insertSite("WS-300", "Dedup Target", true);

        VectorSamplingSite found = vectorSamplingSiteService.getByCode("WS-300");

        assertEquals("Dedup Target", found.getName());
    }

    @Test
    public void getByCode_unknownCode_returnsNull() {
        VectorSamplingSite found = vectorSamplingSiteService.getByCode("NO-SUCH-CODE");

        assertNull("getByCode must return null (not throw) for an unknown code, "
                + "since callers use it to decide whether to create a new site", found);
    }
}
