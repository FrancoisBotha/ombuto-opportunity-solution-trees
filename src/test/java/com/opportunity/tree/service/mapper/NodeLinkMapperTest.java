package com.opportunity.tree.service.mapper;

import static com.opportunity.tree.domain.NodeLinkAsserts.*;
import static com.opportunity.tree.domain.NodeLinkTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NodeLinkMapperTest {

    private NodeLinkMapper nodeLinkMapper;

    @BeforeEach
    void setUp() {
        nodeLinkMapper = new NodeLinkMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getNodeLinkSample1();
        var actual = nodeLinkMapper.toEntity(nodeLinkMapper.toDto(expected));
        assertNodeLinkAllPropertiesEquals(expected, actual);
    }
}
