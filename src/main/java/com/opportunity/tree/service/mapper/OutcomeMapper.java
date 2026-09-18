package com.opportunity.tree.service.mapper;

import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.service.dto.OutcomeDTO;
import com.opportunity.tree.service.dto.ProductDTO;
import com.opportunity.tree.service.dto.UserDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Outcome} and its DTO {@link OutcomeDTO}.
 */
@Mapper(componentModel = "spring")
public interface OutcomeMapper extends EntityMapper<OutcomeDTO, Outcome> {
    @Mapping(target = "product", source = "product", qualifiedByName = "productName")
    @Mapping(target = "owner", source = "owner", qualifiedByName = "userLogin")
    OutcomeDTO toDto(Outcome s);

    @Named("productName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    ProductDTO toDtoProductName(Product product);

    @Named("userLogin")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "login", source = "login")
    UserDTO toDtoUserLogin(User user);
}
