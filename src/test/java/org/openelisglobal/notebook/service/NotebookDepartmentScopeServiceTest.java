package org.openelisglobal.notebook.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;

@RunWith(MockitoJUnitRunner.class)
public class NotebookDepartmentScopeServiceTest {

    @InjectMocks
    private NotebookDepartmentScopeServiceImpl scopeService;

    @Mock
    private NoteBookService noteBookService;

    @Mock
    private TestSectionService testSectionService;

    @Test
    public void resolveNotebookDepartmentIds_biorepositoryOnly_fallsBackWhenNotebookHasNoDepartments() {
        Integer notebookId = 117;
        when(noteBookService.getNoteBookDepartments(notebookId)).thenReturn(Collections.emptySet());

        NoteBook notebook = new NoteBook();
        notebook.setId(notebookId);
        notebook.setWorkflowType("biorepository");
        notebook.setTitle("Biorepository Laboratory - Lab 1");
        when(noteBookService.get(notebookId)).thenReturn(notebook);

        TestSection biorepository = new TestSection();
        biorepository.setId("196");
        biorepository.setTestSectionName("Biorepository Laboratory");
        when(testSectionService.getTestSectionByName("Biorepository Laboratory")).thenReturn(biorepository);
        when(testSectionService.getAllActiveTestSections()).thenReturn(Collections.singletonList(biorepository));

        Set<Integer> ids = scopeService.resolveNotebookDepartmentIds(notebookId, true);

        assertFalse(ids.isEmpty());
        assertTrue(ids.contains(196));
    }

    @Test
    public void isBiorepositoryNotebook_detectsWorkflowType() {
        NoteBook notebook = new NoteBook();
        notebook.setWorkflowType("biorepository");
        when(noteBookService.get(27)).thenReturn(notebook);

        assertTrue(scopeService.isBiorepositoryNotebook(27));
    }
}
