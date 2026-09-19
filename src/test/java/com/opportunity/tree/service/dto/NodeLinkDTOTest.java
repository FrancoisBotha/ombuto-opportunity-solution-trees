package com.opportunity.tree.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.opportunity.tree.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class NodeLinkDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(NodeLinkDTO.class);
        NodeLinkDTO nodeLinkDTO1 = new NodeLinkDTO();
        nodeLinkDTO1.setId(1L);
        NodeLinkDTO nodeLinkDTO2 = new NodeLinkDTO();
        assertThat(nodeLinkDTO1).isNotEqualTo(nodeLinkDTO2);
        nodeLinkDTO2.setId(nodeLinkDTO1.getId());
        assertThat(nodeLinkDTO1).isEqualTo(nodeLinkDTO2);
        nodeLinkDTO2.setId(2L);
        assertThat(nodeLinkDTO1).isNotEqualTo(nodeLinkDTO2);
        nodeLinkDTO1.setId(null);
        assertThat(nodeLinkDTO1).isNotEqualTo(nodeLinkDTO2);
    }
}
