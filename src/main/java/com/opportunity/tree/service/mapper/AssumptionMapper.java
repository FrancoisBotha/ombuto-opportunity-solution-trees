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
 * Mapper for the entity {@link Assumption} and its DTO {@link AssumptionDTO}.
 */
@Mapper(componentModel = "spring")
public interface AssumptionMapper extends EntityMapper<AssumptionDTO, Assumption> {
    @Mapping(target = "solution", source = "solution", qualifiedByName = "solutionTitle")
    @Mapping(target = "experiments", source = "experiments", qualifiedByName = "experimentTitleSet")
    AssumptionDTO toDto(Assumption s);

    @Mapping(target = "experiments", ignore = true)
    @Mapping(target = "removeExperiment", ignore = true)
    Assumption toEntity(AssumptionDTO assumptionDTO);

    @Named("solutionTitle")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    SolutionDTO toDtoSolutionTitle(Solution solution);

    @Named("experimentTitle")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    ExperimentDTO toDtoExperimentTitle(Experiment experiment);

    @Named("experimentTitleSet")
    default Set<ExperimentDTO> toDtoExperimentTitleSet(Set<Experiment> experiment) {
        return experiment.stream().map(this::toDtoExperimentTitle).collect(Collectors.toSet());
    }
}
