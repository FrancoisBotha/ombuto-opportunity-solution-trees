package com.opportunity.tree.service.mapper;

import com.opportunity.tree.domain.Assumption;
import com.opportunity.tree.domain.Evidence;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.service.dto.AssumptionDTO;
import com.opportunity.tree.service.dto.EvidenceDTO;
import com.opportunity.tree.service.dto.OpportunityDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Evidence} and its DTO {@link EvidenceDTO}.
 */
@Mapper(componentModel = "spring")
public interface EvidenceMapper extends EntityMapper<EvidenceDTO, Evidence> {
    @Mapping(target = "opportunity", source = "opportunity", qualifiedByName = "opportunityTitle")
    @Mapping(target = "assumption", source = "assumption", qualifiedByName = "assumptionStatement")
    EvidenceDTO toDto(Evidence s);

    @Named("opportunityTitle")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    OpportunityDTO toDtoOpportunityTitle(Opportunity opportunity);

    @Named("assumptionStatement")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "statement", source = "statement")
    AssumptionDTO toDtoAssumptionStatement(Assumption assumption);
}
