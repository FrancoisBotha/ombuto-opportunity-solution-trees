package com.opportunity.tree.service.mapper;

import static com.opportunity.tree.domain.OpportunityAsserts.*;
import static com.opportunity.tree.domain.OpportunityTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OpportunityMapperTest {

    private OpportunityMapper opportunityMapper;

    @BeforeEach
    void setUp() {
        opportunityMapper = new OpportunityMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getOpportunitySample1();
        var actual = opportunityMapper.toEntity(opportunityMapper.toDto(expected));
        assertOpportunityAllPropertiesEquals(expected, actual);
    }
}
