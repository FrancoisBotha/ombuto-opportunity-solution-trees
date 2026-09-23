package com.opportunity.tree.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.opportunity.tree.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class MeetingTranscriptDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(MeetingTranscriptDTO.class);
        MeetingTranscriptDTO meetingTranscriptDTO1 = new MeetingTranscriptDTO();
        meetingTranscriptDTO1.setId(1L);
        MeetingTranscriptDTO meetingTranscriptDTO2 = new MeetingTranscriptDTO();
        assertThat(meetingTranscriptDTO1).isNotEqualTo(meetingTranscriptDTO2);
        meetingTranscriptDTO2.setId(meetingTranscriptDTO1.getId());
        assertThat(meetingTranscriptDTO1).isEqualTo(meetingTranscriptDTO2);
        meetingTranscriptDTO2.setId(2L);
        assertThat(meetingTranscriptDTO1).isNotEqualTo(meetingTranscriptDTO2);
        meetingTranscriptDTO1.setId(null);
        assertThat(meetingTranscriptDTO1).isNotEqualTo(meetingTranscriptDTO2);
    }
}
