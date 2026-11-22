package org.openelisglobal.accreditation;

import java.time.LocalDate;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.accreditation.service.AccreditingBodyService;
import org.openelisglobal.accreditation.service.TestAccreditationService;
import org.openelisglobal.accreditation.valueholder.AccreditingBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Shared ground for the OGC-686 accreditation integration tests: the two tests
 * the {@code accreditation.xml} fixture supplies, the services under test, and
 * an empty pair of accreditation tables at both ends of every test. The fixture
 * declares no bodies or enrollments, so each test writes exactly the portfolio
 * it is about and none of them see each other's.
 */
public abstract class AccreditationIntegrationTestBase extends BaseWebContextSensitiveTest {

    protected static final String TEST_GLUCOSE = "9101";
    protected static final String TEST_SODIUM = "9102";
    protected static final String USER = "1";

    @Autowired
    protected AccreditingBodyService accreditingBodyService;

    @Autowired
    protected TestAccreditationService testAccreditationService;

    @Autowired
    private DataSource dataSource;

    protected JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        executeDataSetWithStateManagement("testdata/accreditation.xml");
        clean();
    }

    @After
    public void clearAccreditationRows() {
        clean();
    }

    /**
     * Subclasses that seed further tables extend this rather than adding their own
     * {@code @After}, so setup and teardown stay the same statement.
     */
    protected void clean() {
        jdbc.update("DELETE FROM clinlims.test_accreditation");
        jdbc.update("DELETE FROM clinlims.accrediting_body");
    }

    protected AccreditingBody body(String code, String name, LocalDate expiresOn) {
        AccreditingBody b = new AccreditingBody();
        b.setCode(code);
        b.setName(name);
        b.setExpiresOn(expiresOn);
        return b;
    }
}
