package com.opportunity.tree.repository;

import com.opportunity.tree.domain.Opportunity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;

public interface OpportunityRepositoryWithBagRelationships {
    Optional<Opportunity> fetchBagRelationships(Optional<Opportunity> opportunity);

    List<Opportunity> fetchBagRelationships(List<Opportunity> opportunities);

    Page<Opportunity> fetchBagRelationships(Page<Opportunity> opportunities);
}
