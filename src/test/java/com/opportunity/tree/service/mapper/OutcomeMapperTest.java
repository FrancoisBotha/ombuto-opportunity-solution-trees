package com.opportunity.tree.service.mapper;

import static com.opportunity.tree.domain.OutcomeAsserts.*;
import static com.opportunity.tree.domain.OutcomeTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OutcomeMapperTest {

    private OutcomeMapper outcomeMapper;

    @BeforeEach
    void setUp() {
        outcomeMapper = new OutcomeMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getOutcomeSample1();
        var actual = outcomeMapper.toEntity(outcomeMapper.toDto(expected));
        assertOutcomeAllPropertiesEquals(expected, actual);
    }
}
