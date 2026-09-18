package com.opportunity.tree.repository;

import com.opportunity.tree.domain.Experiment;
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
public class ExperimentRepositoryWithBagRelationshipsImpl implements ExperimentRepositoryWithBagRelationships {

    private static final String ID_PARAMETER = "id";
    private static final String EXPERIMENTS_PARAMETER = "experiments";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Experiment> fetchBagRelationships(Optional<Experiment> experiment) {
        return experiment.map(this::fetchAssumptions);
    }

    @Override
    public Page<Experiment> fetchBagRelationships(Page<Experiment> experiments) {
        return new PageImpl<>(fetchBagRelationships(experiments.getContent()), experiments.getPageable(), experiments.getTotalElements());
    }

    @Override
    public List<Experiment> fetchBagRelationships(List<Experiment> experiments) {
        return Optional.of(experiments).map(this::fetchAssumptions).orElse(Collections.emptyList());
    }

    Experiment fetchAssumptions(Experiment result) {
        return entityManager
            .createQuery(
                "select experiment from Experiment experiment left join fetch experiment.assumptions where experiment.id = :id",
                Experiment.class
            )
            .setParameter(ID_PARAMETER, result.getId())
            .getSingleResult();
    }

    List<Experiment> fetchAssumptions(List<Experiment> experiments) {
        HashMap<Object, Integer> order = new HashMap<>();
        IntStream.range(0, experiments.size()).forEach(index -> order.put(experiments.get(index).getId(), index));
        List<Experiment> result = entityManager
            .createQuery(
                "select experiment from Experiment experiment left join fetch experiment.assumptions where experiment in :experiments",
                Experiment.class
            )
            .setParameter(EXPERIMENTS_PARAMETER, experiments)
            .getResultList();
        Collections.sort(result, (o1, o2) -> Integer.compare(order.get(o1.getId()), order.get(o2.getId())));
        return result;
    }
}
