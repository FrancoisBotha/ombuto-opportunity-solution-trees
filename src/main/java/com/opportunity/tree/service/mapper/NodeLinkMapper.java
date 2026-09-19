package com.opportunity.tree.service.mapper;

import com.opportunity.tree.domain.Assumption;
import com.opportunity.tree.domain.Evidence;
import com.opportunity.tree.domain.NodeLink;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.service.dto.AssumptionDTO;
import com.opportunity.tree.service.dto.EvidenceDTO;
import com.opportunity.tree.service.dto.NodeLinkDTO;
import com.opportunity.tree.service.dto.OpportunityDTO;
import com.opportunity.tree.service.dto.OutcomeDTO;
import com.opportunity.tree.service.dto.ProductDTO;
import com.opportunity.tree.service.dto.SolutionDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link NodeLink} and its DTO {@link NodeLinkDTO}.
 */
@Mapper(componentModel = "spring")
public interface NodeLinkMapper extends EntityMapper<NodeLinkDTO, NodeLink> {
    @Mapping(target = "product", source = "product", qualifiedByName = "productName")
    @Mapping(target = "outcome", source = "outcome", qualifiedByName = "outcomeTitle")
    @Mapping(target = "opportunity", source = "opportunity", qualifiedByName = "opportunityTitle")
    @Mapping(target = "solution", source = "solution", qualifiedByName = "solutionTitle")
    @Mapping(target = "assumption", source = "assumption", qualifiedByName = "assumptionStatement")
    @Mapping(target = "evidence", source = "evidence", qualifiedByName = "evidenceTitle")
    NodeLinkDTO toDto(NodeLink s);

    @Named("productName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    ProductDTO toDtoProductName(Product product);

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

    @Named("evidenceTitle")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    EvidenceDTO toDtoEvidenceTitle(Evidence evidence);
}
