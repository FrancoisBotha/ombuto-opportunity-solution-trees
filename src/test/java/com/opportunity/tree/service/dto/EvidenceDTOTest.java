package com.opportunity.tree.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.opportunity.tree.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class EvidenceDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(EvidenceDTO.class);
        EvidenceDTO evidenceDTO1 = new EvidenceDTO();
        evidenceDTO1.setId(1L);
        EvidenceDTO evidenceDTO2 = new EvidenceDTO();
        assertThat(evidenceDTO1).isNotEqualTo(evidenceDTO2);
        evidenceDTO2.setId(evidenceDTO1.getId());
        assertThat(evidenceDTO1).isEqualTo(evidenceDTO2);
        evidenceDTO2.setId(2L);
        assertThat(evidenceDTO1).isNotEqualTo(evidenceDTO2);
        evidenceDTO1.setId(null);
        assertThat(evidenceDTO1).isNotEqualTo(evidenceDTO2);
    }
}
