package org.openelisglobal;

import jakarta.persistence.EntityManager;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.hibernate.Session;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Test-only {@link DataSource} that makes plain-JDBC access (the DBUnit fixture
 * loader, {@code JdbcTemplate}, and the {@code ensure*} seed helpers) share the
 * <em>same physical connection</em> as the current test transaction's JPA
 * {@link EntityManager}.
 *
 * <p>
 * Why this exists (#3711): the integration suite runs each test in a Spring
 * {@code @Transactional} rollback. The standard linchpin —
 * {@code JpaTransactionManager.setDataSource(...)} — is meant to make
 * {@link DataSourceUtils#getConnection(DataSource)} return the JPA connection,
 * but {@code HibernateJpaDialect.getJdbcConnection()} returns {@code null}
 * under Hibernate's default lazy connection acquisition, so nothing is bound
 * and every {@code DataSourceUtils.getConnection} call opens a
 * <em>separate</em> connection. That separate connection then deadlocks (20s
 * lock timeout) when its {@code TRUNCATE ... CASCADE} blocks against the test
 * transaction's own {@code ACCESS SHARE} read locks.
 *
 * <p>
 * This proxy fixes it deterministically, without depending on the Hibernate
 * dialect: when a test {@code EntityManager} is bound to the thread it reads
 * the connection directly off that EntityManager (forcing lazy acquisition);
 * the connection is returned close-suppressed because the EntityManager owns
 * its lifecycle. When no test transaction is active (e.g. the committed-fixture
 * base, or Liquibase at startup) it falls through to the real DataSource,
 * preserving a separate, committed connection.
 */
public class TransactionAwareTestDataSource extends DelegatingDataSource {

    public TransactionAwareTestDataSource(DataSource target) {
        super(target);
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection shared = currentTestTransactionConnection();
        return shared != null ? shared : super.getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection shared = currentTestTransactionConnection();
        return shared != null ? shared : super.getConnection(username, password);
    }

    /**
     * @return a close-suppressing handle to the bound test EntityManager's JDBC
     *         connection, or {@code null} when no test transaction is active.
     */
    private Connection currentTestTransactionConnection() {
        // Only share the test EM's connection when an ACTUAL test transaction is
        // active (the rollback base). The committed base (BaseCommittedFixtureTest)
        // runs
        // @Transactional(NOT_SUPPORTED) — no active tx — and manages its own committed
        // connection; it must get a real, independent connection, never the test EM's.
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            return null;
        }
        EntityManager em = boundTestEntityManager();
        if (em == null) {
            return null;
        }
        // doReturningWork forces lazy acquisition and hands back the physical
        // connection; Hibernate's default RELEASE_AFTER_TRANSACTION holds it for
        // the whole test transaction, so every caller gets the same one.
        Connection physical = em.unwrap(Session.class).doReturningWork(conn -> conn);
        return closeSuppressing(physical);
    }

    private EntityManager boundTestEntityManager() {
        for (Object resource : TransactionSynchronizationManager.getResourceMap().values()) {
            if (resource instanceof EntityManagerHolder) {
                return ((EntityManagerHolder) resource).getEntityManager();
            }
        }
        return null;
    }

    /**
     * The EntityManager owns the connection's lifecycle. The loader and
     * {@code JdbcTemplate} both call {@code DataSourceUtils.releaseConnection}
     * (which closes the handle) on the connection they obtain; suppressing
     * {@code close()} keeps the test transaction's connection alive until Spring
     * rolls it back.
     */
    private Connection closeSuppressing(Connection target) {
        return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { Connection.class },
                new CloseSuppressingHandler(target));
    }

    private static final class CloseSuppressingHandler implements InvocationHandler {
        private final Connection target;

        CloseSuppressingHandler(Connection target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            switch (method.getName()) {
            case "close":
                return null; // no-op: the EntityManager owns the connection
            case "isClosed":
                // Delegate to the real connection: close() is suppressed, so this
                // normally reports false for the life of the test transaction, but it
                // stays honest if the underlying connection is ever closed externally.
                return target.isClosed();
            case "equals":
                return proxy == args[0];
            case "hashCode":
                return System.identityHashCode(proxy);
            case "unwrap":
                // let callers (and instrumentation) reach the real connection
                if (args != null && args.length == 1 && args[0] instanceof Class
                        && ((Class<?>) args[0]).isInstance(target)) {
                    return target;
                }
                break;
            default:
                break;
            }
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        }
    }
}
