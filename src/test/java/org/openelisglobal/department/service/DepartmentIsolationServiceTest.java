package org.openelisglobal.department.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.notebook.service.NoteBookService;
import org.openelisglobal.notebook.service.NotebookSecurityService;
import org.openelisglobal.notebook.service.NotebookEntryService;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
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
    public void canAccessInventoryItemWhenProjectNotebookIsVisibleToDepartment() {
        InventoryItem item = new InventoryItem();
        item.setProjectName("Pathology Project");

        NoteBook notebook = new NoteBook();
        notebook.setId(10);
        when(noteBookService.getAllMatching("title", "Pathology Project")).thenReturn(List.of(notebook));
        when(notebookSecurityService.canViewTemplate(10, USER_ID, LAB_UNIT)).thenReturn(true);

        assertTrue(service.canAccessInventoryItem(item, request));
    }

    @Test
    public void deniesInventoryItemWithoutProjectForDepartmentUser() {
        InventoryItem item = new InventoryItem();

        assertFalse(service.canAccessInventoryItem(item, request));
    }

    @Test
    public void canAccessSampleItemWhenAnyLinkedEntryIsVisible() {
        SampleItem sampleItem = new SampleItem();
        sampleItem.setId("99");
        NotebookEntry entry = new NotebookEntry();

        when(notebookEntryService.findBySampleItemId(99)).thenReturn(List.of(entry));
        when(notebookSecurityService.canViewEntry(entry, USER_ID, LAB_UNIT)).thenReturn(true);

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
}
