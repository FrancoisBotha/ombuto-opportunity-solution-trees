package com.opportunity.tree.service.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Assumption;
import com.opportunity.tree.domain.Comment;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.TeamMember;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.AssumptionStatus;
import com.opportunity.tree.domain.enumeration.OpportunityStatus;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.service.TeamAccessDeniedException;
import com.opportunity.tree.service.mcp.dto.CommentEntry;
import com.opportunity.tree.service.mcp.dto.CommentsPage;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

/**
 * MCPSRV-011: integration tests for the {@code list_node_comments} MCP tool.
 *
 * <p>Covers the four criteria the ticket calls out: a non-member is refused, a viewer (team
 * member without edit rights) sees the thread, a node with no comments returns an empty page
 * with total 0, and a thread longer than one page paginates correctly.
 */
@IntegrationTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ListNodeCommentsToolIT {

    @Autowired
    private EntityManager em;

    @Autowired
    private ListNodeCommentsTool tool;

    private Team teamA;
    private User owner;
    private User viewer;
    private User outsider;
    private Product productA;
    private Outcome outcomeA;
    private Opportunity opportunityA;
    private Opportunity opportunityNoComments;
    private Assumption assumptionA;

    @BeforeEach
    void seed() {
        String suffix = "-" + UUID.randomUUID().toString().substring(0, 8);
        teamA = persistTeam("teamA" + suffix);
        Team teamB = persistTeam("teamB" + suffix);
        owner = persistUser("owner" + suffix);
        viewer = persistUser("viewer" + suffix);
        outsider = persistUser("outsider" + suffix);
        persistMembership(teamA, owner, TeamRole.OWNER);
        persistMembership(teamA, viewer, TeamRole.VIEWER);
        persistMembership(teamB, outsider, TeamRole.OWNER);

        productA = persistProduct(teamA, "Alpha");
        outcomeA = new Outcome().title("Grow WAU").description("d").sortOrder(0).createdDate(Instant.now()).product(productA);
        em.persist(outcomeA);
        opportunityA = new Opportunity()
            .title("Onboarding is confusing")
            .description("d")
            .status(OpportunityStatus.UNEXPLORED)
            .valuerating(4)
            .priority(50)
            .sortOrder(0)
            .createdDate(Instant.now())
            .outcome(outcomeA);
        em.persist(opportunityA);
        opportunityNoComments = new Opportunity()
            .title("Silent one")
            .description("d")
            .status(OpportunityStatus.UNEXPLORED)
            .valuerating(1)
            .priority(1)
            .sortOrder(1)
            .createdDate(Instant.now())
            .outcome(outcomeA);
        em.persist(opportunityNoComments);
        assumptionA = new Assumption()
            .statement("Users will complete the tour")
            .description("d")
            .status(AssumptionStatus.UNTESTED)
            .confidence(40)
            .sortOrder(0)
            .createdDate(Instant.now())
            .solution(null);
        // Assumption requires a solution — link one for referential integrity of the test.
        com.opportunity.tree.domain.Solution sol = new com.opportunity.tree.domain.Solution()
            .title("Interactive tour")
            .description("d")
            .status(com.opportunity.tree.domain.enumeration.SolutionStatus.CANDIDATE)
            .sortOrder(0)
            .createdDate(Instant.now())
            .opportunity(opportunityA);
        em.persist(sol);
        assumptionA.setSolution(sol);
        em.persist(assumptionA);

        em.flush();
        em.clear();
    }

    // ---------------------------------------------------------------------
    // Criterion 3: non-member is refused with no existence leak
    // ---------------------------------------------------------------------

    @Test
    void nonMemberIsRefusedForACommentedNode() {
        seedComments(opportunityA, owner, 3);
        authenticate(outsider);
        assertThatThrownBy(() -> tool.listNodeComments("OPPORTUNITY", opportunityA.getId(), null, null)).isInstanceOf(
            TeamAccessDeniedException.class
        );
    }

    @Test
    void nonMemberRefusalIsIndistinguishableFromMissingId() {
        seedComments(opportunityA, owner, 1);
        authenticate(outsider);
        Throwable crossTeam = org.assertj.core.api.Assertions.catchThrowable(() ->
            tool.listNodeComments("OPPORTUNITY", opportunityA.getId(), null, null)
        );
        Throwable missing = org.assertj.core.api.Assertions.catchThrowable(() ->
            tool.listNodeComments("OPPORTUNITY", 987_654_321L, null, null)
        );
        assertThat(crossTeam).isInstanceOf(TeamAccessDeniedException.class);
        assertThat(missing).isInstanceOf(TeamAccessDeniedException.class);
        assertThat(crossTeam.getMessage()).isEqualTo(missing.getMessage());
    }

    // ---------------------------------------------------------------------
    // Criterion 1 + 3: viewer (team member) gets body, author and times, oldest first
    // ---------------------------------------------------------------------

    @Test
    void viewerReceivesCommentBodyAuthorAndTimesOldestFirst() {
        seedComments(opportunityA, owner, 3);
        authenticate(viewer);

        CommentsPage page = tool.listNodeComments("OPPORTUNITY", opportunityA.getId(), null, null);

        assertThat(page.nodeType()).isEqualTo("OPPORTUNITY");
        assertThat(page.nodeId()).isEqualTo(opportunityA.getId());
        assertThat(page.totalMatching()).isEqualTo(3L);
        assertThat(page.comments()).hasSize(3);
        for (CommentEntry c : page.comments()) {
            assertThat(c.body()).isNotBlank();
            assertThat(c.author()).isEqualTo(owner.getLogin());
            assertThat(c.createdDate()).isNotNull();
        }
        // Oldest first: sequential createdDate — the seed uses ascending timestamps.
        assertThat(page.comments()).isSortedAccordingTo(java.util.Comparator.comparing(CommentEntry::createdDate));
    }

    @Test
    void editedCommentReportsEditedDate() {
        Comment c = seedComments(opportunityA, owner, 1).get(0);
        c.setEditedDate(c.getCreatedDate().plusSeconds(60));
        em.merge(c);
        em.flush();
        em.clear();

        authenticate(viewer);
        CommentsPage page = tool.listNodeComments("OPPORTUNITY", opportunityA.getId(), null, null);
        assertThat(page.comments()).hasSize(1);
        assertThat(page.comments().get(0).editedDate()).isNotNull();
    }

    // ---------------------------------------------------------------------
    // Criterion 7: node with no comments returns an empty page with total 0
    // ---------------------------------------------------------------------

    @Test
    void nodeWithNoCommentsReturnsEmptyPageWithZeroTotal() {
        authenticate(viewer);

        CommentsPage page = tool.listNodeComments("OPPORTUNITY", opportunityNoComments.getId(), null, null);

        assertThat(page.totalMatching()).isEqualTo(0L);
        assertThat(page.comments()).isEmpty();
        assertThat(page.limit()).isEqualTo(ListNodeCommentsTool.MAX_PAGE_SIZE);
    }

    // ---------------------------------------------------------------------
    // Criterion 2 + 7: thread longer than one page paginates
    // ---------------------------------------------------------------------

    @Test
    void longThreadPaginatesAndReportsTotalMessageCount() {
        seedComments(opportunityA, owner, 7);
        authenticate(viewer);

        CommentsPage first = tool.listNodeComments("OPPORTUNITY", opportunityA.getId(), 0, 3);
        assertThat(first.page()).isEqualTo(0);
        assertThat(first.size()).isEqualTo(3);
        assertThat(first.totalMatching()).isEqualTo(7L);
        assertThat(first.comments()).hasSize(3);

        CommentsPage second = tool.listNodeComments("OPPORTUNITY", opportunityA.getId(), 1, 3);
        assertThat(second.page()).isEqualTo(1);
        assertThat(second.comments()).hasSize(3);

        CommentsPage third = tool.listNodeComments("OPPORTUNITY", opportunityA.getId(), 2, 3);
        assertThat(third.comments()).hasSize(1);
        assertThat(third.totalMatching()).isEqualTo(7L);

        // No id appears on two pages.
        java.util.Set<Long> firstIds = first.comments().stream().map(CommentEntry::id).collect(java.util.stream.Collectors.toSet());
        java.util.Set<Long> secondIds = second.comments().stream().map(CommentEntry::id).collect(java.util.stream.Collectors.toSet());
        assertThat(java.util.Collections.disjoint(firstIds, secondIds)).isTrue();
    }

    @Test
    void pageSizeIsCappedToMax() {
        seedComments(opportunityA, owner, 2);
        authenticate(viewer);
        CommentsPage page = tool.listNodeComments("OPPORTUNITY", opportunityA.getId(), 0, 5000);
        assertThat(page.size()).isEqualTo(ListNodeCommentsTool.MAX_PAGE_SIZE);
        assertThat(page.limit()).isEqualTo(ListNodeCommentsTool.MAX_PAGE_SIZE);
    }

    // ---------------------------------------------------------------------
    // Validation
    // ---------------------------------------------------------------------

    @Test
    void productTypeIsRejectedBecauseProductsHaveNoDiscussion() {
        authenticate(owner);
        assertThatThrownBy(() -> tool.listNodeComments("PRODUCT", productA.getId(), null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("PRODUCT");
    }

    @Test
    void unknownTypeIsAValidationError() {
        authenticate(owner);
        assertThatThrownBy(() -> tool.listNodeComments("BOGUS", 1L, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("BOGUS");
    }

    @Test
    void worksForAssumptionAndEvidenceThreadsToo() {
        seedCommentsOnAssumption(assumptionA, owner, 2);
        authenticate(viewer);
        CommentsPage page = tool.listNodeComments("ASSUMPTION", assumptionA.getId(), null, null);
        assertThat(page.totalMatching()).isEqualTo(2L);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private java.util.List<Comment> seedComments(Opportunity op, User author, int count) {
        java.util.List<Comment> created = new java.util.ArrayList<>();
        Instant base = Instant.parse("2026-09-01T10:00:00Z");
        for (int i = 0; i < count; i++) {
            Comment c = new Comment();
            c.setBody("body " + i);
            c.setCreatedDate(base.plusSeconds(i * 60L));
            c.setAuthor(author);
            c.setOpportunity(op);
            em.persist(c);
            created.add(c);
        }
        em.flush();
        return created;
    }

    private void seedCommentsOnAssumption(Assumption a, User author, int count) {
        Instant base = Instant.parse("2026-09-02T10:00:00Z");
        for (int i = 0; i < count; i++) {
            Comment c = new Comment();
            c.setBody("assumption body " + i);
            c.setCreatedDate(base.plusSeconds(i * 60L));
            c.setAuthor(author);
            c.setAssumption(a);
            em.persist(c);
        }
        em.flush();
    }

    private Team persistTeam(String name) {
        Team t = new Team().name(name).description("d").createdDate(Instant.now());
        em.persist(t);
        return t;
    }

    private User persistUser(String login) {
        User u = new User();
        u.setId(UUID.randomUUID().toString());
        u.setLogin(login);
        u.setActivated(true);
        u.setEmail(login + "@example.com");
        u.setFirstName(login);
        u.setLastName("test");
        u.setLangKey("en");
        em.persist(u);
        return u;
    }

    private void persistMembership(Team t, User u, TeamRole role) {
        TeamMember tm = new TeamMember().role(role).joinedDate(Instant.now());
        tm.setTeam(t);
        tm.setUser(u);
        em.persist(tm);
    }

    private Product persistProduct(Team team, String name) {
        Product p = new Product().name(name).description("d").archived(false).sortOrder(0).createdDate(Instant.now()).team(team);
        em.persist(p);
        return p;
    }

    private void authenticate(User user) {
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(new UsernamePasswordAuthenticationToken(user.getLogin(), "n/a"));
        SecurityContextHolder.setContext(ctx);
    }
}
