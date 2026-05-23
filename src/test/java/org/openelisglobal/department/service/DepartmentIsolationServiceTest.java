package org.openelisglobal.department.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.notebook.service.NoteBookService;
import org.openelisglobal.notebook.service.NotebookEntryService;
import org.openelisglobal.notebook.service.NotebookSecurityService;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.mock.web.MockHttpServletRequest;

@RunWith(MockitoJUnitRunner.Silent.class)
public class DepartmentIsolationServiceTest {

    private static final String USER_ID = "42";
    private static final String LAB_UNIT = "Pathology";
    private static final int LAB_UNIT_ID = 7;

    @Mock
    private NotebookSecurityService notebookSecurityService;

    @Mock
    private NotebookEntryService notebookEntryService;

    @Mock
    private NoteBookService noteBookService;

    @Mock
    private TestSectionService testSectionService;

    @Mock
    private UserRoleService userRoleService;

    @Mock
    private SampleItemService sampleItemService;

    @Mock
    private SampleService sampleService;

    @InjectMocks
    private DepartmentIsolationService service;

    private MockHttpServletRequest request;

    @Before
    public void setUp() {
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(Integer.valueOf(USER_ID));
        usd.setLoginLabUnit(LAB_UNIT_ID);

        request = new MockHttpServletRequest();
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, usd);

        TestSection section = new TestSection();
        section.setTestSectionName(LAB_UNIT);
        when(testSectionService.getTestSectionById(String.valueOf(LAB_UNIT_ID))).thenReturn(section);
        when(notebookSecurityService.hasGlobalAdminRole(USER_ID)).thenReturn(false);
        when(userRoleService.getUserLabUnitRoles(USER_ID)).thenReturn(null);
    }

    @Test
    public void deniesInventoryItemWhenOnlyProjectNameIsSetWithoutDepartmentColumn() {
        InventoryItem item = new InventoryItem();
        item.setProjectName("Pathology Project");

        assertFalse(service.canAccessInventoryItem(item, request));
    }

    @Test
    public void deniesInventoryItemWithoutProjectForDepartmentUser() {
        InventoryItem item = new InventoryItem();

        assertFalse(service.canAccessInventoryItem(item, request));
    }

    @Test
    public void canAccessInventoryItemWhenDepartmentColumnMatchesLabUnit() {
        InventoryItem item = new InventoryItem();
        item.setDepartmentTestSectionId(LAB_UNIT_ID);
        when(testSectionService.getTestSectionById(String.valueOf(LAB_UNIT_ID)))
                .thenReturn(buildDepartment("7", LAB_UNIT));

        assertTrue(service.canAccessInventoryItem(item, request));
    }

    @Test
    public void deniesInventoryItemWhenDepartmentColumnDoesNotMatchLabUnit() {
        InventoryItem item = new InventoryItem();
        item.setDepartmentTestSectionId(99);
        when(testSectionService.getTestSectionById("99")).thenReturn(buildDepartment("99", "Biorepository"));

        assertFalse(service.canAccessInventoryItem(item, request));
    }

    @Test
    public void departmentColumnTakesPrecedenceOverNotebookMismatch() {
        InventoryItem item = new InventoryItem();
        item.setDepartmentTestSectionId(LAB_UNIT_ID);
        item.setProjectName("Biorepository Project");
        when(testSectionService.getTestSectionById(String.valueOf(LAB_UNIT_ID)))
                .thenReturn(buildDepartment("7", LAB_UNIT));

        assertTrue(service.canAccessInventoryItem(item, request));
    }

    @Test
    public void strictIntersectionAllowsWhenOwnedDepartmentMatchesEvenIfProjectDiffers() {
        InventoryItem item = new InventoryItem();
        item.setDepartmentTestSectionId(LAB_UNIT_ID);
        item.setProjectName("Biorepository Project");
        when(testSectionService.getTestSectionById(String.valueOf(LAB_UNIT_ID)))
                .thenReturn(buildDepartment("7", LAB_UNIT));

        NoteBook notebook = new NoteBook();
        notebook.setId(11);
        when(noteBookService.getAllMatching("title", "Biorepository Project")).thenReturn(List.of(notebook));
        when(noteBookService.getNoteBookDepartments(11)).thenReturn(
                List.of(buildDepartment("9", "Biorepository")).stream().collect(java.util.stream.Collectors.toSet()));

        assertTrue(service.canAccessInventoryItemStrictIntersection(item, request));
    }

    @Test
    public void resolveDepartmentForStrictScopedCreateUsesActiveDepartmentEvenWhenProjectDiffers() {
        when(testSectionService.getTestSectionById(String.valueOf(LAB_UNIT_ID)))
                .thenReturn(buildDepartment("7", LAB_UNIT));

        NoteBook notebook = new NoteBook();
        notebook.setId(11);
        when(noteBookService.getAllMatching("title", "Biorepository Project")).thenReturn(List.of(notebook));
        when(noteBookService.getNoteBookDepartments(11)).thenReturn(
                List.of(buildDepartment("9", "Biorepository")).stream().collect(java.util.stream.Collectors.toSet()));

        assertEquals(Integer.valueOf(LAB_UNIT_ID),
                service.resolveDepartmentForStrictScopedCreate(request, LAB_UNIT_ID, "Biorepository Project"));
    }

    @Test
    public void resolveDepartmentForStrictScopedCreateKeepsDepartmentWhenProjectMatches() {
        when(testSectionService.getTestSectionById(String.valueOf(LAB_UNIT_ID)))
                .thenReturn(buildDepartment("7", LAB_UNIT));

        NoteBook notebook = new NoteBook();
        notebook.setId(10);
        when(noteBookService.getAllMatching("title", "Pathology Project")).thenReturn(List.of(notebook));
        when(noteBookService.getNoteBookDepartments(10)).thenReturn(
                List.of(buildDepartment("7", LAB_UNIT)).stream().collect(java.util.stream.Collectors.toSet()));

        assertEquals(Integer.valueOf(LAB_UNIT_ID),
                service.resolveDepartmentForStrictScopedCreate(request, LAB_UNIT_ID, "Pathology Project"));
    }

    @Test
    public void inventoryProjectConsistencyRejectsResolvedNotebookInDifferentDepartment() {
        NoteBook notebook = new NoteBook();
        notebook.setId(11);
        when(noteBookService.getAllMatching("title", "Biorepository Project")).thenReturn(List.of(notebook));
        when(noteBookService.getNoteBookDepartments(11)).thenReturn(
                List.of(buildDepartment("9", "Biorepository")).stream().collect(java.util.stream.Collectors.toSet()));

        assertFalse(service.isInventoryProjectConsistent(LAB_UNIT_ID, "Biorepository Project"));
    }

    @Test
    public void inventoryProjectConsistencyAllowsLegacyUnmatchedProjectName() {
        when(noteBookService.getAllMatching("title", "Legacy Project")).thenReturn(List.of());

        assertTrue(service.isInventoryProjectConsistent(LAB_UNIT_ID, "Legacy Project"));
    }

    @Test
    public void assignableInventoryProjectsExcludesTemplatesOutsideUserDepartment() {
        NoteBook parent = new NoteBook();
        parent.setId(20);
        parent.setTitle("Biorepository Laboratory");
        parent.setIsTemplate(true);

        when(noteBookService.getAllParentTemplates()).thenReturn(List.of(parent));
        when(noteBookService.getChildInstances(20)).thenReturn(List.of());
        when(noteBookService.getNoteBookDepartments(20)).thenReturn(
                List.of(buildDepartment("9", "Biorepository")).stream().collect(java.util.stream.Collectors.toSet()));
        when(notebookSecurityService.canViewTemplate(20, USER_ID, LAB_UNIT)).thenReturn(true);

        List<Map<String, String>> projects = service.getAssignableInventoryProjects(request, LAB_UNIT_ID);

        assertTrue(projects.isEmpty());
    }

    @Test
    public void assignableInventoryProjectsPreferChildInstancesWithinDepartment() {
        NoteBook parent = new NoteBook();
        parent.setId(10);
        parent.setTitle("Bioanalytical Laboratory");
        parent.setIsTemplate(true);

        NoteBook childOne = new NoteBook();
        childOne.setId(101);
        childOne.setTitle("Bioanalytical Laboratory - Lab 1");
        childOne.setIsTemplate(false);
        childOne.setParentNotebook(parent);

        NoteBook childTwo = new NoteBook();
        childTwo.setId(102);
        childTwo.setTitle("Bioanalytical Laboratory - Lab 2");
        childTwo.setIsTemplate(false);
        childTwo.setParentNotebook(parent);

        when(noteBookService.getAllParentTemplates()).thenReturn(List.of(parent));
        when(noteBookService.getChildInstances(10)).thenReturn(List.of(childOne, childTwo));
        when(noteBookService.getNoteBookDepartments(10)).thenReturn(
                List.of(buildDepartment("7", LAB_UNIT)).stream().collect(java.util.stream.Collectors.toSet()));
        when(notebookSecurityService.canViewTemplate(10, USER_ID, LAB_UNIT)).thenReturn(true);

        List<Map<String, String>> projects = service.getAssignableInventoryProjects(request, LAB_UNIT_ID);

        assertEquals(2, projects.size());
        assertEquals("101", projects.get(0).get("id"));
        assertEquals("Bioanalytical Laboratory - Lab 1", projects.get(0).get("value"));
        assertEquals("102", projects.get(1).get("id"));
        assertEquals("Bioanalytical Laboratory - Lab 2", projects.get(1).get("value"));
    }

    @Test
    public void canAccessStorageRoomWhenDepartmentMatchesLabUnit() {
        StorageRoom room = new StorageRoom();
        room.setDepartmentTestSectionId(LAB_UNIT_ID);

        assertTrue(service.canAccessStorageRoom(room, request));
    }

    @Test
    public void deniesStorageRoomWithoutDepartmentForRestrictedUser() {
        StorageRoom room = new StorageRoom();

        assertFalse(service.canAccessStorageRoom(room, request));
    }

    @Test
    public void canAccessSampleItemWhenAnyLinkedEntryIsVisible() {
        SampleItem sampleItem = new SampleItem();
        sampleItem.setId("99");
        NotebookEntry entry = new NotebookEntry();
        NoteBook notebook = new NoteBook();
        notebook.setId(10);
        entry.setNotebook(notebook);

        when(notebookEntryService.findBySampleItemId(99)).thenReturn(List.of(entry));
        when(noteBookService.getNoteBookDepartments(10)).thenReturn(
                List.of(buildDepartment("7", LAB_UNIT)).stream().collect(java.util.stream.Collectors.toSet()));

        assertTrue(service.canAccessSampleItem(sampleItem, request));
    }

    @Test
    public void deniesSampleItemWhenNoLinkedEntryIsVisible() {
        SampleItem sampleItem = new SampleItem();
        sampleItem.setId("99");

        when(notebookEntryService.findBySampleItemId(99)).thenReturn(List.of());

        assertFalse(service.canAccessSampleItem(sampleItem, request));
    }

    @Test
    public void globalAdminBypassesDepartmentIsolation() {
        when(notebookSecurityService.hasGlobalAdminRole(USER_ID)).thenReturn(true);

        assertTrue(service.canAccessInventoryItem(new InventoryItem(), request));
        assertTrue(service.canAccessSampleItem(new SampleItem(), request));
    }

    @Test
    public void systemAdminHasUnrestrictedDepartmentAccess() {
        when(userRoleService.userInRole(USER_ID, org.openelisglobal.common.constants.Constants.ROLE_SYSTEM_ADMIN))
                .thenReturn(true);

        assertTrue(service.hasUnrestrictedDepartmentAccess(request));
    }

    @Test
    public void assignableLabDepartmentsForAdminUsesActiveTestSections() {
        when(notebookSecurityService.hasGlobalAdminRole(USER_ID)).thenReturn(true);
        TestSection bacteriology = buildDepartment("168", "Bacteriology");
        TestSection immunology = buildDepartment("59", "Immunology");
        TestSection allLabUnits = buildDepartment("0", "All Lab Units");
        when(testSectionService.getAllActiveTestSections()).thenReturn(List.of(bacteriology, allLabUnits, immunology));

        List<Map<String, String>> rows = service.getAssignableLabDepartments(request);

        assertEquals(2, rows.size());
        assertTrue(rows.stream().anyMatch(row -> "168".equals(row.get("id"))));
        assertTrue(rows.stream().anyMatch(row -> "59".equals(row.get("id"))));
        assertFalse(rows.stream().anyMatch(row -> "All Lab Units".equals(row.get("value"))));
    }

    @Test
    public void assignableLabDepartmentsForRestrictedUserUsesSelectableSections() {
        TestSection pathology = buildDepartment("7", LAB_UNIT);
        when(testSectionService.getTestSectionById("7")).thenReturn(pathology);

        List<Map<String, String>> rows = service.getAssignableLabDepartments(request);

        assertEquals(1, rows.size());
        assertEquals("7", rows.get(0).get("id"));
    }

    @Test
    public void deniesInventoryItemWhenNotebookDepartmentDoesNotMatchUserDepartment() {
        InventoryItem item = new InventoryItem();
        item.setProjectName("Biorepository Project");

        NoteBook notebook = new NoteBook();
        notebook.setId(11);
        when(noteBookService.getAllMatching("title", "Biorepository Project")).thenReturn(List.of(notebook));
        when(noteBookService.getNoteBookDepartments(11)).thenReturn(
                List.of(buildDepartment("9", "Biorepository")).stream().collect(java.util.stream.Collectors.toSet()));

        assertFalse(service.canAccessInventoryItem(item, request));
    }

    @Test
    public void canAccessSampleIdentifierByNumericIdWhenDepartmentMatches() {
        SampleItem sampleItem = new SampleItem();
        sampleItem.setId("99");
        NoteBook notebook = new NoteBook();
        notebook.setId(10);
        NotebookEntry entry = new NotebookEntry();
        entry.setNotebook(notebook);

        when(sampleItemService.getData("99")).thenReturn(sampleItem);
        when(notebookEntryService.findBySampleItemId(99)).thenReturn(List.of(entry));
        when(noteBookService.getNoteBookDepartments(10)).thenReturn(
                List.of(buildDepartment("7", LAB_UNIT)).stream().collect(java.util.stream.Collectors.toSet()));

        assertTrue(service.canAccessSampleItemIdentifier("99", request));
    }

    @Test
    public void canAccessBioSampleWhenSampleItemAccessAllowed() {
        SampleItem sampleItem = new SampleItem();
        sampleItem.setId("99");
        NoteBook notebook = new NoteBook();
        notebook.setId(10);
        NotebookEntry entry = new NotebookEntry();
        entry.setNotebook(notebook);
        when(notebookEntryService.findBySampleItemId(99)).thenReturn(List.of(entry));
        when(noteBookService.getNoteBookDepartments(10)).thenReturn(
                List.of(buildDepartment("7", LAB_UNIT)).stream().collect(java.util.stream.Collectors.toSet()));

        BioSample bioSample = new BioSample();
        bioSample.setSampleItem(sampleItem);

        assertTrue(service.canAccessBioSample(bioSample, request));
    }

    @Test
    public void deniesBioSampleWhenSampleItemHasNoNotebookDepartmentMatch() {
        BioSample bioSample = new BioSample();
        SampleItem sampleItem = new SampleItem();
        sampleItem.setId("1");
        bioSample.setSampleItem(sampleItem);
        when(notebookEntryService.findBySampleItemId(1)).thenReturn(List.of());

        assertFalse(service.canAccessBioSample(bioSample, request));
    }

    private TestSection buildDepartment(String id, String name) {
        TestSection section = new TestSection();
        section.setId(id);
        section.setTestSectionName(name);
        return section;
    }
}
