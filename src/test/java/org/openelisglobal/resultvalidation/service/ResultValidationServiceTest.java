package org.openelisglobal.resultvalidation.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IResultSaveService;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.services.registration.interfaces.IResultUpdate;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultvalidation.bean.AnalysisItem;
import org.openelisglobal.resultvalidation.util.ResultValidationSaveService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;

public class ResultValidationServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ResultValidationService resultValidationService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ResultService resultService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private IStatusService statusService;

    @Autowired
    private NoteService noteService;

    @Autowired
    private SystemUserService systemUserService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/result-validation-service.xml");
    }

    @Test
    public void testPersistdata_shouldUpdateResultAndPromoteSampleToFinished() throws Exception {
        List<Result> deletableList = new ArrayList<>();
        IResultSaveService resultSaveService = new ResultValidationSaveService();

        Analysis analysis1 = analysisService.get("1");
        analysis1.setStatusId(statusService.getStatusID(AnalysisStatus.Finalized));

        List<Analysis> analysisUpdateList = new ArrayList<>();
        analysisUpdateList.add(analysis1);

        Result result1 = resultService.getResultById("1");
        result1.setValue("90.0");

        ArrayList<Result> resultUpdateList = new ArrayList<>();
        resultUpdateList.add(result1);

        Sample sample1 = sampleService.get("1");
        AnalysisItem ai1 = new AnalysisItem();
        ai1.setAccessionNumber(sample1.getAccessionNumber());

        List<AnalysisItem> resultItemList = new ArrayList<>();
        resultItemList.add(ai1);

        ArrayList<Sample> sampleUpdateList = new ArrayList<>();
        ArrayList<Note> noteUpdateList = new ArrayList<>();
        List<IResultUpdate> updaters = new ArrayList<>();

        resultValidationService.persistdata(deletableList, analysisUpdateList, resultUpdateList, resultItemList,
                sampleUpdateList, noteUpdateList, resultSaveService, updaters, "1");

        Analysis updatedAnalysis1 = analysisService.get("1");
        assertEquals("Analysis should be finalized", statusService.getStatusID(AnalysisStatus.Finalized),
                updatedAnalysis1.getStatusId());
        assertEquals("ROUTINE", updatedAnalysis1.getAnalysisType());
        assertEquals("1", updatedAnalysis1.getTest().getId());
        assertEquals("601", updatedAnalysis1.getSampleItem().getId());
        assertEquals("Y", updatedAnalysis1.getIsReportable());
        assertEquals("1", updatedAnalysis1.getRevision());

        Result updatedResult1 = resultService.getResultById("1");
        assertEquals("Result value should be updated", "90.0", updatedResult1.getValue());
        assertEquals("1", updatedResult1.getAnalysis().getId());
        assertEquals("1", updatedResult1.getAnalyte().getId());
        assertEquals("N", updatedResult1.getResultType());
        assertEquals("Y", updatedResult1.getIsReportable());
        assertEquals(1, updatedResult1.getSignificantDigits());

        Sample updatedSample = sampleService.get("1");
        assertEquals("Sample should be promoted to Finished", statusService.getStatusID(OrderStatus.Finished),
                updatedSample.getStatusId());
        assertEquals("12345", updatedSample.getAccessionNumber());
    }

    @Test
    public void testPersistdata_shouldNotPromoteSampleIfAnalysesNotFinished() throws Exception {
        List<Result> deletableList = new ArrayList<>();
        IResultSaveService resultSaveService = new ResultValidationSaveService();

        Analysis analysis1 = analysisService.get("1");
        List<Analysis> analysisUpdateList = new ArrayList<>();
        analysisUpdateList.add(analysis1);

        ArrayList<Result> resultUpdateList = new ArrayList<>();

        Sample sample1 = sampleService.get("1");
        AnalysisItem ai1 = new AnalysisItem();
        ai1.setAccessionNumber(sample1.getAccessionNumber());

        List<AnalysisItem> resultItemList = new ArrayList<>();
        resultItemList.add(ai1);

        ArrayList<Sample> sampleUpdateList = new ArrayList<>();
        ArrayList<Note> noteUpdateList = new ArrayList<>();
        List<IResultUpdate> updaters = new ArrayList<>();

        resultValidationService.persistdata(deletableList, analysisUpdateList, resultUpdateList, resultItemList,
                sampleUpdateList, noteUpdateList, resultSaveService, updaters, "1");

        Sample updatedSample = sampleService.get("1");
        assertEquals("Sample should remain Entered", statusService.getStatusID(OrderStatus.Entered),
                updatedSample.getStatusId());
        assertEquals("12345", updatedSample.getAccessionNumber());

        Analysis updatedAnalysis1 = analysisService.get("1");
        assertEquals("Analysis should remain Not Tested", statusService.getStatusID(AnalysisStatus.NotStarted),
                updatedAnalysis1.getStatusId());
        assertEquals("ROUTINE", updatedAnalysis1.getAnalysisType());
        assertEquals("1", updatedAnalysis1.getTest().getId());
        assertEquals("601", updatedAnalysis1.getSampleItem().getId());
        assertEquals("Y", updatedAnalysis1.getIsReportable());
        assertEquals("1", updatedAnalysis1.getRevision());

        Result unchangedResult1 = resultService.getResultById("1");
        assertEquals("85.0", unchangedResult1.getValue());
        assertEquals("1", unchangedResult1.getAnalysis().getId());
        assertEquals("1", unchangedResult1.getAnalyte().getId());
        assertEquals("N", unchangedResult1.getResultType());
        assertEquals("Y", unchangedResult1.getIsReportable());
        assertEquals(1, unchangedResult1.getSignificantDigits());
    }

    @Test
    public void testPersistdata_shouldDeleteResults() throws Exception {
        IResultSaveService resultSaveService = new ResultValidationSaveService();

        Result result2 = resultService.getResultById("2");

        List<Result> deletableList = new ArrayList<>();
        deletableList.add(result2);

        List<Analysis> analysisUpdateList = new ArrayList<>();
        ArrayList<Result> resultUpdateList = new ArrayList<>();
        List<AnalysisItem> resultItemList = new ArrayList<>();
        ArrayList<Sample> sampleUpdateList = new ArrayList<>();
        ArrayList<Note> noteUpdateList = new ArrayList<>();
        List<IResultUpdate> updaters = new ArrayList<>();

        resultValidationService.persistdata(deletableList, analysisUpdateList, resultUpdateList, resultItemList,
                sampleUpdateList, noteUpdateList, resultSaveService, updaters, "1");

        Result fetchedDeletedResult = resultService.getResultById("2");
        assertNull("Result should be successfully deleted and not found", fetchedDeletedResult);

        List<Result> resultsAfterDelete = resultService.getResultsByAnalysis(analysisService.get("2"));
        assertTrue("Result list should not contain the deleted result",
                resultsAfterDelete.stream().noneMatch(r -> "2".equals(r.getId())));
        assertEquals("Only deleted result should be gone; analysis 2 result count should be 0", 0,
                resultsAfterDelete.size());

        Result untouchedResult1 = resultService.getResultById("1");
        assertEquals("85.0", untouchedResult1.getValue());
        assertEquals("1", untouchedResult1.getAnalysis().getId());
        assertEquals("1", untouchedResult1.getAnalyte().getId());
        assertEquals("N", untouchedResult1.getResultType());
        assertEquals("Y", untouchedResult1.getIsReportable());
        assertEquals(1, untouchedResult1.getSignificantDigits());

        Analysis untouchedAnalysis2 = analysisService.get("2");
        assertEquals(statusService.getStatusID(AnalysisStatus.Finalized), untouchedAnalysis2.getStatusId());
        assertEquals("ROUTINE", untouchedAnalysis2.getAnalysisType());
        assertEquals("2", untouchedAnalysis2.getTest().getId());
        assertEquals("601", untouchedAnalysis2.getSampleItem().getId());
        assertEquals("Y", untouchedAnalysis2.getIsReportable());
    }

    @Test
    public void testPersistdata_shouldCreateNewNote() throws Exception {
        IResultSaveService resultSaveService = new ResultValidationSaveService();

        Note newNote = new Note();
        newNote.setSubject("New Test Subject");
        newNote.setText("New Test Text content");
        newNote.setNoteType("I");
        newNote.setReferenceId("1");
        newNote.setReferenceTableId("1");

        SystemUser sysUser = systemUserService.get("1");
        newNote.setSystemUser(sysUser);

        ArrayList<Note> noteUpdateList = new ArrayList<>();
        noteUpdateList.add(newNote);

        List<Result> deletableList = new ArrayList<>();
        List<Analysis> analysisUpdateList = new ArrayList<>();
        ArrayList<Result> resultUpdateList = new ArrayList<>();
        List<AnalysisItem> resultItemList = new ArrayList<>();
        ArrayList<Sample> sampleUpdateList = new ArrayList<>();
        List<IResultUpdate> updaters = new ArrayList<>();

        resultValidationService.persistdata(deletableList, analysisUpdateList, resultUpdateList, resultItemList,
                sampleUpdateList, noteUpdateList, resultSaveService, updaters, "1");

        List<Note> notes = noteService.getNotesChronologicallyByRefIdAndRefTable("1", "1");
        assertEquals("Should now have 2 notes for refId 1 and refTable 1", 2, notes.size());

        boolean found = false;
        for (Note n : notes) {
            if ("New Test Subject".equals(n.getSubject())) {
                assertEquals("New Test Text content", n.getText());
                assertEquals("I", n.getNoteType());
                assertEquals("1", n.getSystemUser().getId());
                found = true;
                break;
            }
        }
        assertTrue("Persisted note with specified subject should be found", found);
    }

    @Test
    public void testPersistdata_shouldUpdateExistingNote() throws Exception {
        IResultSaveService resultSaveService = new ResultValidationSaveService();

        Note existingNote = noteService.getData("1");
        assertEquals("Initial Subject", existingNote.getSubject());
        assertEquals("Initial Text", existingNote.getText());

        existingNote.setSubject("Updated Subject 1");
        existingNote.setText("Updated Text 1");

        ArrayList<Note> noteUpdateList = new ArrayList<>();
        noteUpdateList.add(existingNote);

        List<Result> deletableList = new ArrayList<>();
        List<Analysis> analysisUpdateList = new ArrayList<>();
        ArrayList<Result> resultUpdateList = new ArrayList<>();
        List<AnalysisItem> resultItemList = new ArrayList<>();
        ArrayList<Sample> sampleUpdateList = new ArrayList<>();
        List<IResultUpdate> updaters = new ArrayList<>();

        resultValidationService.persistdata(deletableList, analysisUpdateList, resultUpdateList, resultItemList,
                sampleUpdateList, noteUpdateList, resultSaveService, updaters, "1");

        Note updatedNote = noteService.getData("1");
        assertEquals("Updated Subject 1", updatedNote.getSubject());
        assertEquals("Updated Text 1", updatedNote.getText());
        assertEquals("I", updatedNote.getNoteType());
        assertEquals("1", updatedNote.getSystemUser().getId());
    }

    @Test
    public void testPersistdata_shouldExecuteUpdaters() throws Exception {
        IResultSaveService resultSaveService = new ResultValidationSaveService();

        final boolean[] updaterExecuted = { false };
        IResultUpdate testUpdater = new IResultUpdate() {
            @Override
            public void transactionalUpdate(IResultSaveService saveService) {
                assertEquals(resultSaveService, saveService);
                updaterExecuted[0] = true;
            }

            @Override
            public void postTransactionalCommitUpdate(IResultSaveService saveService) {
            }
        };

        Analysis analysis1 = analysisService.get("1");
        analysis1.setStatusId(statusService.getStatusID(AnalysisStatus.Finalized));

        List<Analysis> analysisUpdateList = new ArrayList<>();
        analysisUpdateList.add(analysis1);

        Result result1 = resultService.getResultById("1");
        result1.setValue("75.0");

        ArrayList<Result> resultUpdateList = new ArrayList<>();
        resultUpdateList.add(result1);

        Sample sample1 = sampleService.get("1");
        AnalysisItem ai1 = new AnalysisItem();
        ai1.setAccessionNumber(sample1.getAccessionNumber());

        List<AnalysisItem> resultItemList = new ArrayList<>();
        resultItemList.add(ai1);

        List<IResultUpdate> updaters = new ArrayList<>();
        updaters.add(testUpdater);

        List<Result> deletableList = new ArrayList<>();
        ArrayList<Sample> sampleUpdateList = new ArrayList<>();
        ArrayList<Note> noteUpdateList = new ArrayList<>();

        resultValidationService.persistdata(deletableList, analysisUpdateList, resultUpdateList, resultItemList,
                sampleUpdateList, noteUpdateList, resultSaveService, updaters, "1");

        assertTrue("Transactional updater should have been executed", updaterExecuted[0]);

        Analysis updatedAnalysis1 = analysisService.get("1");
        assertEquals(statusService.getStatusID(AnalysisStatus.Finalized), updatedAnalysis1.getStatusId());
        assertEquals("ROUTINE", updatedAnalysis1.getAnalysisType());
        assertEquals("1", updatedAnalysis1.getTest().getId());
        assertEquals("601", updatedAnalysis1.getSampleItem().getId());
        assertEquals("Y", updatedAnalysis1.getIsReportable());
        assertEquals("1", updatedAnalysis1.getRevision());

        Result updatedResult1 = resultService.getResultById("1");
        assertEquals("75.0", updatedResult1.getValue());
        assertEquals("1", updatedResult1.getAnalysis().getId());
        assertEquals("1", updatedResult1.getAnalyte().getId());
        assertEquals("N", updatedResult1.getResultType());
        assertEquals("Y", updatedResult1.getIsReportable());
        assertEquals(1, updatedResult1.getSignificantDigits());

        Sample updatedSample = sampleService.get("1");
        assertEquals(statusService.getStatusID(OrderStatus.Finished), updatedSample.getStatusId());
        assertEquals("12345", updatedSample.getAccessionNumber());
    }
}
