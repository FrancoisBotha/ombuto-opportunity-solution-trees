package com.opportunity.tree.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.opportunity.tree.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class OutcomeDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(OutcomeDTO.class);
        OutcomeDTO outcomeDTO1 = new OutcomeDTO();
        outcomeDTO1.setId(1L);
        OutcomeDTO outcomeDTO2 = new OutcomeDTO();
        assertThat(outcomeDTO1).isNotEqualTo(outcomeDTO2);
        outcomeDTO2.setId(outcomeDTO1.getId());
        assertThat(outcomeDTO1).isEqualTo(outcomeDTO2);
        outcomeDTO2.setId(2L);
        assertThat(outcomeDTO1).isNotEqualTo(outcomeDTO2);
        outcomeDTO1.setId(null);
        assertThat(outcomeDTO1).isNotEqualTo(outcomeDTO2);
    }
}
