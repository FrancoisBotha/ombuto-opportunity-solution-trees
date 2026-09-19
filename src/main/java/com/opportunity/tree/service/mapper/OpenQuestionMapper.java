package com.opportunity.tree.service.mapper;

import com.opportunity.tree.domain.OpenQuestion;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.service.dto.OpenQuestionDTO;
import com.opportunity.tree.service.dto.OpportunityDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link OpenQuestion} and its DTO {@link OpenQuestionDTO}.
 */
@Mapper(componentModel = "spring")
public interface OpenQuestionMapper extends EntityMapper<OpenQuestionDTO, OpenQuestion> {
    @Mapping(target = "opportunity", source = "opportunity", qualifiedByName = "opportunityTitle")
    OpenQuestionDTO toDto(OpenQuestion s);

    @Named("opportunityTitle")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    OpportunityDTO toDtoOpportunityTitle(Opportunity opportunity);
}
