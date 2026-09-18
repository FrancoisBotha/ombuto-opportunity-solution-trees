package com.opportunity.tree.service.mapper;

import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.TeamMember;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.service.dto.TeamDTO;
import com.opportunity.tree.service.dto.TeamMemberDTO;
import com.opportunity.tree.service.dto.UserDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link TeamMember} and its DTO {@link TeamMemberDTO}.
 */
@Mapper(componentModel = "spring")
public interface TeamMemberMapper extends EntityMapper<TeamMemberDTO, TeamMember> {
    @Mapping(target = "team", source = "team", qualifiedByName = "teamName")
    @Mapping(target = "user", source = "user", qualifiedByName = "userLogin")
    TeamMemberDTO toDto(TeamMember s);

    @Named("teamName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    TeamDTO toDtoTeamName(Team team);

    @Named("userLogin")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "login", source = "login")
    UserDTO toDtoUserLogin(User user);
}
