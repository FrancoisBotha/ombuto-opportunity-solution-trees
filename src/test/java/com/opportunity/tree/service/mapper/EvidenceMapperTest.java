package com.opportunity.tree.service.mapper;

import static com.opportunity.tree.domain.EvidenceAsserts.*;
import static com.opportunity.tree.domain.EvidenceTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EvidenceMapperTest {

    private EvidenceMapper evidenceMapper;

    @BeforeEach
    void setUp() {
        evidenceMapper = new EvidenceMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getEvidenceSample1();
        var actual = evidenceMapper.toEntity(evidenceMapper.toDto(expected));
        assertEvidenceAllPropertiesEquals(expected, actual);
    }
}
