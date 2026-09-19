package com.opportunity.tree.repository;

import com.opportunity.tree.domain.OpenQuestion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the OpenQuestion entity.
 */
@Repository
public interface OpenQuestionRepository extends JpaRepository<OpenQuestion, Long> {
    default Optional<OpenQuestion> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<OpenQuestion> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<OpenQuestion> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select openQuestion from OpenQuestion openQuestion left join fetch openQuestion.opportunity",
        countQuery = "select count(openQuestion) from OpenQuestion openQuestion"
    )
    Page<OpenQuestion> findAllWithToOneRelationships(Pageable pageable);

    @Query("select openQuestion from OpenQuestion openQuestion left join fetch openQuestion.opportunity")
    List<OpenQuestion> findAllWithToOneRelationships();

    @Query("select openQuestion from OpenQuestion openQuestion left join fetch openQuestion.opportunity where openQuestion.id =:id")
    Optional<OpenQuestion> findOneWithToOneRelationships(@Param("id") Long id);
}
