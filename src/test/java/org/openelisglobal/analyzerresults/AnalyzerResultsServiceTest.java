package org.openelisglobal.analyzerresults;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.analyzerresults.valueholder.SampleGrouping;
import org.openelisglobal.common.services.StatusSet;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Database query and persistence checks; fixture replacement rolls back with
 * each test.
 */
@Transactional
public class AnalyzerResultsServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AnalyzerResultsService analyzerResultsService;
    @Autowired
    private NoteService noteService;

    @PersistenceContext
    protected EntityManager entityManager;

    private List<AnalyzerResults> analyzerResultsList;
    private Map<String, Object> propertyValues;
    private List<String> orderProperties;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/analyzer-results.xml");

        propertyValues = new HashMap<>();
        propertyValues.put("completeDate", Timestamp.valueOf("2025-07-01 09:15:00"));
        orderProperties = new ArrayList<>();
        orderProperties.add("accessionNumber");
    }

    @Test
    public void readAnalyzerResults_ShouldReturnAnalyzerResults_UsingAnId() {
        AnalyzerResults analyzerResults = analyzerResultsService.readAnalyzerResults("1002");
        assertNotNull(analyzerResults);
        assertEquals(Timestamp.valueOf("2025-07-01 09:15:00"), analyzerResults.getCompleteDate());
        assertTrue(analyzerResults.isReadOnly());
        assertFalse(analyzerResults.getIsControl());
    }

    @Test
    public void getResultsByAnalyzer_ShouldReturnAnalyzerResults_UsingAnAnalyzerId() {
        analyzerResultsList = analyzerResultsService.getResultsbyAnalyzer("2001");
        assertNotNull(analyzerResultsList);
        assertEquals("ACC123456", analyzerResultsList.get(0).getAccessionNumber());
        assertFalse(analyzerResultsList.get(0).isReadOnly());
    }

    @Test
    public void insertAnalyzerResults_ShouldInsertAListOfAnalyzerResults() {
        List<AnalyzerResults> analyzerResultsList = analyzerResultsService.getAll();
        assertFalse(analyzerResultsList.isEmpty());
        analyzerResultsService.deleteAll(analyzerResultsList);
        List<AnalyzerResults> newAnalyzerResultsList = analyzerResultsService.getAll();
        assertTrue(newAnalyzerResultsList.isEmpty());
        AnalyzerResults analyzerResults = new AnalyzerResults();
        analyzerResults.setAnalyzerId("2001");
        analyzerResults.setTestName("Body mass");
        analyzerResults.setAccessionNumber("QAN23L");
        analyzerResults.setResult("278");
        analyzerResults.setIsControl(false);
        List<AnalyzerResults> insertAnalyzerResults = new ArrayList<>();
        insertAnalyzerResults.add(analyzerResults);
        analyzerResultsService.insertAnalyzerResults(insertAnalyzerResults, TEST_SYS_USER_ID);
        entityManager.flush();
        entityManager.clear();
        List<AnalyzerResults> persisted = analyzerResultsService.getAll();
        assertEquals(1, persisted.size());
        assertEquals("QAN23L", persisted.get(0).getAccessionNumber());
        assertEquals("278", persisted.get(0).getResult());
        assertTrue("Insertion must record the real audit actor", historyCount(persisted.get(0).getId(), "I") > 0);
    }

    @Test
    public void persistAnalyzerResults_ShouldDeleteAListOfAnalyzerResultsAndInsertANewSampleGroupingList() {
        AnalyzerResults analyzerResult = analyzerResultsService.get("1003");
        List<AnalyzerResults> deletableAnalyzerResults = new ArrayList<>();
        deletableAnalyzerResults.add(analyzerResult);

        SampleGrouping sampleGrouping = new SampleGrouping();
        List<SampleGrouping> sampleGroupingList = new ArrayList<>();

        Sample sample = new Sample();
        sample.setAccessionNumber("78891");
        sample.setEnteredDate(Date.valueOf("2025-07-01"));
        sample.setReceivedDate(Date.valueOf("2025-07-01"));
        sample.setIsConfirmation(true);
        sampleGrouping.sample = sample;

        SampleItem sampleItem = new SampleItem();
        sampleItem.setSortOrder("2");
        sampleItem.setSample(sample);
        sampleItem.setLastupdated(Timestamp.valueOf("2025-02-01 12:00:00"));
        sampleItem.setStatusId("401");
        sampleGrouping.sampleItem = sampleItem;

        Patient patient = new Patient();
        patient.setPerson(new Person());
        patient.setRace("Red");
        patient.setBirthDate(Timestamp.valueOf("2014-03-20 12:00:00"));
        sampleGrouping.patient = patient;

        List<Note> notes = noteService.getAll();
        noteService.deleteAll(notes);
        Note note = new Note();
        note.setSysUserId(TEST_SYS_USER_ID);
        note.setReferenceId("3001");
        note.setReferenceTableId("1");
        note.setNoteType("G");
        note.setSubject("General Observation");
        note.setText("Patient shows signs of improvement.");
        List<Note> noteList = new ArrayList<>();
        noteList.add(note);
        sampleGrouping.noteList = noteList;

        Analysis analysis = new Analysis();
        analysis.setSampleItem(sampleItem);
        analysis.setAnalysisType("Endoscopy");
        analysis.setStartedDate(Date.valueOf("2024-06-17"));
        List<Analysis> analysisList = new ArrayList<>();
        analysisList.add(analysis);
        sampleGrouping.analysisList = analysisList;

        Result result = new Result();
        result.setAnalysis(analysis);
        result.setIsReportable("Y");
        result.setResultType("N");
        result.setLastupdated(Timestamp.valueOf("2025-11-16 10:00:00"));
        List<Result> resultList = new ArrayList<>();
        resultList.add(result);
        sampleGrouping.resultList = resultList;

        Map<String, List<String>> reflexMap = new HashMap<>();
        reflexMap.put("Trigger_A", Arrays.asList("Reflex_A1", "Reflex_A2"));
        reflexMap.put("triger_B", Arrays.asList("Reflex_B1", "Reflex_B2"));
        sampleGrouping.triggersToSelectedReflexesMap = reflexMap;

        sampleGrouping.sampleHuman = new SampleHuman();
        sampleGrouping.statusSet = new StatusSet();
        sampleGrouping.addSample = true;
        sampleGrouping.updateSample = false;
        sampleGrouping.addSampleItem = true;

        sampleGroupingList.add(sampleGrouping);
        int priorDeleteHistory = historyCount(analyzerResult.getId(), "D");
        analyzerResultsService.persistAnalyzerResults(deletableAnalyzerResults, sampleGroupingList, TEST_SYS_USER_ID);
        entityManager.flush();
        entityManager.clear();
        assertEquals(priorDeleteHistory + 1, historyCount(analyzerResult.getId(), "D"));
        List<AnalyzerResults> analyzerResults = analyzerResultsService.getAll();
        assertFalse(analyzerResults.contains(analyzerResult));

        Sample sampleInDb = entityManager.find(Sample.class, sampleGrouping.sample.getId());
        assertNotNull(sampleInDb);
        assertEquals("78891", sampleInDb.getAccessionNumber());

        Analysis analysisInDB = entityManager.find(Analysis.class, sampleGrouping.analysisList.get(0).getId());
        assertNotNull(analysisInDB);
        assertEquals("Endoscopy", analysisInDB.getAnalysisType());
    }

    @Test
    public void getAll_ShouldReturnAllAnalyzerResults() {
        analyzerResultsList = analyzerResultsService.getAll();
        assertNotNull(analyzerResultsList);
        assertEquals(4, analyzerResultsList.size());
        assertEquals("1003", analyzerResultsList.get(2).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingAnalyzerResults_UsingPropertyNameAndValue() {
        analyzerResultsList = analyzerResultsService.getAllMatching("analyzerId", "2001");
        assertNotNull(analyzerResultsList);
        assertEquals(2, analyzerResultsList.size());
        assertEquals("1001", analyzerResultsList.get(0).getId());
        assertEquals("1004", analyzerResultsList.get(1).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingAnalyzerResults_UsingAMap() {
        analyzerResultsList = analyzerResultsService.getAllMatching(propertyValues);
        assertNotNull(analyzerResultsList);
        assertEquals(2, analyzerResultsList.size());
        assertEquals("1003", analyzerResultsList.get(1).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedAnalyzerResults_UsingAnOrderProperty() {
        analyzerResultsList = analyzerResultsService.getAllOrdered("accessionNumber", false);
        assertNotNull(analyzerResultsList);
        assertEquals(4, analyzerResultsList.size());
        assertEquals("1002", analyzerResultsList.get(3).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrdered_UsingAList() {
        analyzerResultsList = analyzerResultsService.getAllOrdered(orderProperties, true);
        assertNotNull(analyzerResultsList);
        assertEquals(4, analyzerResultsList.size());
        assertEquals("1002", analyzerResultsList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedAnalyzerResults_UsingPropertyNameAndValueAndAnOrderProperty() {
        analyzerResultsList = analyzerResultsService.getAllMatchingOrdered("analyzerId", "2002", "lastupdated", true);
        assertNotNull(analyzerResultsList);
        assertEquals(2, analyzerResultsList.size());
        assertEquals("1002", analyzerResultsList.get(1).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedAnalyzerResults_UsingPropertyNameAndValueAndAList() {
        analyzerResultsList = analyzerResultsService.getAllMatchingOrdered("analyzerId", "2002", orderProperties, true);
        assertNotNull(analyzerResultsList);
        assertEquals(2, analyzerResultsList.size());
        assertEquals("1002", analyzerResultsList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedAnalyzerResults_UsingAMapAndAnOrderProperty() {
        analyzerResultsList = analyzerResultsService.getAllMatchingOrdered(propertyValues, "result", true);
        assertNotNull(analyzerResultsList);
        assertEquals(2, analyzerResultsList.size());
        assertEquals("1003", analyzerResultsList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedAnalyzerResults_UsingAMapAndAList() {
        analyzerResultsList = analyzerResultsService.getAllMatchingOrdered(propertyValues, orderProperties, false);
        assertNotNull(analyzerResultsList);
        assertEquals(2, analyzerResultsList.size());
        assertEquals("1003", analyzerResultsList.get(0).getId());
    }

    @Test
    public void getPage_ShouldReturnAPageOfAnalyzerResults_UsingAPageNumber() {
        analyzerResultsList = analyzerResultsService.getPage(1);
        assertResultMembership("1001", "1002", "1003", "1004");
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfAnalyzerResults_UsingAPropertyNameAndValue() {
        analyzerResultsList = analyzerResultsService.getMatchingPage("analyzerId", "2001", 1);
        assertResultMembership("1001", "1004");
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfAnalyzerResults_UsingAMap() {
        analyzerResultsList = analyzerResultsService.getMatchingPage(propertyValues, 1);
        assertResultMembership("1002", "1003");
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfAnalyzerResults_UsingAnOrderProperty() {
        analyzerResultsList = analyzerResultsService.getOrderedPage("lastupdated", true, 1);
        assertResultOrder("1004", "1003", "1002", "1001");
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfAnalyzerResults_UsingAList() {
        analyzerResultsList = analyzerResultsService.getOrderedPage(orderProperties, false, 1);
        assertResultOrder("1001", "1003", "1004", "1002");
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfAnalyzerResults_UsingAPropertyNameAndValueAndAnOrderProperty() {
        analyzerResultsList = analyzerResultsService.getMatchingOrderedPage("analyzerId", "2002", "lastupdated", true,
                1);
        assertResultOrder("1003", "1002");
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfAnalyzerResults_UsingAPropertyNameAndValueAndAList() {
        analyzerResultsList = analyzerResultsService.getMatchingOrderedPage("analyzerId", "2002", orderProperties, true,
                1);
        assertResultOrder("1002", "1003");
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfAnalyzerResults_UsingAMapAndAnOrderProperty() {
        analyzerResultsList = analyzerResultsService.getMatchingOrderedPage(propertyValues, "analyzerId", false, 1);
        assertResultMembership("1002", "1003");
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfAnalyzerResults_UsingAMapAndAList() {
        analyzerResultsList = analyzerResultsService.getMatchingOrderedPage(propertyValues, orderProperties, false, 1);
        assertResultOrder("1003", "1002");
    }

    @Test
    public void delete_ShouldDeleteAnAnalyzerResult() {
        analyzerResultsList = analyzerResultsService.getAll();
        assertEquals(4, analyzerResultsList.size());
        AnalyzerResults analyzerResults = analyzerResultsService.get("1002");
        analyzerResultsService.delete(analyzerResults);
        List<AnalyzerResults> newAnalyzerResultsList = analyzerResultsService.getAll();
        assertEquals(3, newAnalyzerResultsList.size());
    }

    @Test
    public void deleteAll_ShouldDeleteAllAnalyzerResults() {
        analyzerResultsList = analyzerResultsService.getAll();
        assertEquals(4, analyzerResultsList.size());
        analyzerResultsService.deleteAll(analyzerResultsList);
        List<AnalyzerResults> updatedAnalyzerResultsList = analyzerResultsService.getAll();
        assertTrue(updatedAnalyzerResultsList.isEmpty());
    }

    @Test
    public void orderedPageStartsAtTheRequestedRecordAndEndsBeyondTheLastRecord() {
        analyzerResultsList = analyzerResultsService.getOrderedPage("accessionNumber", false, 3);
        assertResultOrder("1004", "1002");
        assertTrue(analyzerResultsService.getOrderedPage("accessionNumber", false, 5).isEmpty());
    }

    @Test
    public void matchingPageExcludesOtherAnalyzers() {
        assertTrue(analyzerResultsService.getMatchingPage("analyzerId", "999999", 1).isEmpty());
        analyzerResultsList = analyzerResultsService.getMatchingOrderedPage("analyzerId", "2001", "accessionNumber",
                false, 2);
        assertResultOrder("1004");
    }

    private void assertResultMembership(String... expectedIds) {
        assertEquals(expectedIds.length, analyzerResultsList.size());
        assertEquals(Set.of(expectedIds),
                analyzerResultsList.stream().map(AnalyzerResults::getId).collect(Collectors.toSet()));
    }

    private void assertResultOrder(String... expectedIds) {
        assertEquals(List.of(expectedIds),
                analyzerResultsList.stream().map(AnalyzerResults::getId).collect(Collectors.toList()));
    }

    private int historyCount(String resultId, String activity) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM clinlims.history h"
                + " JOIN clinlims.reference_tables r ON r.id = h.reference_table"
                + " WHERE LOWER(r.name) = 'analyzer_results' AND h.reference_id = ? AND h.activity = ? AND h.sys_user_id = ?",
                Integer.class, resultId, activity, Integer.valueOf(TEST_SYS_USER_ID));
    }

}
