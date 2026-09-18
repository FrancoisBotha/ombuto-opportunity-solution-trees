package com.opportunity.tree.service.mapper;

import static com.opportunity.tree.domain.OpportunityLinkAsserts.*;
import static com.opportunity.tree.domain.OpportunityLinkTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OpportunityLinkMapperTest {

    private OpportunityLinkMapper opportunityLinkMapper;

    @BeforeEach
    void setUp() {
        opportunityLinkMapper = new OpportunityLinkMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getOpportunityLinkSample1();
        var actual = opportunityLinkMapper.toEntity(opportunityLinkMapper.toDto(expected));
        assertOpportunityLinkAllPropertiesEquals(expected, actual);
    }
}
