package com.opportunity.tree.service.mapper;

import com.opportunity.tree.domain.NodeHistory;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.service.dto.NodeHistoryDTO;
import com.opportunity.tree.service.dto.UserDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link NodeHistory} and its DTO {@link NodeHistoryDTO}.
 */
@Mapper(componentModel = "spring")
public interface NodeHistoryMapper extends EntityMapper<NodeHistoryDTO, NodeHistory> {
    @Mapping(target = "author", source = "author", qualifiedByName = "userLogin")
    NodeHistoryDTO toDto(NodeHistory s);

    @Named("userLogin")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "login", source = "login")
    UserDTO toDtoUserLogin(User user);
}
