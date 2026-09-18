package com.opportunity.tree.service.mapper;

import com.opportunity.tree.domain.Assumption;
import com.opportunity.tree.domain.Experiment;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.service.dto.AssumptionDTO;
import com.opportunity.tree.service.dto.ExperimentDTO;
import com.opportunity.tree.service.dto.SolutionDTO;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Experiment} and its DTO {@link ExperimentDTO}.
 */
@Mapper(componentModel = "spring")
public interface ExperimentMapper extends EntityMapper<ExperimentDTO, Experiment> {
    @Mapping(target = "solution", source = "solution", qualifiedByName = "solutionTitle")
    @Mapping(target = "assumptions", source = "assumptions", qualifiedByName = "assumptionStatementSet")
    ExperimentDTO toDto(Experiment s);

    @Mapping(target = "removeAssumption", ignore = true)
    Experiment toEntity(ExperimentDTO experimentDTO);

    @Named("solutionTitle")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    SolutionDTO toDtoSolutionTitle(Solution solution);

    @Named("assumptionStatement")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "statement", source = "statement")
    AssumptionDTO toDtoAssumptionStatement(Assumption assumption);

    @Named("assumptionStatementSet")
    default Set<AssumptionDTO> toDtoAssumptionStatementSet(Set<Assumption> assumption) {
        return assumption.stream().map(this::toDtoAssumptionStatement).collect(Collectors.toSet());
    }
}
