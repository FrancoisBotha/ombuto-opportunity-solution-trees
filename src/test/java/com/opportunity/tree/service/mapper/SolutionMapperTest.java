package com.opportunity.tree.service.mapper;

import static com.opportunity.tree.domain.SolutionAsserts.*;
import static com.opportunity.tree.domain.SolutionTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SolutionMapperTest {

    private SolutionMapper solutionMapper;

    @BeforeEach
    void setUp() {
        solutionMapper = new SolutionMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getSolutionSample1();
        var actual = solutionMapper.toEntity(solutionMapper.toDto(expected));
        assertSolutionAllPropertiesEquals(expected, actual);
    }
}
