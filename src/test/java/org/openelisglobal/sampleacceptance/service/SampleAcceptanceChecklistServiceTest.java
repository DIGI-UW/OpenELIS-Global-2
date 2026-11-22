package org.openelisglobal.sampleacceptance.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.dictionarycategory.service.DictionaryCategoryService;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;

/**
 * Integration tests for the S-09 (OGC-580) Sample Acceptance Checklist config
 * read service. State is driven through the real reuse path
 * ({@link DictionaryService} for items, {@code ConfigurationProperties} for
 * enforcement) and asserted via {@link SampleAcceptanceChecklistService}, so
 * the tests verify the feature's behaviour rather than re-testing Dictionary
 * internals.
 */
@Rollback
public class SampleAcceptanceChecklistServiceTest extends BaseWebContextSensitiveTest {

    private static final String CLINICAL_CATEGORY = SampleAcceptanceChecklistService.DOMAIN_CATEGORY_PREFIX
            + "CLINICAL";

    private static final List<String> DEFAULT_KEYS = List.of("containerIntact", "labelLegible", "volumeAdequate",
            "coldChain", "transit", "paperwork");

    @Autowired
    private SampleAcceptanceChecklistService service;

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private DictionaryCategoryService dictionaryCategoryService;

    @Autowired
    private SiteInformationService siteInformationService;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/sample-acceptance-checklist.xml");
        // The 3 per-domain enforcement settings are migration-seeded into
        // site_information, not by the fixture above. A sibling test whose fixture
        // names site_information (e.g. BarcodeConfigServiceTest) runs a committed
        // TRUNCATE ... CASCADE that permanently wipes them for the rest of the JVM
        // run, so setEnforcement() would hit its "setting not found" guard. Re-seed
        // them (default OPTIONAL) so the read/write path has rows to work with.
        ensureSiteInformation("sampleAcceptCheck.clinical", "OPTIONAL");
        ensureSiteInformation("sampleAcceptCheck.environmental", "OPTIONAL");
        ensureSiteInformation("sampleAcceptCheck.vector", "OPTIONAL");
    }

    // ---- seed correctness + sorting
    // -------------------------------------------------

    @Test
    public void listLabWide_returnsTheSixSeededDefaultsInSortOrder() {
        List<Dictionary> items = service.listLabWide();

        assertEquals(6, items.size());
        // Fixture ids are reversed vs sort_order, so this also proves
        // sort-by-sortOrder.
        assertEquals(DEFAULT_KEYS, keys(items));
        items.forEach(i -> assertEquals("Y", i.getIsActive()));
    }

    // ---- create / edit / reorder / deactivate (via DictionaryService)
    // ---------------

    @Test
    public void create_newLabWideItemAppearsAtItsSortedPosition() {
        addItem(SampleAcceptanceChecklistService.LAB_WIDE_CATEGORY, "extraCheck", "Extra check", 7);

        List<Dictionary> items = service.listLabWide();
        assertEquals(7, items.size());
        assertEquals("extraCheck", items.get(6).getDictEntry());
    }

    @Test
    public void edit_labelChangeIsReflectedInTheRead() {
        Dictionary target = findLabWide("coldChain");
        Dictionary managed = dictionaryService.get(target.getId());
        managed.setLocalAbbreviation("Cold chain intact and within range");
        managed.setSysUserId("1");
        dictionaryService.update(managed);

        assertEquals("Cold chain intact and within range", findLabWide("coldChain").getLocalAbbreviation());
    }

    @Test
    public void reorder_sortOrderChangeMovesItemToTheFront() {
        Dictionary paperwork = dictionaryService.get(findLabWide("paperwork").getId());
        paperwork.setSortOrder(0);
        paperwork.setSysUserId("1");
        dictionaryService.update(paperwork);

        assertEquals("paperwork", service.listLabWide().get(0).getDictEntry());
    }

    @Test
    public void deactivate_excludesItemAndReactivateRestoresIt() {
        String id = findLabWide("volumeAdequate").getId();

        Dictionary toDeactivate = dictionaryService.get(id);
        toDeactivate.setIsActive("N");
        toDeactivate.setSysUserId("1");
        dictionaryService.update(toDeactivate);

        List<String> afterDeactivate = keys(service.listLabWide());
        assertEquals(5, afterDeactivate.size());
        assertFalse(afterDeactivate.contains("volumeAdequate"));

        Dictionary toReactivate = dictionaryService.get(id);
        toReactivate.setIsActive("Y");
        toReactivate.setSysUserId("1");
        dictionaryService.update(toReactivate);

        assertTrue(keys(service.listLabWide()).contains("volumeAdequate"));
    }

    // ---- domain precedence + lab-wide fallback
    // --------------------------------------

    @Test
    public void listForDomain_usesDomainListWhenItHasActiveItems_neverMerged() {
        addItem(CLINICAL_CATEGORY, "clinicalOnly", "Clinical-only check", 1);

        List<Dictionary> clinical = service.listForDomain("CLINICAL");
        assertEquals(List.of("clinicalOnly"), keys(clinical));
        // Resolved domain list is the Clinical list alone, not merged with lab-wide.
        assertNotEquals(keys(service.listLabWide()), keys(clinical));
    }

    @Test
    public void listForDomain_fallsBackToLabWideWhenDomainHasNoActiveItems() {
        // Domains with no items of their own fall back to the lab-wide list.
        assertEquals(DEFAULT_KEYS, keys(service.listForDomain("ENVIRONMENTAL")));
        assertEquals(DEFAULT_KEYS, keys(service.listForDomain("VECTOR")));
        assertEquals(DEFAULT_KEYS, keys(service.listForDomain(null)));

        // Add a Clinical item, then deactivate it -> Clinical falls back to lab-wide.
        String id = addItem(CLINICAL_CATEGORY, "clinicalOnly", "Clinical-only check", 1);
        assertEquals(1, service.listForDomain("CLINICAL").size());

        Dictionary clinicalItem = dictionaryService.get(id);
        clinicalItem.setIsActive("N");
        clinicalItem.setSysUserId("1");
        dictionaryService.update(clinicalItem);

        assertEquals(DEFAULT_KEYS, keys(service.listForDomain("CLINICAL")));
    }

    @Test
    public void listForDomain_isCaseInsensitive() {
        addItem(CLINICAL_CATEGORY, "clinicalOnly", "Clinical-only check", 1);
        assertEquals(keys(service.listForDomain("CLINICAL")), keys(service.listForDomain("clinical")));
    }

    // ---- enforcement
    // ----------------------------------------------------------------

    @Test
    public void getEnforcement_defaultsToOptionalPerDomainAndForUnknownInputs() {
        assertEquals("OPTIONAL", service.getEnforcement("CLINICAL"));
        assertEquals("OPTIONAL", service.getEnforcement("ENVIRONMENTAL"));
        assertEquals("OPTIONAL", service.getEnforcement("VECTOR"));
        assertEquals("OPTIONAL", service.getEnforcement("clinical")); // case-insensitive
        assertEquals("OPTIONAL", service.getEnforcement(null)); // unknown -> default
        assertEquals("OPTIONAL", service.getEnforcement("BOGUS")); // unknown -> default
    }

    // ---- isolation from the QA checklist (false-positive guard)
    // ----------------------

    @Test
    public void sampleAcceptanceAndQaChecklistsNeverShareItems() {
        List<String> acceptanceKeys = keys(service.listLabWide());
        // Read the QA checklist category the same way SampleQaChecklistService does.
        List<String> qaKeys = dictionaryService.getActiveSortedEntriesByCategoryName("QAChecklistItem").stream()
                .map(Dictionary::getDictEntry).collect(Collectors.toList());

        // The QA item lives in the QA category only.
        assertTrue(qaKeys.contains("patientInfoVerified"));
        assertFalse(acceptanceKeys.contains("patientInfoVerified"));
        // None of the acceptance items leak into the QA list.
        acceptanceKeys.forEach(k -> assertFalse(qaKeys.contains(k)));
    }

    // ---- admin: getAdminView
    // ---------------------------------------------------------

    @Test
    public void getAdminView_allDomains_returnsLabWideItemsInclInactive_noEnforcement() {
        // Deactivate one lab-wide item: the admin view must still surface it (so the
        // "Active" toggle has something to toggle), unlike the active-only read.
        Dictionary volume = dictionaryService.get(findLabWide("volumeAdequate").getId());
        volume.setIsActive("N");
        volume.setSysUserId("1");
        dictionaryService.update(volume);

        AdminChecklistView view = service.getAdminView("ALL");

        assertEquals("ALL", view.getDomain());
        assertNull(view.getEnforcement());
        assertFalse(view.isDomainOverrides());
        assertTrue(view.getLabWideItems().isEmpty());
        // All six (incl. the deactivated one) in sort order.
        assertEquals(DEFAULT_KEYS, keys(view.getOwnItems()));
        Dictionary deactivated = view.getOwnItems().stream().filter(d -> "volumeAdequate".equals(d.getDictEntry()))
                .findFirst().orElseThrow(() -> new AssertionError("inactive item missing from admin view"));
        assertEquals("N", deactivated.getIsActive());
    }

    @Test
    public void getAdminView_domainWithNoOwnItems_fallsBackAndDoesNotOverride() {
        AdminChecklistView view = service.getAdminView("ENVIRONMENTAL");

        assertEquals("ENVIRONMENTAL", view.getDomain());
        assertEquals("OPTIONAL", view.getEnforcement());
        assertFalse(view.isDomainOverrides());
        assertTrue(view.getOwnItems().isEmpty());
        // Lab-wide list is surfaced as the active fallback.
        assertEquals(DEFAULT_KEYS, keys(view.getLabWideItems()));
    }

    @Test
    public void getAdminView_domainWithOwnActiveItem_overridesAndStillShowsLabWide() {
        Dictionary created = service.createItem("CLINICAL", "Patient identifiers match request", "1");

        AdminChecklistView view = service.getAdminView("CLINICAL");

        assertTrue(view.isDomainOverrides());
        assertEquals(1, view.getOwnItems().size());
        assertEquals(created.getId(), view.getOwnItems().get(0).getId());
        assertEquals("Patient identifiers match request", view.getOwnItems().get(0).getLocalizedName());
        // Lab-wide items are still returned (the screen renders them "superseded").
        assertEquals(DEFAULT_KEYS, keys(view.getLabWideItems()));
    }

    // ---- admin: create / update / reorder
    // --------------------------------------------

    @Test
    public void createItem_appendsActiveItemWithNextSortOrderAndLabelAsLocalizedName() {
        Dictionary created = service.createItem("ALL", "Sealed and leak-free", "1");

        assertEquals("Y", created.getIsActive());
        assertEquals(Integer.valueOf(7), created.getSortOrder());
        assertEquals("Sealed and leak-free", created.getLocalizedName());

        List<Dictionary> items = service.listLabWide();
        assertEquals(7, items.size());
        Dictionary last = items.get(6);
        assertEquals(created.getId(), last.getId());
        assertEquals("Sealed and leak-free", last.getLocalizedName());
        // The label is stored in full in local_abbrev too (no silent truncation):
        // local_abbrev == localized name, never divergent.
        assertEquals("Sealed and leak-free", last.getLocalAbbreviation());
    }

    @Test
    public void createItem_rejectsLabelLongerThanColumnWidth_butAcceptsExactly60() {
        String tooLong = "x".repeat(61);
        assertThrows(IllegalArgumentException.class, () -> service.createItem("ALL", tooLong, "1"));

        String exactly60 = "y".repeat(60);
        Dictionary created = service.createItem("ALL", exactly60, "1");
        // Stored in full, identically, in both the label column and the localized name.
        assertEquals(exactly60, created.getLocalAbbreviation());
        assertEquals(exactly60, created.getLocalizedName());
        assertEquals(60, created.getLocalAbbreviation().length());
    }

    @Test
    public void getAdminView_rejectsUnknownDomain() {
        assertThrows(IllegalArgumentException.class, () -> service.getAdminView("BOGUS"));
    }

    @Test
    public void createItem_rejectsUnknownDomain() {
        assertThrows(IllegalArgumentException.class, () -> service.createItem("BOGUS", "Some label", "1"));
    }

    @Test
    public void updateItem_changesLabelAndActiveAndIsReflectedInReads() {
        String id = findLabWide("coldChain").getId();

        service.updateItem(id, "Cold chain intact and within range", true, "1");
        Dictionary reloaded = dictionaryService.get(id);
        assertEquals("Cold chain intact and within range", reloaded.getLocalizedName());
        assertEquals("Y", reloaded.getIsActive());

        // Deactivating drops it from the active resolved list (dictEntry stays stable).
        service.updateItem(id, "Cold chain intact and within range", false, "1");
        assertFalse(keys(service.listLabWide()).contains("coldChain"));
    }

    @Test
    public void reorder_persistsRequestedOrderAsSortOrderOneToN() {
        List<String> ids = service.listLabWide().stream().map(Dictionary::getId).collect(Collectors.toList());
        List<String> reversedIds = new ArrayList<>(ids);
        Collections.reverse(reversedIds);

        service.reorder("ALL", reversedIds, "1");

        List<Dictionary> reordered = service.listLabWide();
        List<String> expectedKeys = new ArrayList<>(DEFAULT_KEYS);
        Collections.reverse(expectedKeys);
        assertEquals(expectedKeys, keys(reordered));
        for (int i = 0; i < reordered.size(); i++) {
            assertEquals(Integer.valueOf(i + 1), reordered.get(i).getSortOrder());
        }
    }

    // ---- admin: enforcement
    // ----------------------------------------------------------

    @Test
    public void setEnforcement_persistsModeAndIsReflectedByGetEnforcement() {
        try {
            service.setEnforcement("CLINICAL", "MANDATORY", "1");

            assertEquals("MANDATORY", service.getEnforcement("CLINICAL"));
            // Other domains keep the seeded default.
            assertEquals("OPTIONAL", service.getEnforcement("ENVIRONMENTAL"));
            // Persisted to the backing site_information row.
            assertEquals("MANDATORY",
                    siteInformationService.getSiteInformationByName("sampleAcceptCheck.clinical").getValue());
        } finally {
            // setEnforcement refreshes the process-wide ConfigurationProperties
            // singleton, which @Rollback does not undo; restore it in-transaction so
            // the value never leaks into another test.
            service.setEnforcement("CLINICAL", "OPTIONAL", "1");
        }
    }

    @Test
    public void setEnforcement_rejectsInvalidModeAndNonEnforceableDomain_leavingStateUnchanged() {
        assertThrows(IllegalArgumentException.class, () -> service.setEnforcement("CLINICAL", "SOMETIMES", "1"));
        assertThrows(IllegalArgumentException.class, () -> service.setEnforcement("ALL", "MANDATORY", "1"));
        // The rejected calls validate before persisting, so nothing changed.
        assertEquals("OPTIONAL", service.getEnforcement("CLINICAL"));
    }

    // ---- helpers
    // --------------------------------------------------------------------

    private List<String> keys(List<Dictionary> items) {
        return items.stream().map(Dictionary::getDictEntry).collect(Collectors.toList());
    }

    private Dictionary findLabWide(String dictEntry) {
        return service.listLabWide().stream().filter(d -> dictEntry.equals(d.getDictEntry())).findFirst()
                .orElseThrow(() -> new AssertionError("Expected lab-wide item not found: " + dictEntry));
    }

    private String addItem(String categoryName, String dictEntry, String label, int sortOrder) {
        Dictionary dictionary = new Dictionary();
        dictionary.setDictionaryCategory(dictionaryCategoryService.getDictionaryCategoryByName(categoryName));
        dictionary.setDictEntry(dictEntry);
        dictionary.setLocalAbbreviation(label);
        dictionary.setIsActive("Y");
        dictionary.setSortOrder(sortOrder);
        dictionary.setSysUserId("1");
        return dictionaryService.insert(dictionary);
    }
}
