package com.opportunity.tree.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.opportunity.tree.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class AssumptionDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(AssumptionDTO.class);
        AssumptionDTO assumptionDTO1 = new AssumptionDTO();
        assumptionDTO1.setId(1L);
        AssumptionDTO assumptionDTO2 = new AssumptionDTO();
        assertThat(assumptionDTO1).isNotEqualTo(assumptionDTO2);
        assumptionDTO2.setId(assumptionDTO1.getId());
        assertThat(assumptionDTO1).isEqualTo(assumptionDTO2);
        assumptionDTO2.setId(2L);
        assertThat(assumptionDTO1).isNotEqualTo(assumptionDTO2);
        assumptionDTO1.setId(null);
        assertThat(assumptionDTO1).isNotEqualTo(assumptionDTO2);
    }
}
