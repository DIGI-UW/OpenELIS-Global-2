package org.openelisglobal.analysisqaevent;

import java.sql.Date;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysisqaevent.dao.AnalysisQaEventDAO;
import org.openelisglobal.analysisqaevent.service.AnalysisQaEventServiceImpl;
import org.openelisglobal.analysisqaevent.valueholder.AnalysisQaEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.openelisglobal.analysisqaevent.service.AnalysisQaEventService;

import static org.junit.Assert.*;

public class AnalysisQaEventServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    AnalysisQaEventService analysisQaEventService;

    @Autowired
    AnalysisQaEventDAO AnalysisQaEventDAO;

    @Before
    public void init() throws Exception {

        executeDataSetWithStateManagement("testdata/analysisqaevent.xml");
    }

    @Test
    public void verifyTestData() {
        // Fetch the AnalysisQaEvent records from the database
        List<AnalysisQaEvent> analysisQaEvents = analysisQaEventService.getAll();  // Assuming a getAll() method exists

        // Print the number of records in the database
        System.out.println("AnalysisQaEvents we have in db: " + analysisQaEvents.size());

        // Print the details of each AnalysisQaEvent record
        analysisQaEvents.forEach(analysisQaEvent -> System.out
                .println(analysisQaEvent.getId() + " - " + analysisQaEvent.getQaEventId() + " - "
                        + analysisQaEvent.getAnalysisId() + " - " + analysisQaEvent.getCompletedDate()));
    }

    @Test
    public void GetAnalysisQaEventById() {
        // Fetch the AnalysisQaEvent with ID 1
        AnalysisQaEvent analysisQaEvent = analysisQaEventService.get("1");

        // Assertions

        assertEquals("1", analysisQaEvent.getId());
        assertEquals("1", analysisQaEvent.getAnalysisId());
        assertEquals("1", analysisQaEvent.getQaEventId());
        assertEquals(Date.valueOf("2023-10-01"), analysisQaEvent.getCompletedDate());
    }

    @Test
    public void InsertAnalysisQaEvent() {
        // Create a new AnalysisQaEvent
        AnalysisQaEvent newAnalysisQaEvent = new AnalysisQaEvent();
        newAnalysisQaEvent.setId("3");
        newAnalysisQaEvent.setAnalysisId("1");
        newAnalysisQaEvent.setQaEventId("2");
        newAnalysisQaEvent.setCompletedDate(Date.valueOf("2023-10-03"));

        // Insert the new AnalysisQaEvent
        String analysisQaEventId = analysisQaEventService.insert(newAnalysisQaEvent);

        // Assertions
        assertNotNull(analysisQaEventId);
        AnalysisQaEvent retrievedAnalysisQaEvent = analysisQaEventService.get(analysisQaEventId);
        assertNotNull(retrievedAnalysisQaEvent);
        assertEquals("3", retrievedAnalysisQaEvent.getId());
        assertEquals("1", retrievedAnalysisQaEvent.getAnalysisId());
        assertEquals("2", retrievedAnalysisQaEvent.getQaEventId());
    }

    @Test
    public void UpdateAnalysisQaEvent() {
        // Fetch the AnalysisQaEvent with ID 1
        AnalysisQaEvent analysisQaEvent = analysisQaEventService.get("1");

        // Update the completed date
        analysisQaEvent.setCompletedDate(Date.valueOf("2023-10-05"));
        analysisQaEventService.update(analysisQaEvent);

        // Fetch the updated AnalysisQaEvent
        AnalysisQaEvent updatedAnalysisQaEvent = analysisQaEventService.get("1");

        // Assertions
        assertNotNull(updatedAnalysisQaEvent);
        assertEquals(Date.valueOf("2023-10-05"), updatedAnalysisQaEvent.getCompletedDate());
    }

    @Test
    public void DeleteAnalysisQaEvent() {
        AnalysisQaEvent analysisQaEvent = analysisQaEventService.get("1");

        analysisQaEventService.delete(analysisQaEvent);
        AnalysisQaEvent deletedAnalysisQaEvent = analysisQaEventService.get("1");

        assertNull(deletedAnalysisQaEvent);
    }

    @Test
    public void GetAnalysisQaEventDisplayValue() {
        // Fetch the AnalysisQaEvent with ID 1
        AnalysisQaEvent analysisQaEvent = analysisQaEventService.get("1");

        // Get the display value
        String displayValue = analysisQaEvent.getAnalysisQaEventDisplayValue();

        // Assertions
        assertNotNull(displayValue);
        assertTrue(displayValue.contains("Test 1")); // Ensure the analysis name is included
        assertTrue(displayValue.contains("QA Event 1")); // Ensure the QA event name is included
    }
}