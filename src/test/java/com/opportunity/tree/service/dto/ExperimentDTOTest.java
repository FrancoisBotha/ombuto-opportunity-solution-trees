package com.opportunity.tree.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.opportunity.tree.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ExperimentDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ExperimentDTO.class);
        ExperimentDTO experimentDTO1 = new ExperimentDTO();
        experimentDTO1.setId(1L);
        ExperimentDTO experimentDTO2 = new ExperimentDTO();
        assertThat(experimentDTO1).isNotEqualTo(experimentDTO2);
        experimentDTO2.setId(experimentDTO1.getId());
        assertThat(experimentDTO1).isEqualTo(experimentDTO2);
        experimentDTO2.setId(2L);
        assertThat(experimentDTO1).isNotEqualTo(experimentDTO2);
        experimentDTO1.setId(null);
        assertThat(experimentDTO1).isNotEqualTo(experimentDTO2);
    }
}
