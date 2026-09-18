package com.opportunity.tree.service.mapper;

import static com.opportunity.tree.domain.AssumptionAsserts.*;
import static com.opportunity.tree.domain.AssumptionTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AssumptionMapperTest {

    private AssumptionMapper assumptionMapper;

    @BeforeEach
    void setUp() {
        assumptionMapper = new AssumptionMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getAssumptionSample1();
        var actual = assumptionMapper.toEntity(assumptionMapper.toDto(expected));
        assertAssumptionAllPropertiesEquals(expected, actual);
    }
}
