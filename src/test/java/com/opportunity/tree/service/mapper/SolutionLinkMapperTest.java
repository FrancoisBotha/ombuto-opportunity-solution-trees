package com.opportunity.tree.service.mapper;

import static com.opportunity.tree.domain.SolutionLinkAsserts.*;
import static com.opportunity.tree.domain.SolutionLinkTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SolutionLinkMapperTest {

    private SolutionLinkMapper solutionLinkMapper;

    @BeforeEach
    void setUp() {
        solutionLinkMapper = new SolutionLinkMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getSolutionLinkSample1();
        var actual = solutionLinkMapper.toEntity(solutionLinkMapper.toDto(expected));
        assertSolutionLinkAllPropertiesEquals(expected, actual);
    }
}
