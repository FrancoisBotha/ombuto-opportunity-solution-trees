package com.opportunity.tree.service.mapper;

import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.Tag;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.service.dto.OpportunityDTO;
import com.opportunity.tree.service.dto.SolutionDTO;
import com.opportunity.tree.service.dto.TagDTO;
import com.opportunity.tree.service.dto.TeamDTO;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Tag} and its DTO {@link TagDTO}.
 */
@Mapper(componentModel = "spring")
public interface TagMapper extends EntityMapper<TagDTO, Tag> {
    @Mapping(target = "team", source = "team", qualifiedByName = "teamName")
    @Mapping(target = "opportunities", source = "opportunities", qualifiedByName = "opportunityTitleSet")
    @Mapping(target = "solutions", source = "solutions", qualifiedByName = "solutionTitleSet")
    TagDTO toDto(Tag s);

    @Mapping(target = "opportunities", ignore = true)
    @Mapping(target = "removeOpportunity", ignore = true)
    @Mapping(target = "solutions", ignore = true)
    @Mapping(target = "removeSolution", ignore = true)
    Tag toEntity(TagDTO tagDTO);

    @Named("teamName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    TeamDTO toDtoTeamName(Team team);

    @Named("opportunityTitle")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    OpportunityDTO toDtoOpportunityTitle(Opportunity opportunity);

    @Named("opportunityTitleSet")
    default Set<OpportunityDTO> toDtoOpportunityTitleSet(Set<Opportunity> opportunity) {
        return opportunity.stream().map(this::toDtoOpportunityTitle).collect(Collectors.toSet());
    }

    @Named("solutionTitle")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    SolutionDTO toDtoSolutionTitle(Solution solution);

    @Named("solutionTitleSet")
    default Set<SolutionDTO> toDtoSolutionTitleSet(Set<Solution> solution) {
        return solution.stream().map(this::toDtoSolutionTitle).collect(Collectors.toSet());
    }
}
