package org.openelisglobal.program;

import static org.junit.Assert.assertEquals;

import java.util.List;
import javax.sql.DataSource;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.internationalization.MessageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Finalizing a Pathology, Immunohistochemistry or Cytology case stores "see the
 * report" as the text result of its tests. The Immunohistochemistry key was
 * missing and the Cytology one misspelled, so the raw key was stored and shown
 * on the Results page; changeset 118 repairs the rows already stored.
 */
public class SeeReportResultTextTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DataSource dataSource;

    @Test
    public void eachProgramsSeeReportKey_resolvesToText() {
        assertEquals("See Pathology Report", MessageUtil.getMessage("result.pathology.seereport"));
        assertEquals("See Immunohistochemistry Report", MessageUtil.getMessage("result.immunochemistry.seereport"));
        assertEquals("See Cytology Report", MessageUtil.getMessage("result.cytology.seereport"));
    }

    @Test
    public void storedRawKeys_areRepaired() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertEquals(List.of("EXECUTED"), jdbc.queryForList(
                "SELECT exectype FROM databasechangelog WHERE id = 'repair-see-report-results'", String.class));
        assertEquals(Integer.valueOf(0),
                jdbc.queryForObject(
                        "SELECT count(*) FROM clinlims.result WHERE value IN"
                                + " ('result.immunochemistry.seereport', 'result.cytoology.seereport')",
                        Integer.class));
    }
}
