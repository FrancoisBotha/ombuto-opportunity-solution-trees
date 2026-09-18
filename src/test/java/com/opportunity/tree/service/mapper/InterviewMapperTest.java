package com.opportunity.tree.service.mapper;

import static com.opportunity.tree.domain.InterviewAsserts.*;
import static com.opportunity.tree.domain.InterviewTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InterviewMapperTest {

    private InterviewMapper interviewMapper;

    @BeforeEach
    void setUp() {
        interviewMapper = new InterviewMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getInterviewSample1();
        var actual = interviewMapper.toEntity(interviewMapper.toDto(expected));
        assertInterviewAllPropertiesEquals(expected, actual);
    }
}
