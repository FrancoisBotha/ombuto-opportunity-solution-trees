package com.opportunity.tree.service.mapper;

import static com.opportunity.tree.domain.NodeHistoryAsserts.*;
import static com.opportunity.tree.domain.NodeHistoryTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NodeHistoryMapperTest {

    private NodeHistoryMapper nodeHistoryMapper;

    @BeforeEach
    void setUp() {
        nodeHistoryMapper = new NodeHistoryMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getNodeHistorySample1();
        var actual = nodeHistoryMapper.toEntity(nodeHistoryMapper.toDto(expected));
        assertNodeHistoryAllPropertiesEquals(expected, actual);
    }
}
