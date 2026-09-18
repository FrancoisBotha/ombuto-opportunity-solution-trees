package com.opportunity.tree.service.mapper;

import com.opportunity.tree.domain.Interview;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Tag;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.service.dto.InterviewDTO;
import com.opportunity.tree.service.dto.OpportunityDTO;
import com.opportunity.tree.service.dto.OutcomeDTO;
import com.opportunity.tree.service.dto.TagDTO;
import com.opportunity.tree.service.dto.UserDTO;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Opportunity} and its DTO {@link OpportunityDTO}.
 */
@Mapper(componentModel = "spring")
public interface OpportunityMapper extends EntityMapper<OpportunityDTO, Opportunity> {
    @Mapping(target = "outcome", source = "outcome", qualifiedByName = "outcomeTitle")
    @Mapping(target = "parent", source = "parent", qualifiedByName = "opportunityTitle")
    @Mapping(target = "owner", source = "owner", qualifiedByName = "userLogin")
    @Mapping(target = "interviews", source = "interviews", qualifiedByName = "interviewTitleSet")
    @Mapping(target = "tags", source = "tags", qualifiedByName = "tagNameSet")
    OpportunityDTO toDto(Opportunity s);

    @Mapping(target = "removeInterview", ignore = true)
    @Mapping(target = "removeTag", ignore = true)
    Opportunity toEntity(OpportunityDTO opportunityDTO);

    @Named("outcomeTitle")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    OutcomeDTO toDtoOutcomeTitle(Outcome outcome);

    @Named("opportunityTitle")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    OpportunityDTO toDtoOpportunityTitle(Opportunity opportunity);

    @Named("userLogin")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "login", source = "login")
    UserDTO toDtoUserLogin(User user);

    @Named("interviewTitle")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    InterviewDTO toDtoInterviewTitle(Interview interview);

    @Named("interviewTitleSet")
    default Set<InterviewDTO> toDtoInterviewTitleSet(Set<Interview> interview) {
        return interview.stream().map(this::toDtoInterviewTitle).collect(Collectors.toSet());
    }

    @Named("tagName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    TagDTO toDtoTagName(Tag tag);

    @Named("tagNameSet")
    default Set<TagDTO> toDtoTagNameSet(Set<Tag> tag) {
        return tag.stream().map(this::toDtoTagName).collect(Collectors.toSet());
    }
}
