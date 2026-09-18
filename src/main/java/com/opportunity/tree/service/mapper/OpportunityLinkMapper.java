package com.opportunity.tree.service.mapper;

import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.OpportunityLink;
import com.opportunity.tree.service.dto.OpportunityDTO;
import com.opportunity.tree.service.dto.OpportunityLinkDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link OpportunityLink} and its DTO {@link OpportunityLinkDTO}.
 */
@Mapper(componentModel = "spring")
public interface OpportunityLinkMapper extends EntityMapper<OpportunityLinkDTO, OpportunityLink> {
    @Mapping(target = "opportunity", source = "opportunity", qualifiedByName = "opportunityTitle")
    OpportunityLinkDTO toDto(OpportunityLink s);

    @Named("opportunityTitle")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    OpportunityDTO toDtoOpportunityTitle(Opportunity opportunity);
}
