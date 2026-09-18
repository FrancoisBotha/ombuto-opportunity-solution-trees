package com.opportunity.tree.repository;

import com.opportunity.tree.domain.Opportunity;
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
public class OpportunityRepositoryWithBagRelationshipsImpl implements OpportunityRepositoryWithBagRelationships {

    private static final String ID_PARAMETER = "id";
    private static final String OPPORTUNITIES_PARAMETER = "opportunities";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Opportunity> fetchBagRelationships(Optional<Opportunity> opportunity) {
        return opportunity.map(this::fetchInterviews).map(this::fetchTags);
    }

    @Override
    public Page<Opportunity> fetchBagRelationships(Page<Opportunity> opportunities) {
        return new PageImpl<>(
            fetchBagRelationships(opportunities.getContent()),
            opportunities.getPageable(),
            opportunities.getTotalElements()
        );
    }

    @Override
    public List<Opportunity> fetchBagRelationships(List<Opportunity> opportunities) {
        return Optional.of(opportunities).map(this::fetchInterviews).map(this::fetchTags).orElse(Collections.emptyList());
    }

    Opportunity fetchInterviews(Opportunity result) {
        return entityManager
            .createQuery(
                "select opportunity from Opportunity opportunity left join fetch opportunity.interviews where opportunity.id = :id",
                Opportunity.class
            )
            .setParameter(ID_PARAMETER, result.getId())
            .getSingleResult();
    }

    List<Opportunity> fetchInterviews(List<Opportunity> opportunities) {
        HashMap<Object, Integer> order = new HashMap<>();
        IntStream.range(0, opportunities.size()).forEach(index -> order.put(opportunities.get(index).getId(), index));
        List<Opportunity> result = entityManager
            .createQuery(
                "select opportunity from Opportunity opportunity left join fetch opportunity.interviews where opportunity in :opportunities",
                Opportunity.class
            )
            .setParameter(OPPORTUNITIES_PARAMETER, opportunities)
            .getResultList();
        Collections.sort(result, (o1, o2) -> Integer.compare(order.get(o1.getId()), order.get(o2.getId())));
        return result;
    }

    Opportunity fetchTags(Opportunity result) {
        return entityManager
            .createQuery(
                "select opportunity from Opportunity opportunity left join fetch opportunity.tags where opportunity.id = :id",
                Opportunity.class
            )
            .setParameter(ID_PARAMETER, result.getId())
            .getSingleResult();
    }

    List<Opportunity> fetchTags(List<Opportunity> opportunities) {
        HashMap<Object, Integer> order = new HashMap<>();
        IntStream.range(0, opportunities.size()).forEach(index -> order.put(opportunities.get(index).getId(), index));
        List<Opportunity> result = entityManager
            .createQuery(
                "select opportunity from Opportunity opportunity left join fetch opportunity.tags where opportunity in :opportunities",
                Opportunity.class
            )
            .setParameter(OPPORTUNITIES_PARAMETER, opportunities)
            .getResultList();
        Collections.sort(result, (o1, o2) -> Integer.compare(order.get(o1.getId()), order.get(o2.getId())));
        return result;
    }
}
