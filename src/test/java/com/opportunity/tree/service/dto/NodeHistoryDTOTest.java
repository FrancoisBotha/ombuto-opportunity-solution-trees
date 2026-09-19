package com.opportunity.tree.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.opportunity.tree.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class NodeHistoryDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(NodeHistoryDTO.class);
        NodeHistoryDTO nodeHistoryDTO1 = new NodeHistoryDTO();
        nodeHistoryDTO1.setId(1L);
        NodeHistoryDTO nodeHistoryDTO2 = new NodeHistoryDTO();
        assertThat(nodeHistoryDTO1).isNotEqualTo(nodeHistoryDTO2);
        nodeHistoryDTO2.setId(nodeHistoryDTO1.getId());
        assertThat(nodeHistoryDTO1).isEqualTo(nodeHistoryDTO2);
        nodeHistoryDTO2.setId(2L);
        assertThat(nodeHistoryDTO1).isNotEqualTo(nodeHistoryDTO2);
        nodeHistoryDTO1.setId(null);
        assertThat(nodeHistoryDTO1).isNotEqualTo(nodeHistoryDTO2);
    }
}
