package com.opportunity.tree.web.rest;

import java.util.Locale;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * Test-only Hibernate {@link StatementInspector} that lets an integration test run code at an exact
 * point inside a request: once armed for a thread, the first SQL statement of that thread that
 * comes AFTER a statement mentioning {@code marker} runs the action first (and waits for it). Every
 * other thread, and the thread once the action ran, is untouched.
 *
 * <p>Registered through {@code spring.jpa.properties.hibernate.session_factory.statement_inspector}
 * by the tests that use it (Hibernate instantiates it reflectively, hence the static state).
 */
public class StatementTrap implements StatementInspector {

    private static volatile Thread armedThread;
    private static volatile String marker;
    private static volatile Runnable action;
    private static volatile boolean markerSeen;
    private static volatile boolean fired;

    /** Arms the trap for the calling thread. */
    static void arm(String sqlMarker, Runnable then) {
        marker = sqlMarker.toLowerCase(Locale.ROOT);
        action = then;
        markerSeen = false;
        fired = false;
        armedThread = Thread.currentThread();
    }

    static void disarm() {
        armedThread = null;
        action = null;
        marker = null;
    }

    /** Did the action run? (Guards against a test passing because the trap never went off.) */
    static boolean fired() {
        return fired;
    }

    @Override
    public String inspect(String sql) {
        if (Thread.currentThread() != armedThread || action == null) {
            return sql;
        }
        if (markerSeen) {
            Runnable run = action;
            action = null;
            fired = true;
            run.run();
        } else if (sql.toLowerCase(Locale.ROOT).contains(marker)) {
            markerSeen = true;
        }
        return sql;
    }
}
