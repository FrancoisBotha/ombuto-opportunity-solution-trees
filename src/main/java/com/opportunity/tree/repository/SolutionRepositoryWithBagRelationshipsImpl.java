package com.opportunity.tree.repository;

import com.opportunity.tree.domain.Solution;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

/**
 * Utility repository to load bag relationships based on https://vladmihalcea.com/hibernate-multiplebagfetchexception/
 */
public class SolutionRepositoryWithBagRelationshipsImpl implements SolutionRepositoryWithBagRelationships {

    private static final String ID_PARAMETER = "id";
    private static final String SOLUTIONS_PARAMETER = "solutions";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Solution> fetchBagRelationships(Optional<Solution> solution) {
        return solution.map(this::fetchTags);
    }

    @Override
    public Page<Solution> fetchBagRelationships(Page<Solution> solutions) {
        return new PageImpl<>(fetchBagRelationships(solutions.getContent()), solutions.getPageable(), solutions.getTotalElements());
    }

    @Override
    public List<Solution> fetchBagRelationships(List<Solution> solutions) {
        return Optional.of(solutions).map(this::fetchTags).orElse(Collections.emptyList());
    }

    Solution fetchTags(Solution result) {
        return entityManager
            .createQuery("select solution from Solution solution left join fetch solution.tags where solution.id = :id", Solution.class)
            .setParameter(ID_PARAMETER, result.getId())
            .getSingleResult();
    }

    List<Solution> fetchTags(List<Solution> solutions) {
        HashMap<Object, Integer> order = new HashMap<>();
        IntStream.range(0, solutions.size()).forEach(index -> order.put(solutions.get(index).getId(), index));
        List<Solution> result = entityManager
            .createQuery(
                "select solution from Solution solution left join fetch solution.tags where solution in :solutions",
                Solution.class
            )
            .setParameter(SOLUTIONS_PARAMETER, solutions)
            .getResultList();
        Collections.sort(result, (o1, o2) -> Integer.compare(order.get(o1.getId()), order.get(o2.getId())));
        return result;
    }
}
