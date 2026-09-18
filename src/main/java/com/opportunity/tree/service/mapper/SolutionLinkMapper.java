package com.opportunity.tree.service.mapper;

import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.SolutionLink;
import com.opportunity.tree.service.dto.SolutionDTO;
import com.opportunity.tree.service.dto.SolutionLinkDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link SolutionLink} and its DTO {@link SolutionLinkDTO}.
 */
@Mapper(componentModel = "spring")
public interface SolutionLinkMapper extends EntityMapper<SolutionLinkDTO, SolutionLink> {
    @Mapping(target = "solution", source = "solution", qualifiedByName = "solutionTitle")
    SolutionLinkDTO toDto(SolutionLink s);

    @Named("solutionTitle")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    SolutionDTO toDtoSolutionTitle(Solution solution);
}
