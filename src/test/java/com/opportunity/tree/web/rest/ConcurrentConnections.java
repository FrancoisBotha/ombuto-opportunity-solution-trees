package com.opportunity.tree.web.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariDataSource;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.test.context.TestPropertySource;

/**
 * For integration tests that exercise real concurrency between requests. {@code testprod}
 * (Testcontainers PostgreSQL) caps the Hikari pool at one connection, which silently serialises
 * "concurrent" requests so a concurrency test passes even without any locking. This annotation
 * gives the test context its own pool of {@value #POOL_SIZE}; every class that uses it gets the
 * same property set and therefore shares one cached Spring context.
 *
 * <p>Tests must also call {@link Guard#assertRealConcurrency(DataSource)} before running, so that a
 * configuration change can never make them pass vacuously again.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@TestPropertySource(properties = "spring.datasource.hikari.maximum-pool-size=" + ConcurrentConnections.POOL_SIZE)
public @interface ConcurrentConnections {
    int POOL_SIZE = 8;

    final class Guard {

        private Guard() {}

        /** Fails unless the pool can really hand out several connections at once. */
        static void assertRealConcurrency(DataSource dataSource) throws SQLException {
            int max = dataSource.unwrap(HikariDataSource.class).getMaximumPoolSize();
            assertThat(max)
                .as("connection pool size — with fewer connections the concurrent requests run one by one")
                .isGreaterThanOrEqualTo(POOL_SIZE);
        }
    }
}
