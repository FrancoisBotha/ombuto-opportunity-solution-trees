package com.opportunity.tree.service.mapper;

import com.opportunity.tree.domain.Interview;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.service.dto.InterviewDTO;
import com.opportunity.tree.service.dto.OpportunityDTO;
import com.opportunity.tree.service.dto.ProductDTO;
import com.opportunity.tree.service.dto.UserDTO;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Interview} and its DTO {@link InterviewDTO}.
 */
@Mapper(componentModel = "spring")
public interface InterviewMapper extends EntityMapper<InterviewDTO, Interview> {
    @Mapping(target = "product", source = "product", qualifiedByName = "productName")
    @Mapping(target = "interviewer", source = "interviewer", qualifiedByName = "userLogin")
    @Mapping(target = "opportunities", source = "opportunities", qualifiedByName = "opportunityTitleSet")
    InterviewDTO toDto(Interview s);

    @Mapping(target = "opportunities", ignore = true)
    @Mapping(target = "removeOpportunity", ignore = true)
    Interview toEntity(InterviewDTO interviewDTO);

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

    @Named("opportunityTitle")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    OpportunityDTO toDtoOpportunityTitle(Opportunity opportunity);

    @Named("opportunityTitleSet")
    default Set<OpportunityDTO> toDtoOpportunityTitleSet(Set<Opportunity> opportunity) {
        return opportunity.stream().map(this::toDtoOpportunityTitle).collect(Collectors.toSet());
    }
}
