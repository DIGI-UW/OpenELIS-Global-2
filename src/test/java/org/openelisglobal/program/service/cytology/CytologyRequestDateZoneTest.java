package org.openelisglobal.program.service.cytology;

import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.TimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.services.TableIdService;
import org.openelisglobal.program.valueholder.cytology.CytologyCaseViewDisplayItem;
import org.openelisglobal.program.valueholder.cytology.CytologyDisplayItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The cytology dashboard row and case header print the day the examination was
 * requested as the JSON layer writes it. The fixture stamps that day at local
 * midnight, which a UTC formatter reads as the day before anywhere east of
 * Greenwich.
 */
public class CytologyRequestDateZoneTest extends BaseWebContextSensitiveTest {

    private static final int CASE_ENTERED_ON_THE_THIRD = 1;

    @Autowired
    private CytologyDisplayService cytologyDisplayService;

    private TimeZone runnersTimeZone;

    private String runnersReferringOrgTypeId;

    private String runnersReferringOrgDepartmentTypeId;

    @Before
    public void init() throws Exception {
        // Set before the fixture is written, so the dataset and every read of it
        // share one zone. The zone is east of Greenwich on purpose: a UTC runner
        // would otherwise serialize the same string either way.
        runnersTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Africa/Nairobi"));
        executeDataSetWithStateManagement("testdata/cytology-display-service.xml");
        authenticateAs("technician1");
        runnersReferringOrgTypeId = TableIdService.getInstance().REFERRING_ORG_TYPE_ID;
        runnersReferringOrgDepartmentTypeId = TableIdService.getInstance().REFERRING_ORG_DEPARTMENT_TYPE_ID;
        TableIdService.getInstance().REFERRING_ORG_TYPE_ID = "5";
        TableIdService.getInstance().REFERRING_ORG_DEPARTMENT_TYPE_ID = "6";
    }

    @After
    public void restoreTimeZone() {
        TableIdService.getInstance().REFERRING_ORG_TYPE_ID = runnersReferringOrgTypeId;
        TableIdService.getInstance().REFERRING_ORG_DEPARTMENT_TYPE_ID = runnersReferringOrgDepartmentTypeId;
        TimeZone.setDefault(runnersTimeZone);
    }

    @Test
    public void dashboardRow_serialisesTheRequestDateAsTheServersCalendarDay() throws Exception {
        CytologyDisplayItem item = cytologyDisplayService.convertToDisplayItem(CASE_ENTERED_ON_THE_THIRD);

        String json = new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(item);

        assertTrue("the dashboard prints the server's own day, not UTC's: " + json,
                json.contains("\"requestDate\":\"2024-06-03\""));
    }

    @Test
    public void caseView_serialisesTheRequestDateAsTheServersCalendarDay() throws Exception {
        CytologyCaseViewDisplayItem item = cytologyDisplayService.convertToCaseDisplayItem(CASE_ENTERED_ON_THE_THIRD);
        // A FHIR questionnaire needs the application's own serializer, and the day
        // under test does not depend on it.
        item.setProgramQuestionnaire(null);
        item.setProgramQuestionnaireResponse(null);

        String json = new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(item);

        assertTrue("the case header prints the server's own day, not UTC's: " + json,
                json.contains("\"requestDate\":\"2024-06-03\""));
    }
}
