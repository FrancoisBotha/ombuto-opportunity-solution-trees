package com.opportunity.tree.domain;

import static com.opportunity.tree.domain.NodeHistoryTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.opportunity.tree.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class NodeHistoryTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(NodeHistory.class);
        NodeHistory nodeHistory1 = getNodeHistorySample1();
        NodeHistory nodeHistory2 = new NodeHistory();
        assertThat(nodeHistory1).isNotEqualTo(nodeHistory2);

        nodeHistory2.setId(nodeHistory1.getId());
        assertThat(nodeHistory1).isEqualTo(nodeHistory2);

        nodeHistory2 = getNodeHistorySample2();
        assertThat(nodeHistory1).isNotEqualTo(nodeHistory2);
    }
}
