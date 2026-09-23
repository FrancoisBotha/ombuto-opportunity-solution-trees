package com.opportunity.tree.repository;

import com.opportunity.tree.domain.Tag;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Tag entity.
 */
@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    default Optional<Tag> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Tag> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Tag> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(value = "select tag from Tag tag left join fetch tag.team", countQuery = "select count(tag) from Tag tag")
    Page<Tag> findAllWithToOneRelationships(Pageable pageable);

    @Query("select tag from Tag tag left join fetch tag.team")
    List<Tag> findAllWithToOneRelationships();

    @Query("select tag from Tag tag left join fetch tag.team where tag.id =:id")
    Optional<Tag> findOneWithToOneRelationships(@Param("id") Long id);

    /** LABEL-001: all tags for a single team, ordered by name. */
    @Query("select tag from Tag tag where tag.team.id = :teamId order by tag.name asc")
    List<Tag> findAllByTeamId(@Param("teamId") Long teamId);

    /** LABEL-001: all tags for a set of teams, ordered by team, then name. */
    @Query("select tag from Tag tag where tag.team.id in :teamIds order by tag.team.id, tag.name asc")
    List<Tag> findAllByTeamIdIn(@Param("teamIds") java.util.Collection<Long> teamIds);

    /** LABEL-001: exact normalized-name lookup within one team (for inline "create if missing"). */
    @Query("select tag from Tag tag where tag.team.id = :teamId and tag.normalizedName = :normalized")
    Optional<Tag> findByTeamIdAndNormalizedName(@Param("teamId") Long teamId, @Param("normalized") String normalized);

    /** LABEL-001: count of opportunity + solution associations referencing a tag. */
    @Query(
        "select (select count(o) from Opportunity o join o.tags t where t.id = :tagId) + " +
            "(select count(s) from Solution s join s.tags t where t.id = :tagId)"
    )
    long countAssociations(@Param("tagId") Long tagId);
}
