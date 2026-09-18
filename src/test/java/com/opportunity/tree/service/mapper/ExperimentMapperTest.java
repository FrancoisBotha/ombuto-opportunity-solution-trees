package com.opportunity.tree.service.mapper;

import static com.opportunity.tree.domain.ExperimentAsserts.*;
import static com.opportunity.tree.domain.ExperimentTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExperimentMapperTest {

    private ExperimentMapper experimentMapper;

    @BeforeEach
    void setUp() {
        experimentMapper = new ExperimentMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getExperimentSample1();
        var actual = experimentMapper.toEntity(experimentMapper.toDto(expected));
        assertExperimentAllPropertiesEquals(expected, actual);
    }
}
