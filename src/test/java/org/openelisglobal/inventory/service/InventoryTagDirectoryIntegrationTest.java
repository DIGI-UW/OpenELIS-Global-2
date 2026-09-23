package org.openelisglobal.inventory.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.inventory.service.InventoryTagService.TagSummary;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The tag directory is governance over free-form strings, not a managed entity
 * that owns them.
 *
 * <p>
 * The cases that matter are the ones where the two halves have to agree: a tag
 * exists because an item carries it, or because someone declared it, and the
 * directory has to show both without showing either twice. And deactivating has
 * to stop a tag being offered without touching a single item that carries it —
 * if it did touch them, it would be a delete wearing a different name.
 */
public class InventoryTagDirectoryIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private InventoryItemService inventoryItemService;

    @Autowired
    private InventoryTagService inventoryTagService;

    private static final String SYS_USER_ID = "1";

    private String unique(String prefix) {
        return prefix + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    private Long itemWithTags(String name, String... tags) {
        InventoryItem item = new InventoryItem();
        item.setFhirUuid(UUID.randomUUID());
        item.setName(name + " " + UUID.randomUUID());
        item.setUnits("tests");
        item.setIsActive("Y");
        item.setSysUserId(SYS_USER_ID);
        item.setTags(new LinkedHashSet<>(List.of(tags)));
        return inventoryItemService.insert(item);
    }

    private TagSummary find(String name) {
        return inventoryTagService.getDirectory().stream().filter(tag -> tag.getName().equals(name)).findFirst()
                .orElse(null);
    }

    @Test
    public void aTagInUseAppearsInTheDirectoryWithoutAnyoneDeclaringIt() {
        String tag = unique("Fridge");
        itemWithTags("Directory item", tag);

        TagSummary entry = find(tag);

        assertNotNull("a tag an item carries is a tag the directory knows", entry);
        assertEquals(1, entry.getItemCount());
        assertTrue(entry.isActive());
    }

    @Test
    public void theCountIsTheNumberOfItemsCarryingTheTag() {
        String tag = unique("Shared");
        itemWithTags("First", tag);
        itemWithTags("Second", tag);
        itemWithTags("Third", tag);

        assertEquals(3, find(tag).getItemCount());
    }

    @Test
    public void aTagCreatedAheadOfUseAppearsWithNoItems() {
        String tag = unique("Declared");

        inventoryTagService.createTag(tag, SYS_USER_ID);

        TagSummary entry = find(tag);
        assertNotNull("a declared tag is offered before anything carries it", entry);
        assertEquals(0, entry.getItemCount());
        assertTrue(entry.isActive());
    }

    /**
     * The directory is a union of two sources. A tag that is both in use and
     * declared is still one tag.
     */
    @Test
    public void aTagThatIsBothDeclaredAndInUseAppearsOnce() {
        String tag = unique("Both");
        inventoryTagService.createTag(tag, SYS_USER_ID);
        itemWithTags("Carrying item", tag);

        long appearances = inventoryTagService.getDirectory().stream()
                .filter(entry -> entry.getName().equalsIgnoreCase(tag)).count();

        assertEquals(1, appearances);
    }

    @Test
    public void deactivatingRemovesATagFromTheSuggestionsButNotFromItsItems() {
        String tag = unique("Retired");
        Long itemId = itemWithTags("Tagged item", tag);
        assertTrue(inventoryTagService.getActiveTagNames().contains(tag));

        inventoryTagService.setActive(tag, false, SYS_USER_ID);

        assertFalse("a retired tag is no longer offered", inventoryTagService.getActiveTagNames().contains(tag));
        assertEquals("but the item carrying it is untouched", Set.of(tag), inventoryItemService.get(itemId).getTags());
        assertFalse("and it is still listed, marked inactive", find(tag).isActive());
        assertEquals("with its count intact", 1, find(tag).getItemCount());
    }

    @Test
    public void reactivatingPutsATagBackInTheSuggestions() {
        String tag = unique("Returning");
        itemWithTags("Tagged item", tag);
        inventoryTagService.setActive(tag, false, SYS_USER_ID);

        inventoryTagService.setActive(tag, true, SYS_USER_ID);

        assertTrue(inventoryTagService.getActiveTagNames().contains(tag));
        assertTrue(find(tag).isActive());
    }

    /** Creating a tag that was retired is how a lab changes its mind. */
    @Test
    public void creatingADeactivatedTagBringsItBack() {
        String tag = unique("Rethought");
        inventoryTagService.createTag(tag, SYS_USER_ID);
        inventoryTagService.setActive(tag, false, SYS_USER_ID);
        assertFalse(find(tag).isActive());

        inventoryTagService.createTag(tag, SYS_USER_ID);

        assertTrue(find(tag).isActive());
    }

    @Test
    public void aTagDeclaredTwiceIsStillOneTag() {
        String tag = unique("Twice");

        inventoryTagService.createTag(tag, SYS_USER_ID);
        inventoryTagService.createTag(tag, SYS_USER_ID);

        assertEquals(1, inventoryTagService.getDirectory().stream()
                .filter(entry -> entry.getName().equalsIgnoreCase(tag)).count());
    }

    /**
     * Declaring a tag that differs only in case must not split the directory in
     * two, for the same reason the write path folds spellings on the item.
     */
    @Test
    public void declaringADifferentSpellingDoesNotSplitTheDirectory() {
        String tag = unique("Casing");
        itemWithTags("Tagged item", tag);

        inventoryTagService.createTag(tag.toUpperCase(), SYS_USER_ID);

        assertEquals(1, inventoryTagService.getDirectory().stream()
                .filter(entry -> entry.getName().equalsIgnoreCase(tag)).count());
    }

    @Test
    public void deactivatingMatchesTheTagWhateverCaseItIsGivenIn() {
        String tag = unique("Folded");
        itemWithTags("Tagged item", tag);

        inventoryTagService.setActive(tag.toUpperCase(), false, SYS_USER_ID);

        assertFalse(inventoryTagService.getActiveTagNames().contains(tag));
    }

    @Test
    public void aDeactivatedTagCanStillBeAppliedToAnItemThatAlreadyUsesIt() {
        String tag = unique("Legacy");
        itemWithTags("Original item", tag);
        inventoryTagService.setActive(tag, false, SYS_USER_ID);

        // Nothing at the data layer forbids it: deactivation governs what is
        // suggested, and an item edited for another reason must not silently lose
        // a tag the lab chose to retire.
        Long second = itemWithTags("Later item", tag);

        assertEquals(Set.of(tag), inventoryItemService.get(second).getTags());
    }

    @Test
    public void theDirectoryIsSortedSoDuplicatesSitTogether() {
        itemWithTags("Sorted item", "Zebra tag", "Alpha tag");

        List<String> names = inventoryTagService.getDirectory().stream().map(TagSummary::getName).toList();
        List<String> sorted = names.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();

        assertEquals(sorted, names);
    }
}
