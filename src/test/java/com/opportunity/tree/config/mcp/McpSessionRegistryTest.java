package com.opportunity.tree.config.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Ownership semantics of the MCP session binding (MCPSRV-002). */
class McpSessionRegistryTest {

    private final McpSessionRegistry registry = new McpSessionRegistry();

    @Test
    void aBoundSessionIsOwnedOnlyByThePrincipalThatOpenedIt() {
        String session = UUID.randomUUID().toString();
        registry.bind(session, "alice");

        assertThat(registry.isOwnedBy(session, "alice")).isTrue();
        assertThat(registry.isOwnedBy(session, "bob")).isFalse();
        assertThat(registry.isOwnedBy(session, null)).isFalse();
    }

    @Test
    void anUnknownOrReleasedSessionIsOwnedByNobody() {
        String session = UUID.randomUUID().toString();
        assertThat(registry.isOwnedBy(session, "alice")).isFalse();

        registry.bind(session, "alice");
        registry.unbind(session);

        assertThat(registry.isOwnedBy(session, "alice")).isFalse();
        assertThat(registry.size()).isZero();
    }

    @Test
    void releasingLeavesNoEntryBehind() {
        for (int i = 0; i < 50; i++) {
            String session = UUID.randomUUID().toString();
            registry.bind(session, "alice-" + i);
            registry.unbind(session);
        }
        assertThat(registry.size()).isZero();
    }

    @Test
    void bindingIsRefusedOnceTheRegistryIsFull() {
        for (int i = 0; i < McpSessionRegistry.MAX_SESSIONS; i++) {
            registry.bind(UUID.randomUUID().toString(), "alice");
        }
        String overflow = UUID.randomUUID().toString();
        registry.bind(overflow, "alice");

        assertThat(registry.size()).isEqualTo(McpSessionRegistry.MAX_SESSIONS);
        assertThat(registry.isOwnedBy(overflow, "alice")).isFalse();
    }
}
