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
}
