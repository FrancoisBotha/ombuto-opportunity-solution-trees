package com.opportunity.tree.repository;

import com.opportunity.tree.domain.TeamMember;
import com.opportunity.tree.domain.enumeration.TeamRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the TeamMember entity.
 */
@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    @Query("select teamMember from TeamMember teamMember where teamMember.user.login = ?#{authentication.name}")
    List<TeamMember> findByUserIsCurrentUser();

    // Only DevDataSeederIT uses this since "my teams" became one projection query; kept on purpose.
    List<TeamMember> findAllByUserLogin(String login);

    /**
     * The "my teams" list of the user with {@code login} in ONE statement: rows of [teamId (Long),
     * name (String), description (String), createdDate (Instant), role (TeamRole), memberCount (Long),
     * productCount (Long)]. Reading the teams in the same statement as the memberships means a team
     * deleted concurrently simply drops out, instead of failing a later lazy Team load with an
     * ObjectNotFoundException (HTTP 500).
     */
    @Query(
        "select t.id, t.name, t.description, t.createdDate, m.role," +
            " (select count(m2) from TeamMember m2 where m2.team.id = t.id)," +
            " (select count(p) from Product p where p.team.id = t.id)" +
            " from TeamMember m join m.team t where m.user.login = :login order by t.id"
    )
    List<Object[]> findMyTeamRows(@Param("login") String login);

    List<TeamMember> findAllByTeamId(Long teamId);

    Optional<TeamMember> findOneByTeamIdAndUserId(Long teamId, String userId);

    boolean existsByTeamIdAndUserId(Long teamId, String userId);

    long countByTeamId(Long teamId);

    long countByTeamIdAndRole(Long teamId, TeamRole role);

    default Optional<TeamMember> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<TeamMember> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<TeamMember> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select teamMember from TeamMember teamMember left join fetch teamMember.team left join fetch teamMember.user",
        countQuery = "select count(teamMember) from TeamMember teamMember"
    )
    Page<TeamMember> findAllWithToOneRelationships(Pageable pageable);

    @Query("select teamMember from TeamMember teamMember left join fetch teamMember.team left join fetch teamMember.user")
    List<TeamMember> findAllWithToOneRelationships();

    @Query(
        "select teamMember from TeamMember teamMember left join fetch teamMember.team left join fetch teamMember.user where teamMember.id =:id"
    )
    Optional<TeamMember> findOneWithToOneRelationships(@Param("id") Long id);
}
