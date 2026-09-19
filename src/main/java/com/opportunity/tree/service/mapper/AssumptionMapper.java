package com.opportunity.tree.service.mapper;

import com.opportunity.tree.domain.Assumption;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.service.dto.AssumptionDTO;
import com.opportunity.tree.service.dto.SolutionDTO;
import com.opportunity.tree.service.dto.UserDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Assumption} and its DTO {@link AssumptionDTO}.
 */
@Mapper(componentModel = "spring")
public interface AssumptionMapper extends EntityMapper<AssumptionDTO, Assumption> {
    @Mapping(target = "solution", source = "solution", qualifiedByName = "solutionTitle")
    @Mapping(target = "owner", source = "owner", qualifiedByName = "userLogin")
    AssumptionDTO toDto(Assumption s);

    @Named("solutionTitle")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    SolutionDTO toDtoSolutionTitle(Solution solution);

    @Named("userLogin")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "login", source = "login")
    UserDTO toDtoUserLogin(User user);
}
