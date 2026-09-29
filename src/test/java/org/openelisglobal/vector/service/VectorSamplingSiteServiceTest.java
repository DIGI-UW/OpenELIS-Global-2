package org.openelisglobal.vector.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.security.SeededRoleAuthorities;
import org.openelisglobal.vector.valueholder.VectorSamplingSite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

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

    // ---- resolveOrCreateForOrder: the order-entry entry point ----

    private void authenticateAsReception() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("reception", "N/A", SeededRoleAuthorities.role("Reception")));
    }

    @Test
    public void resolveOrCreateForOrder_reception_createsTheSiteTheFormIntroduced() {
        authenticateAsReception();
        String id = vectorSamplingSiteService.resolveOrCreateForOrder(null, "Sungai Intake", "RBC-ORD-1",
                "WATER_SOURCE", "1");
        assertNotNull("a new site must yield an id", id);
        VectorSamplingSite created = vectorSamplingSiteService.getByCode("RBC-ORD-1");
        assertNotNull("the site must be findable by the code the form supplied", created);
        assertEquals(id, String.valueOf(created.getId()));
        assertEquals("Sungai Intake", created.getName());
        assertEquals("WATER_SOURCE", created.getType());
        assertTrue("an inline-created site is active", created.getActive());
        assertEquals("LOCAL", created.getSource());
    }

    @Test
    public void resolveOrCreateForOrder_knownCode_reusesInsteadOfDuplicating() {
        authenticateAsReception();
        String first = vectorSamplingSiteService.resolveOrCreateForOrder(null, "Pasar Well", "RBC-ORD-2", null, "1");
        String second = vectorSamplingSiteService.resolveOrCreateForOrder(null, "Pasar Well (again)", "RBC-ORD-2", null,
                "1");
        assertEquals("a second order naming the same code must reuse the site", first, second);
    }

    @Test
    public void resolveOrCreateForOrder_knownId_syncsNameCodeAndTypeFromTheForm() {
        VectorSamplingSite site = insertSite("RBC-ORD-3", "Old Name", true); // admin fixture
        authenticateAsReception();
        String id = vectorSamplingSiteService.resolveOrCreateForOrder(String.valueOf(site.getId()), "New Name",
                "RBC-ORD-3", "TREATMENT_PLANT", "1");
        assertEquals(String.valueOf(site.getId()), id);
        VectorSamplingSite reloaded = vectorSamplingSiteService.getByCode("RBC-ORD-3");
        assertEquals("New Name", reloaded.getName());
        assertEquals("TREATMENT_PLANT", reloaded.getType());
    }

    @Test
    public void resolveOrCreateForOrder_nothingToResolve_returnsWhatItWasGiven() {
        authenticateAsReception();
        assertNull(vectorSamplingSiteService.resolveOrCreateForOrder(null, null, null, null, "1"));
        assertNull("a name without a code is not enough to create a site",
                vectorSamplingSiteService.resolveOrCreateForOrder(null, "Name only", null, null, "1"));
    }

    /**
     * Inversion, both halves. The gate moved onto the order-entry method; the write
     * itself did not open. Reception still cannot insert a site directly...
     */
    @Test(expected = AccessDeniedException.class)
    public void insert_reception_isStillRefused() {
        authenticateAsReception();
        VectorSamplingSite site = new VectorSamplingSite();
        site.setCode("RBC-ORD-4");
        site.setName("Should not exist");
        site.setActive(true);
        site.setSource("LOCAL");
        site.setSysUserId("1");
        vectorSamplingSiteService.insert(site);
    }

    /** ...and the new method's own gate is real: neither privilege, no site. */
    @Test(expected = AccessDeniedException.class)
    public void resolveOrCreateForOrder_withoutEitherPrivilege_isRefused() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("nobody", "N/A", Collections.emptyList()));
        vectorSamplingSiteService.resolveOrCreateForOrder(null, "Nope", "RBC-ORD-5", null, "1");
    }
}
