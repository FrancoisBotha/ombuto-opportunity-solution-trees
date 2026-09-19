package com.opportunity.tree.service.mapper;

import static com.opportunity.tree.domain.OpenQuestionAsserts.*;
import static com.opportunity.tree.domain.OpenQuestionTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OpenQuestionMapperTest {

    private OpenQuestionMapper openQuestionMapper;

    @BeforeEach
    void setUp() {
        openQuestionMapper = new OpenQuestionMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getOpenQuestionSample1();
        var actual = openQuestionMapper.toEntity(openQuestionMapper.toDto(expected));
        assertOpenQuestionAllPropertiesEquals(expected, actual);
    }
}
