package com.opportunity.tree.service.mapper;

import static com.opportunity.tree.domain.MeetingTranscriptAsserts.*;
import static com.opportunity.tree.domain.MeetingTranscriptTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MeetingTranscriptMapperTest {

    private MeetingTranscriptMapper meetingTranscriptMapper;

    @BeforeEach
    void setUp() {
        meetingTranscriptMapper = new MeetingTranscriptMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getMeetingTranscriptSample1();
        var actual = meetingTranscriptMapper.toEntity(meetingTranscriptMapper.toDto(expected));
        assertMeetingTranscriptAllPropertiesEquals(expected, actual);
    }
}
