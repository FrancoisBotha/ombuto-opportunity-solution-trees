package com.opportunity.tree.repository;

import com.opportunity.tree.domain.MeetingTranscript;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the MeetingTranscript entity.
 */
@Repository
public interface MeetingTranscriptRepository extends JpaRepository<MeetingTranscript, Long>, JpaSpecificationExecutor<MeetingTranscript> {
    @Query(
        "select meetingTranscript from MeetingTranscript meetingTranscript where meetingTranscript.author.login = ?#{authentication.name}"
    )
    List<MeetingTranscript> findByAuthorIsCurrentUser();

    default Optional<MeetingTranscript> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<MeetingTranscript> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<MeetingTranscript> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select meetingTranscript from MeetingTranscript meetingTranscript left join fetch meetingTranscript.author left join fetch meetingTranscript.product left join fetch meetingTranscript.outcome left join fetch meetingTranscript.opportunity left join fetch meetingTranscript.solution left join fetch meetingTranscript.assumption left join fetch meetingTranscript.evidence",
        countQuery = "select count(meetingTranscript) from MeetingTranscript meetingTranscript"
    )
    Page<MeetingTranscript> findAllWithToOneRelationships(Pageable pageable);

    @Query(
        "select meetingTranscript from MeetingTranscript meetingTranscript left join fetch meetingTranscript.author left join fetch meetingTranscript.product left join fetch meetingTranscript.outcome left join fetch meetingTranscript.opportunity left join fetch meetingTranscript.solution left join fetch meetingTranscript.assumption left join fetch meetingTranscript.evidence"
    )
    List<MeetingTranscript> findAllWithToOneRelationships();

    @Query(
        "select meetingTranscript from MeetingTranscript meetingTranscript left join fetch meetingTranscript.author left join fetch meetingTranscript.product left join fetch meetingTranscript.outcome left join fetch meetingTranscript.opportunity left join fetch meetingTranscript.solution left join fetch meetingTranscript.assumption left join fetch meetingTranscript.evidence where meetingTranscript.id =:id"
    )
    Optional<MeetingTranscript> findOneWithToOneRelationships(@Param("id") Long id);

    // --- Team-scoped list queries (hand-written; not regenerated) ---

    /**
     * Every transcript hanging off any node in the given team. Author is fetched eagerly; the body
     * is left in the entity but the caller must not expose it in list responses.
     *
     * <p>Uses explicit LEFT JOINs so a transcript whose evidence hangs off an assumption still
     * matches on the assumption path (and vice versa) — matching the shape of
     * {@code TreeAccessLookupRepository#findTeamIdOfEvidence}.
     */
    @Query(
        value = "select mt from MeetingTranscript mt" +
            " left join fetch mt.author" +
            " left join mt.product mp" +
            " left join mt.outcome moc left join moc.product mocp" +
            " left join mt.opportunity mop left join mop.outcome mopc left join mopc.product mopcp" +
            " left join mt.solution ms left join ms.opportunity mso left join mso.outcome msoc left join msoc.product msocp" +
            " left join mt.assumption ma left join ma.solution mas left join mas.opportunity maso left join maso.outcome masoc left join masoc.product masocp" +
            " left join mt.evidence me" +
            " left join me.opportunity meo left join meo.outcome meoc left join meoc.product meocp" +
            " left join me.assumption mea left join mea.solution meas left join meas.opportunity measo left join measo.outcome measoc left join measoc.product measocp" +
            " where mp.team.id = :teamId" +
            " or mocp.team.id = :teamId" +
            " or mopcp.team.id = :teamId" +
            " or msocp.team.id = :teamId" +
            " or masocp.team.id = :teamId" +
            " or meocp.team.id = :teamId" +
            " or measocp.team.id = :teamId",
        countQuery = "select count(mt) from MeetingTranscript mt" +
            " left join mt.product mp" +
            " left join mt.outcome moc left join moc.product mocp" +
            " left join mt.opportunity mop left join mop.outcome mopc left join mopc.product mopcp" +
            " left join mt.solution ms left join ms.opportunity mso left join mso.outcome msoc left join msoc.product msocp" +
            " left join mt.assumption ma left join ma.solution mas left join mas.opportunity maso left join maso.outcome masoc left join masoc.product masocp" +
            " left join mt.evidence me" +
            " left join me.opportunity meo left join meo.outcome meoc left join meoc.product meocp" +
            " left join me.assumption mea left join mea.solution meas left join meas.opportunity measo left join measo.outcome measoc left join measoc.product measocp" +
            " where mp.team.id = :teamId" +
            " or mocp.team.id = :teamId" +
            " or mopcp.team.id = :teamId" +
            " or msocp.team.id = :teamId" +
            " or masocp.team.id = :teamId" +
            " or meocp.team.id = :teamId" +
            " or measocp.team.id = :teamId"
    )
    Page<MeetingTranscript> findAllByTeamId(@Param("teamId") Long teamId, Pageable pageable);

    @Query("select t from MeetingTranscript t left join fetch t.author where t.product.id = :nodeId")
    Page<MeetingTranscript> findAllByProductId(@Param("nodeId") Long nodeId, Pageable pageable);

    @Query("select t from MeetingTranscript t left join fetch t.author where t.outcome.id = :nodeId")
    Page<MeetingTranscript> findAllByOutcomeId(@Param("nodeId") Long nodeId, Pageable pageable);

    @Query("select t from MeetingTranscript t left join fetch t.author where t.opportunity.id = :nodeId")
    Page<MeetingTranscript> findAllByOpportunityId(@Param("nodeId") Long nodeId, Pageable pageable);

    @Query("select t from MeetingTranscript t left join fetch t.author where t.solution.id = :nodeId")
    Page<MeetingTranscript> findAllBySolutionId(@Param("nodeId") Long nodeId, Pageable pageable);

    @Query("select t from MeetingTranscript t left join fetch t.author where t.assumption.id = :nodeId")
    Page<MeetingTranscript> findAllByAssumptionId(@Param("nodeId") Long nodeId, Pageable pageable);

    @Query("select t from MeetingTranscript t left join fetch t.author where t.evidence.id = :nodeId")
    Page<MeetingTranscript> findAllByEvidenceId(@Param("nodeId") Long nodeId, Pageable pageable);
}
