package com.opportunity.tree.repository;

import com.opportunity.tree.domain.Solution;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;

public interface SolutionRepositoryWithBagRelationships {
    Optional<Solution> fetchBagRelationships(Optional<Solution> solution);

    List<Solution> fetchBagRelationships(List<Solution> solutions);

    Page<Solution> fetchBagRelationships(Page<Solution> solutions);
}
