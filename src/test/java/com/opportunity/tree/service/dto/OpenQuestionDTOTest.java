package com.opportunity.tree.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.opportunity.tree.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class OpenQuestionDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(OpenQuestionDTO.class);
        OpenQuestionDTO openQuestionDTO1 = new OpenQuestionDTO();
        openQuestionDTO1.setId(1L);
        OpenQuestionDTO openQuestionDTO2 = new OpenQuestionDTO();
        assertThat(openQuestionDTO1).isNotEqualTo(openQuestionDTO2);
        openQuestionDTO2.setId(openQuestionDTO1.getId());
        assertThat(openQuestionDTO1).isEqualTo(openQuestionDTO2);
        openQuestionDTO2.setId(2L);
        assertThat(openQuestionDTO1).isNotEqualTo(openQuestionDTO2);
        openQuestionDTO1.setId(null);
        assertThat(openQuestionDTO1).isNotEqualTo(openQuestionDTO2);
    }
}
