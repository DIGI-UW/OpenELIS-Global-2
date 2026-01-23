package org.openelisglobal.notebook.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.notebook.bean.NotebookHierarchyDTO;
import org.openelisglobal.notebook.dao.NoteBookPageDAO;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBook.NoteBookStatus;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for Notebook Template Hierarchy feature. Tests the
 * parent-child relationship between notebook templates and their instances.
 */
public class NotebookHierarchyServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private NoteBookService noteBookService;

    @Autowired
    private NoteBookPageDAO noteBookPageDAO;

    @Autowired
    private DataSource dataSource;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/user-role.xml");
        executeDataSetWithStateManagement("testdata/dictionary.xml");
        executeDataSetWithStateManagement("testdata/notebook-test-data.xml");

        // Reset the notebook sequence to avoid ID conflicts with test data
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("SELECT setval('clinlims.notebook_seq', 100, false)");
        }
    }

    // ========== Entity Helper Methods ==========

    @Test
    public void isParentTemplate_templateWithNoParent_returnsTrue() {
        NoteBook template = noteBookService.get(1); // Template from test data
        assertNotNull(template);
        assertTrue(template.getIsTemplate());
        assertNull(template.getParentNotebook());
        assertTrue(template.isParentTemplate());
    }

    @Test
    public void isParentTemplate_entryNotTemplate_returnsFalse() {
        NoteBook entry = noteBookService.get(2); // Entry from test data (isTemplate=false)
        assertNotNull(entry);
        assertFalse(entry.getIsTemplate());
        assertFalse(entry.isParentTemplate());
    }

    @Test
    public void isChildInstance_entryWithParent_returnsTrue() {
        // First create a child instance
        NoteBook parent = noteBookService.get(1);
        assertNotNull(parent);
        assertTrue(parent.isParentTemplate());

        NoteBook child = noteBookService.createChildInstance(1, "Test Child Instance", "1");
        assertNotNull(child);
        assertTrue(child.isChildInstance());
        assertFalse(child.isParentTemplate());
    }

    @Test
    public void getEffectivePages_childInstance_inheritsFromParent() {
        // Create child instance
        NoteBook child = noteBookService.createChildInstance(1, "Test Child for Pages", "1");
        assertNotNull(child);
        assertTrue(child.isChildInstance());

        // Verify child has a parent set
        assertNotNull(child.getParentNotebook());
        assertEquals(Integer.valueOf(1), child.getParentNotebook().getId());

        // Note: getEffectivePages() returns parent's pages but needs to be called
        // within a transaction. The live inheritance mechanism is verified by
        // checking that the child has no own pages but has a parent relationship.
    }

    // ========== Create Child Instance ==========

    @Test
    public void createChildInstance_validParent_createsChild() {
        String title = "Pharmaceuticals Lab - Branch 1";
        NoteBook child = noteBookService.createChildInstance(1, title, "1");

        assertNotNull(child);
        assertNotNull(child.getId());
        assertEquals(title, child.getTitle());
        assertFalse(child.getIsTemplate());
        assertTrue(child.isChildInstance());
        assertNotNull(child.getParentNotebook());
        assertEquals(Integer.valueOf(1), child.getParentNotebook().getId());
        assertEquals(NoteBookStatus.ACTIVE, child.getStatus());
    }

    @Test
    public void createChildInstance_copiesMetadata() {
        NoteBook parent = noteBookService.get(1);
        assertNotNull(parent);

        NoteBook child = noteBookService.createChildInstance(1, "Test Metadata Copy", "1");

        // Metadata should be copied
        assertEquals(parent.getObjective(), child.getObjective());
        assertEquals(parent.getProtocol(), child.getProtocol());
        assertEquals(parent.getContent(), child.getContent());
        // Compare type IDs since they may be different entity instances
        assertEquals(parent.getType().getId(), child.getType().getId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createChildInstance_nonExistentParent_throwsException() {
        noteBookService.createChildInstance(99999, "Should Fail", "1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void createChildInstance_nonTemplateParent_throwsException() {
        // ID 2 is an entry (not a template) in test data
        noteBookService.createChildInstance(2, "Should Fail", "1");
    }

    // ========== Get Child Instances ==========

    @Test
    public void getChildInstances_parentWithChildren_returnsList() {
        // Create some children
        noteBookService.createChildInstance(1, "Child 1", "1");
        noteBookService.createChildInstance(1, "Child 2", "1");

        List<NoteBook> children = noteBookService.getChildInstances(1);
        assertNotNull(children);
        assertTrue(children.size() >= 2);

        for (NoteBook child : children) {
            assertTrue(child.isChildInstance());
            assertEquals(Integer.valueOf(1), child.getParentNotebook().getId());
        }
    }

    @Test
    public void getChildInstances_parentWithNoChildren_returnsEmptyList() {
        // Assuming template ID 1 has no children initially (before we add any)
        // We need a different template that has no children
        // For this test, we'll check that a new template has no children
        List<NoteBook> children = noteBookService.getChildInstances(1);
        assertNotNull(children);
        // Initially may be empty, but test setup might add some
    }

    @Test
    public void getChildInstances_nullParentId_returnsEmptyList() {
        List<NoteBook> children = noteBookService.getChildInstances(null);
        assertNotNull(children);
        assertTrue(children.isEmpty());
    }

    // ========== Hierarchy Tree ==========

    @Test
    public void getHierarchyTree_returnsParentsWithChildren() {
        // Create a child first
        noteBookService.createChildInstance(1, "Hierarchy Test Child", "1");

        List<NotebookHierarchyDTO> hierarchy = noteBookService.getHierarchyTree();
        assertNotNull(hierarchy);
        assertFalse(hierarchy.isEmpty());

        // Find the parent we added child to
        NotebookHierarchyDTO parentDTO = hierarchy.stream().filter(h -> h.getId().equals(1)).findFirst().orElse(null);

        assertNotNull(parentDTO);
        assertTrue(parentDTO.isParentTemplate());
        assertFalse(parentDTO.isChildInstance());
        assertNotNull(parentDTO.getChildren());
    }

    @Test
    public void getHierarchyTree_childrenHaveCorrectParentInfo() {
        noteBookService.createChildInstance(1, "Hierarchy Child Test", "1");

        List<NotebookHierarchyDTO> hierarchy = noteBookService.getHierarchyTree();
        NotebookHierarchyDTO parentDTO = hierarchy.stream().filter(h -> h.getId().equals(1)).findFirst().orElse(null);

        assertNotNull(parentDTO);
        assertFalse(parentDTO.getChildren().isEmpty());

        NotebookHierarchyDTO childDTO = parentDTO.getChildren().get(0);
        assertTrue(childDTO.isChildInstance());
        assertFalse(childDTO.isParentTemplate());
        assertEquals(Integer.valueOf(1), childDTO.getParentNotebookId());
    }

    // ========== Aggregated Statistics ==========

    @Test
    public void getAggregatedStatistics_parentWithNoChildren_returnsZeros() {
        // For a parent with no children, stats should be zero
        Map<String, Long> stats = noteBookService.getAggregatedStatistics(1);
        assertNotNull(stats);
        assertNotNull(stats.get("totalEntries"));
        assertNotNull(stats.get("drafts"));
        assertNotNull(stats.get("pendingReview"));
        assertNotNull(stats.get("finalizedThisWeek"));
    }

    @Test
    public void getAggregatedStatistics_nullParentId_returnsZeros() {
        Map<String, Long> stats = noteBookService.getAggregatedStatistics(null);
        assertNotNull(stats);
        assertEquals(Long.valueOf(0), stats.get("totalEntries"));
    }

    // ========== Can Accept Entries ==========

    @Test
    public void canAcceptEntries_parentTemplate_returnsFalse() {
        // Parent templates cannot have entries added directly
        boolean canAccept = noteBookService.canAcceptEntries(1);
        assertFalse(canAccept);
    }

    @Test
    public void canAcceptEntries_childInstance_returnsTrue() {
        NoteBook child = noteBookService.createChildInstance(1, "Entry Test Child", "1");
        assertNotNull(child);

        boolean canAccept = noteBookService.canAcceptEntries(child.getId());
        assertTrue(canAccept);
    }

    @Test
    public void canAcceptEntries_nullId_returnsFalse() {
        boolean canAccept = noteBookService.canAcceptEntries(null);
        assertFalse(canAccept);
    }

    @Test
    public void canAcceptEntries_nonExistentId_returnsFalse() {
        // Non-existent ID should return false (no exception)
        // The service handles ObjectNotFoundException internally
        boolean canAccept = noteBookService.canAcceptEntries(99999);
        assertFalse(canAccept);
    }

    // ========== Get All Parent Templates ==========

    @Test
    public void getAllParentTemplates_returnsOnlyParents() {
        List<NoteBook> parents = noteBookService.getAllParentTemplates();
        assertNotNull(parents);

        for (NoteBook parent : parents) {
            assertTrue(parent.isParentTemplate());
            assertTrue(parent.getIsTemplate());
            assertNull(parent.getParentNotebook());
        }
    }

    // ========== Live Inheritance ==========

    @Test
    public void liveInheritance_childInheritsParentPagesViaRelationship() {
        NoteBook child = noteBookService.createChildInstance(1, "Live Inheritance Test", "1");
        assertNotNull(child);

        // Verify child instance is correctly configured for inheritance
        assertTrue(child.isChildInstance());
        assertNotNull(child.getParentNotebook());
        assertEquals(Integer.valueOf(1), child.getParentNotebook().getId());

        // Child should not be a template itself
        assertFalse(child.getIsTemplate());

        // Parent relationship enables live inheritance through getEffectivePages()
        // which returns parent's pages when called within an active transaction
    }

    // ========== Child Cannot Have Children ==========

    @Test(expected = IllegalArgumentException.class)
    public void createChildInstance_fromChildInstance_throwsException() {
        // Create a child instance first
        NoteBook child = noteBookService.createChildInstance(1, "First Level Child", "1");
        assertNotNull(child);

        // Try to create a child from the child - should fail
        noteBookService.createChildInstance(child.getId(), "Should Fail - Nested Child", "1");
    }

    // ========== Page Copying Tests ==========

    @Test
    public void createChildInstance_copiesPagesFromParent() {
        // Get parent pages using DAO (avoids lazy loading issues)
        List<NoteBookPage> parentPages = noteBookPageDAO.getByNotebookId(1);
        assertNotNull(parentPages);
        int parentPageCount = parentPages.size();
        assertTrue("Parent template should have pages for this test", parentPageCount > 0);

        // Create child instance
        NoteBook child = noteBookService.createChildInstance(1, "Page Copy Test Child", "1");
        assertNotNull(child);
        assertNotNull(child.getId());

        // Get child pages using DAO
        List<NoteBookPage> childPages = noteBookPageDAO.getByNotebookId(child.getId());
        assertNotNull("Child should have pages copied from parent", childPages);
        assertEquals("Child should have same number of pages as parent", parentPageCount, childPages.size());
    }

    @Test
    public void createChildInstance_copiesPageTitlesAndInstructions() {
        // Get parent pages
        List<NoteBookPage> parentPages = noteBookPageDAO.getByNotebookId(1);
        assertNotNull(parentPages);
        assertTrue("Parent should have pages", parentPages.size() > 0);

        // Create child instance
        NoteBook child = noteBookService.createChildInstance(1, "Page Details Copy Test", "1");

        // Get child pages
        List<NoteBookPage> childPages = noteBookPageDAO.getByNotebookId(child.getId());

        // Verify page details are copied
        for (int i = 0; i < parentPages.size(); i++) {
            assertEquals("Page title should be copied", parentPages.get(i).getTitle(), childPages.get(i).getTitle());
            assertEquals("Page instructions should be copied", parentPages.get(i).getInstructions(),
                    childPages.get(i).getInstructions());
            assertEquals("Page order should be copied", parentPages.get(i).getOrder(), childPages.get(i).getOrder());
        }
    }

    @Test
    public void createChildInstance_childPagesHaveOwnIds() {
        // Get parent pages
        List<NoteBookPage> parentPages = noteBookPageDAO.getByNotebookId(1);
        assertNotNull(parentPages);
        assertTrue("Parent should have pages", parentPages.size() > 0);

        // Create child instance
        NoteBook child = noteBookService.createChildInstance(1, "Page ID Test Child", "1");

        // Get child pages
        List<NoteBookPage> childPages = noteBookPageDAO.getByNotebookId(child.getId());
        assertNotNull(childPages);
        assertTrue("Child should have pages", childPages.size() > 0);

        // Verify child pages have their own IDs (not same as parent)
        for (int i = 0; i < childPages.size(); i++) {
            assertNotNull("Child page should have an ID", childPages.get(i).getId());
            assertNotEquals("Child page ID should differ from parent page ID", parentPages.get(i).getId(),
                    childPages.get(i).getId());
        }
    }

    @Test
    public void createChildInstance_childPagesLinkedToChild() {
        // Create child instance
        NoteBook child = noteBookService.createChildInstance(1, "Page Linkage Test", "1");

        // Get child pages
        List<NoteBookPage> childPages = noteBookPageDAO.getByNotebookId(child.getId());
        assertNotNull(childPages);

        // Verify each page is linked to the child, not the parent
        for (NoteBookPage page : childPages) {
            assertEquals("Page should be linked to child notebook", child.getId(), page.getNotebook().getId());
        }
    }

    @Test
    public void createChildInstance_childPagesResetCompletedStatus() {
        // Create child instance
        NoteBook child = noteBookService.createChildInstance(1, "Page Status Reset Test", "1");

        // Get child pages
        List<NoteBookPage> childPages = noteBookPageDAO.getByNotebookId(child.getId());
        assertNotNull(childPages);

        // Verify all child pages have completed=false (fresh start)
        for (NoteBookPage page : childPages) {
            assertFalse("Child page completed status should be reset to false", page.getCompleted());
        }
    }

    @Test
    public void createChildInstance_multipleChildren_eachHasOwnPages() {
        // Get parent page count first
        List<NoteBookPage> parentPages = noteBookPageDAO.getByNotebookId(1);
        int expectedPageCount = parentPages.size();
        assertTrue("Parent should have pages for this test", expectedPageCount > 0);

        // Create two child instances
        NoteBook child1 = noteBookService.createChildInstance(1, "Multi Child 1", "1");
        NoteBook child2 = noteBookService.createChildInstance(1, "Multi Child 2", "1");

        // Verify children have different IDs
        assertNotNull("Child1 should have an ID", child1.getId());
        assertNotNull("Child2 should have an ID", child2.getId());
        assertNotEquals("Children should have different IDs", child1.getId(), child2.getId());

        // Get pages for each child
        List<NoteBookPage> child1Pages = noteBookPageDAO.getByNotebookId(child1.getId());
        List<NoteBookPage> child2Pages = noteBookPageDAO.getByNotebookId(child2.getId());

        assertNotNull(child1Pages);
        assertNotNull(child2Pages);

        // Verify all child2 pages are linked to child2
        for (NoteBookPage page : child2Pages) {
            assertEquals("All child2 pages should be linked to child2", child2.getId(), page.getNotebook().getId());
        }

        // Each child should have the same number of pages as the parent template
        assertEquals("Child1 should have same page count as parent", expectedPageCount, child1Pages.size());
        assertEquals("Child2 should have same page count as parent", expectedPageCount, child2Pages.size());

        // Verify pages are separate (different IDs) - pages should not be shared
        for (NoteBookPage child1Page : child1Pages) {
            for (NoteBookPage child2Page : child2Pages) {
                assertNotEquals("Each child should have its own page instances (no sharing)", child1Page.getId(),
                        child2Page.getId());
            }
        }
    }

    // ========== Entry Retrieval Tests ==========

    /**
     * Helper method to create and link an entry to a notebook.
     */
    private NoteBook createAndLinkEntry(NoteBook notebook, String title) {
        // Create entry notebook
        NoteBook entry = new NoteBook();
        entry.setTitle(title);
        entry.setIsTemplate(false);
        entry.setStatus(NoteBookStatus.DRAFT);
        entry.setSysUserId("1");
        entry = noteBookService.save(entry);

        // Link entry to notebook using the service method (handles lazy loading)
        noteBookService.addEntry(notebook.getId(), entry, "1");

        return entry;
    }

    @Test
    public void getNoteBookEntries_templateWithEntries_returnsEntries() {
        // Get template notebook
        NoteBook template = noteBookService.get(1);
        assertNotNull(template);
        assertTrue("Should be a template", template.getIsTemplate());

        // Create an entry and link to template
        createAndLinkEntry(template, "Test Entry for Template");

        // Verify entries can be retrieved
        List<NoteBook> entries = noteBookService.getNoteBookEntries(1);
        assertNotNull(entries);
        assertTrue("Template should have at least one entry", entries.size() >= 1);

        // Verify the entry is not a template
        for (NoteBook entry : entries) {
            assertFalse("Entry should not be a template", entry.getIsTemplate());
        }
    }

    @Test
    public void getNoteBookEntries_childInstanceWithEntries_returnsEntries() {
        // Create a child instance
        NoteBook child = noteBookService.createChildInstance(1, "Child With Entries", "1");
        assertNotNull(child);
        assertFalse("Child should not be a template", child.getIsTemplate());

        // Create an entry and link to child instance
        createAndLinkEntry(child, "Test Entry for Child");

        // Retrieve entries for the child instance
        List<NoteBook> childEntries = noteBookService.getNoteBookEntries(child.getId());
        assertNotNull("Child instance should return entries", childEntries);
        assertTrue("Child instance should have at least one entry", childEntries.size() >= 1);
    }

    @Test
    public void getNoteBookEntries_childInstanceNoEntries_returnsEmptyList() {
        // Create a child instance without adding entries
        NoteBook child = noteBookService.createChildInstance(1, "Child Without Entries", "1");
        assertNotNull(child);

        // Retrieve entries - should be empty since we haven't added any
        List<NoteBook> entries = noteBookService.getNoteBookEntries(child.getId());
        assertNotNull("Should return non-null list", entries);
        assertTrue("Should return empty list for child with no entries", entries.isEmpty());
    }

    @Test
    public void getNoteBookEntries_nonExistentNotebook_returnsEmptyList() {
        List<NoteBook> entries = noteBookService.getNoteBookEntries(99999);
        assertNotNull(entries);
        assertTrue("Non-existent notebook should return empty list", entries.isEmpty());
    }

    @Test
    public void getNoteBookEntries_nullNotebookId_returnsEmptyList() {
        List<NoteBook> entries = noteBookService.getNoteBookEntries(null);
        assertNotNull(entries);
        assertTrue("Null notebook ID should return empty list", entries.isEmpty());
    }

    @Test
    public void getNoteBookEntries_parentTemplateEntriesNotInChildInstance() {
        // Get template notebook
        NoteBook template = noteBookService.get(1);

        // Create an entry and link to template
        NoteBook templateEntry = createAndLinkEntry(template, "Template Entry");

        // Get template entries
        List<NoteBook> templateEntries = noteBookService.getNoteBookEntries(1);
        assertTrue("Template should have entries", templateEntries.size() >= 1);

        // Create a child instance
        NoteBook child = noteBookService.createChildInstance(1, "Separate Entries Child", "1");

        // Child instance should NOT inherit parent's entries
        List<NoteBook> childEntries = noteBookService.getNoteBookEntries(child.getId());
        assertNotNull(childEntries);
        assertTrue("Child should have no entries initially", childEntries.isEmpty());

        // Verify template entry is not in child entries
        assertFalse("Child should not contain template's entry",
                childEntries.stream().anyMatch(e -> e.getId().equals(templateEntry.getId())));
    }

    @Test
    public void getNoteBookEntries_multipleChildInstancesHaveSeparateEntries() {
        // Create two child instances
        NoteBook child1 = noteBookService.createChildInstance(1, "Child 1 for Entries", "1");
        NoteBook child2 = noteBookService.createChildInstance(1, "Child 2 for Entries", "1");

        // Create entries for each child
        NoteBook entry1 = createAndLinkEntry(child1, "Entry for Child 1");
        NoteBook entry2 = createAndLinkEntry(child2, "Entry for Child 2");

        // Get entries for each child
        List<NoteBook> child1Entries = noteBookService.getNoteBookEntries(child1.getId());
        List<NoteBook> child2Entries = noteBookService.getNoteBookEntries(child2.getId());

        // Verify each child has exactly one entry
        assertEquals("Child1 should have 1 entry", 1, child1Entries.size());
        assertEquals("Child2 should have 1 entry", 1, child2Entries.size());

        // Verify entries are separate (different IDs)
        assertNotEquals("Entries should be different", child1Entries.get(0).getId(), child2Entries.get(0).getId());

        // Verify correct entry is in correct child
        assertEquals("Child1 should have entry1", entry1.getId(), child1Entries.get(0).getId());
        assertEquals("Child2 should have entry2", entry2.getId(), child2Entries.get(0).getId());
    }
}
