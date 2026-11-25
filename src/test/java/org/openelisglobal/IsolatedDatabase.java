package org.openelisglobal;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a test class that starts its own PostgreSQL container and never touches
 * the shared test database that {@link BaseTestConfig} starts once per JVM.
 *
 * <p>
 * CI runs these classes in a shard of their own, several JVMs at a time, and
 * leaves them out of the shards that share a database. Every test class that
 * creates a {@code PostgreSQLContainer} must carry this annotation;
 * {@code .github/scripts/backend_test_shards.py check} fails the build when one
 * does not.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface IsolatedDatabase {
}
