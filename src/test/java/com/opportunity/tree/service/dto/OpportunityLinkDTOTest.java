package com.opportunity.tree.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.opportunity.tree.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class OpportunityLinkDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(OpportunityLinkDTO.class);
        OpportunityLinkDTO opportunityLinkDTO1 = new OpportunityLinkDTO();
        opportunityLinkDTO1.setId(1L);
        OpportunityLinkDTO opportunityLinkDTO2 = new OpportunityLinkDTO();
        assertThat(opportunityLinkDTO1).isNotEqualTo(opportunityLinkDTO2);
        opportunityLinkDTO2.setId(opportunityLinkDTO1.getId());
        assertThat(opportunityLinkDTO1).isEqualTo(opportunityLinkDTO2);
        opportunityLinkDTO2.setId(2L);
        assertThat(opportunityLinkDTO1).isNotEqualTo(opportunityLinkDTO2);
        opportunityLinkDTO1.setId(null);
        assertThat(opportunityLinkDTO1).isNotEqualTo(opportunityLinkDTO2);
    }
}
