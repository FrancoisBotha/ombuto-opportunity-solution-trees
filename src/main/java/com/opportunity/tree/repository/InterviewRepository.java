package com.opportunity.tree.repository;

import com.opportunity.tree.domain.Interview;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Interview entity.
 */
@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long>, JpaSpecificationExecutor<Interview> {
    @Query("select interview from Interview interview where interview.interviewer.login = ?#{authentication.name}")
    List<Interview> findByInterviewerIsCurrentUser();

    default Optional<Interview> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Interview> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Interview> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select interview from Interview interview left join fetch interview.product left join fetch interview.interviewer",
        countQuery = "select count(interview) from Interview interview"
    )
    Page<Interview> findAllWithToOneRelationships(Pageable pageable);

    @Query("select interview from Interview interview left join fetch interview.product left join fetch interview.interviewer")
    List<Interview> findAllWithToOneRelationships();

    @Query(
        "select interview from Interview interview left join fetch interview.product left join fetch interview.interviewer where interview.id =:id"
    )
    Optional<Interview> findOneWithToOneRelationships(@Param("id") Long id);
}
