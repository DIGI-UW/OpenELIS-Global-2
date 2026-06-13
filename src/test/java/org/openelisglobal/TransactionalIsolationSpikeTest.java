package org.openelisglobal;

import static org.junit.Assert.assertEquals;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spike proving the connection-unification linchpin for issue #3711
 * (integration-test isolation). It validates that, after wiring
 * {@code emf.setDataSource} + {@code transactionManager.setDataSource} in
 * {@link BaseTestConfig}:
 *
 * <ol>
 * <li>a DBUnit-style raw-JDBC write via
 * {@code DataSourceUtils.getConnection(dataSource)} lands on the SAME physical
 * connection the JPA/Hibernate session uses (so fixtures are visible to
 * services-under-test without committing), and</li>
 * <li>Spring rolls that write back after each test method (so no pollution
 * survives).</li>
 * </ol>
 *
 * If both pass, the full transactional-rollback migration described in the plan
 * is sound. This class intentionally does NOT extend
 * {@code BaseWebContextSensitiveTest} (which still carries the
 * {@code @Transactional(NOT_SUPPORTED)} opt-out); it exercises the target model
 * in isolation. It can be deleted once the base class itself is converted.
 */
@Transactional
@ContextConfiguration(classes = { BaseTestConfig.class, AppTestConfig.class })
@WebAppConfiguration
@TestPropertySource("classpath:common.properties")
@ActiveProfiles("test")
public class TransactionalIsolationSpikeTest extends AbstractTransactionalJUnit4SpringContextTests {

    private static final int PROBE_ID = 987654;

    @Autowired
    private DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Insert via the DBUnit-style JDBC path, then read via Hibernate. The EM can
     * only see the row if it shares the connection with
     * {@code DataSourceUtils.getConnection(dataSource)} — i.e. the linchpin wiring
     * works.
     */
    @Test
    public void jdbcInsertIsVisibleToHibernateSameConnection() throws Exception {
        assertEquals("probe row must not pre-exist", 0,
                countRowsInTableWhere("clinlims.system_user", "id = " + PROBE_ID));

        Connection conn = DataSourceUtils.getConnection(dataSource);
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("INSERT INTO clinlims.system_user (id, external_id, login_name, last_name,"
                    + " first_name, initials, is_active, is_employee, lastupdated) VALUES (" + PROBE_ID + ", '"
                    + PROBE_ID + "', 'spikeUser', 'Spike', 'Test', 'ST', 'Y', 'Y', now())");
        }

        Number seenByHibernate = (Number) entityManager
                .createNativeQuery("SELECT count(*) FROM clinlims.system_user WHERE id = " + PROBE_ID)
                .getSingleResult();
        assertEquals("Hibernate must observe the JDBC-inserted row — proves shared connection", 1,
                seenByHibernate.intValue());
    }

    /**
     * Separate test method: the probe row inserted by the other test must be
     * absent, proving Spring rolled it back at the previous method boundary (no
     * cross-test pollution).
     */
    @Test
    public void rollbackRevertsBetweenTests() {
        assertEquals("probe row must be absent at the start of each test — proves rollback", 0,
                countRowsInTableWhere("clinlims.system_user", "id = " + PROBE_ID));
    }
}
