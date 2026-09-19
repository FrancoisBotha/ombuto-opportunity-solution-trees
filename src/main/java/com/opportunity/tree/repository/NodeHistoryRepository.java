package com.opportunity.tree.repository;

import com.opportunity.tree.domain.NodeHistory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the NodeHistory entity.
 */
@Repository
public interface NodeHistoryRepository extends JpaRepository<NodeHistory, Long> {
    @Query("select nodeHistory from NodeHistory nodeHistory where nodeHistory.author.login = ?#{authentication.name}")
    List<NodeHistory> findByAuthorIsCurrentUser();

    default Optional<NodeHistory> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<NodeHistory> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<NodeHistory> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select nodeHistory from NodeHistory nodeHistory left join fetch nodeHistory.author",
        countQuery = "select count(nodeHistory) from NodeHistory nodeHistory"
    )
    Page<NodeHistory> findAllWithToOneRelationships(Pageable pageable);

    @Query("select nodeHistory from NodeHistory nodeHistory left join fetch nodeHistory.author")
    List<NodeHistory> findAllWithToOneRelationships();

    @Query("select nodeHistory from NodeHistory nodeHistory left join fetch nodeHistory.author where nodeHistory.id =:id")
    Optional<NodeHistory> findOneWithToOneRelationships(@Param("id") Long id);
}
