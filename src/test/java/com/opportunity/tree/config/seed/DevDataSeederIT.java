package com.opportunity.tree.config.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.config.ApplicationProperties;
import com.opportunity.tree.domain.Authority;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.TeamMember;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.HistoryEventType;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.repository.AssumptionRepository;
import com.opportunity.tree.repository.AuthorityRepository;
import com.opportunity.tree.repository.CommentRepository;
import com.opportunity.tree.repository.EvidenceRepository;
import com.opportunity.tree.repository.NodeHistoryRepository;
import com.opportunity.tree.repository.NodeLinkRepository;
import com.opportunity.tree.repository.OpenQuestionRepository;
import com.opportunity.tree.repository.OpportunityRepository;
import com.opportunity.tree.repository.OutcomeRepository;
import com.opportunity.tree.repository.ProductRepository;
import com.opportunity.tree.repository.SolutionRepository;
import com.opportunity.tree.repository.TeamMemberRepository;
import com.opportunity.tree.repository.TeamRepository;
import com.opportunity.tree.repository.UserRepository;
import com.opportunity.tree.security.AuthoritiesConstants;
import jakarta.persistence.EntityManager;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

/**
 * OST step 2: {@link DevDataSeeder} against the test database. The seeder bean only exists under the
 * {@code dev} profile, so the test builds one by hand with the seed property switched on. Every test
 * is transactional, so the seeded rows are rolled back and do not leak into other ITs.
 */
@IntegrationTest
@Transactional
class DevDataSeederIT {

    /** Node counts in the prototype's SEED array ({@code Ombuto OST.dc.html}). */
    private static final Map<String, Long> PROTOTYPE_COUNTS = Map.of(
        "Product",
        2L,
        "Outcome",
        2L,
        "Opportunity",
        6L,
        "Solution",
        5L,
        "Assumption",
        6L,
        "Evidence",
        3L
    );

    @Autowired
    private EntityManager em;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OutcomeRepository outcomeRepository;

    @Autowired
    private OpportunityRepository opportunityRepository;

    @Autowired
    private SolutionRepository solutionRepository;

    @Autowired
    private AssumptionRepository assumptionRepository;

    @Autowired
    private EvidenceRepository evidenceRepository;

    @Autowired
    private NodeLinkRepository nodeLinkRepository;

    @Autowired
    private OpenQuestionRepository openQuestionRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private NodeHistoryRepository nodeHistoryRepository;

    private DevDataSeeder seeder(boolean enabled) {
        ApplicationProperties properties = new ApplicationProperties();
        properties.getSeed().setEnabled(enabled);
        return new DevDataSeeder(
            properties,
            transactionManager,
            jdbcTemplate,
            userRepository,
            authorityRepository,
            teamRepository,
            teamMemberRepository,
            productRepository,
            outcomeRepository,
            opportunityRepository,
            solutionRepository,
            assumptionRepository,
            evidenceRepository,
            nodeLinkRepository,
            openQuestionRepository,
            commentRepository,
            nodeHistoryRepository
        );
    }

    private long count(String jpql) {
        return em.createQuery(jpql, Long.class).setParameter("team", DevDataSeeder.TEAM_JUPITER).getSingleResult();
    }

    private long nodeCount(String entity) {
        return switch (entity) {
            case "Product" -> count("select count(p) from Product p where p.team.name = :team");
            case "Outcome" -> count("select count(o) from Outcome o where o.product.team.name = :team");
            case "Opportunity" -> count("select count(o) from Opportunity o where o.outcome.product.team.name = :team");
            case "Solution" -> count("select count(s) from Solution s where s.opportunity.outcome.product.team.name = :team");
            case "Assumption" -> count("select count(a) from Assumption a where a.solution.opportunity.outcome.product.team.name = :team");
            // Implicit path joins are inner joins, so the two possible parents are counted separately.
            case "Evidence" -> count("select count(e) from Evidence e where e.opportunity.outcome.product.team.name = :team") +
            count("select count(e) from Evidence e where e.assumption.solution.opportunity.outcome.product.team.name = :team");
            default -> throw new IllegalArgumentException(entity);
        };
    }

    private Map<String, Long> tableCounts() {
        return Map.of(
            "users",
            userRepository.count(),
            "teams",
            teamRepository.count(),
            "members",
            teamMemberRepository.count(),
            "products",
            productRepository.count(),
            "opportunities",
            opportunityRepository.count(),
            "evidence",
            evidenceRepository.count(),
            "links",
            nodeLinkRepository.count(),
            "questions",
            openQuestionRepository.count(),
            "comments",
            commentRepository.count(),
            "history",
            nodeHistoryRepository.count()
        );
    }

    private Map<String, TeamRole> rolesOf(String login) {
        return teamMemberRepository
            .findAllByUserLogin(login)
            .stream()
            .collect(Collectors.toMap(member -> member.getTeam().getName(), TeamMember::getRole));
    }

    @Test
    void disabledSeederWritesNothing() {
        Map<String, Long> before = tableCounts();
        assertThat(seeder(false).seed()).isFalse();
        assertThat(tableCounts()).isEqualTo(before);
    }

    @Test
    void seedsUsersWithKeycloakIdsAndAuthorities() {
        assertThat(seeder(true).seed()).isTrue();

        User admin = userRepository.findOneByLogin(DevDataSeeder.ADMIN_LOGIN).orElseThrow();
        assertThat(admin.getId()).isEqualTo("4c973896-5761-41fc-8217-07c5d13a004b");
        assertThat(admin.isActivated()).isTrue();
        assertThat(admin.getAuthorities())
            .extracting(Authority::getName)
            .containsExactlyInAnyOrder(AuthoritiesConstants.ADMIN, AuthoritiesConstants.USER);

        User user = userRepository.findOneByLogin(DevDataSeeder.USER_LOGIN).orElseThrow();
        assertThat(user.getId()).isEqualTo("c4af4e2f-b432-4c3b-8405-cca86cd5b97b");
        assertThat(user.isActivated()).isTrue();
        assertThat(user.getAuthorities()).extracting(Authority::getName).containsExactly(AuthoritiesConstants.USER);
    }

    @Test
    void seedsTheFourTeamsWithTheirRoles() {
        seeder(true).seed();

        assertThat(rolesOf(DevDataSeeder.USER_LOGIN)).isEqualTo(
            Map.of(DevDataSeeder.TEAM_JUPITER, TeamRole.OWNER, DevDataSeeder.TEAM_VENUS, TeamRole.OWNER)
        );
        assertThat(rolesOf(DevDataSeeder.ADMIN_LOGIN)).isEqualTo(
            Map.of(
                DevDataSeeder.TEAM_JUPITER,
                TeamRole.VIEWER,
                DevDataSeeder.TEAM_VENUS,
                TeamRole.VIEWER,
                DevDataSeeder.BEST_TEAM,
                TeamRole.OWNER,
                DevDataSeeder.TEAM_MARS,
                TeamRole.OWNER
            )
        );
        for (String name : List.of(
            DevDataSeeder.TEAM_JUPITER,
            DevDataSeeder.TEAM_VENUS,
            DevDataSeeder.BEST_TEAM,
            DevDataSeeder.TEAM_MARS
        )) {
            assertThat(
                teamRepository
                    .findAll()
                    .stream()
                    .filter(team -> name.equals(team.getName()))
            )
                .as(name)
                .hasSize(1);
        }
        // Only Team Jupiter gets the tree.
        assertThat(
            em
                .createQuery("select count(p) from Product p where p.team.name in :names", Long.class)
                .setParameter("names", List.of(DevDataSeeder.TEAM_VENUS, DevDataSeeder.BEST_TEAM, DevDataSeeder.TEAM_MARS))
                .getSingleResult()
        ).isZero();
    }

    @Test
    void seedsThePrototypeTreeIntoTeamJupiter() {
        seeder(true).seed();

        PROTOTYPE_COUNTS.forEach((entity, expected) -> {
            assertThat(nodeCount(entity)).as(entity).isEqualTo(expected);
            assertThat(DevDataSeeder.count(TreeNodeType.valueOf(entity.toUpperCase()))).as(entity + " in SEED").isEqualTo(expected);
        });
        assertThat(
            em
                .createQuery("select p.name from Product p where p.team.name = :team order by p.sortOrder", String.class)
                .setParameter("team", DevDataSeeder.TEAM_JUPITER)
                .getResultList()
        ).containsExactly("Discovery Canvas", "Insight Library");

        // Evidence never hangs off a solution (the model has no such FK): every seeded evidence node sits
        // under exactly one opportunity or assumption, and the prototype's three all sit under opportunities.
        assertThat(
            count("select count(e) from Evidence e where e.assumption is null and e.opportunity.outcome.product.team.name = :team")
        ).isEqualTo(3);
        assertThat(
            count("select count(e) from Evidence e join e.opportunity o join e.assumption a where o.outcome.product.team.name = :team")
        ).isZero();

        // Nested opportunities: op1a under op1, op3a under op3, sharing their parent's outcome.
        assertThat(
            count(
                "select count(o) from Opportunity o where o.parent is not null and o.outcome = o.parent.outcome and o.outcome.product.team.name = :team"
            )
        ).isEqualTo(2);

        // Collaboration data from the prototype.
        assertThat(count("select count(c) from Comment c where c.opportunity.outcome.product.team.name = :team")).isEqualTo(6);
        assertThat(count("select count(q) from OpenQuestion q where q.opportunity.outcome.product.team.name = :team")).isEqualTo(13);
        assertThat(
            count("select count(q) from OpenQuestion q where q.done = true and q.opportunity.outcome.product.team.name = :team")
        ).isEqualTo(2);
        assertThat(
            count("select count(l) from NodeLink l where l.product.team.name = :team") +
                count("select count(l) from NodeLink l where l.outcome.product.team.name = :team") +
                count("select count(l) from NodeLink l where l.opportunity.outcome.product.team.name = :team") +
                count("select count(l) from NodeLink l where l.solution.opportunity.outcome.product.team.name = :team") +
                count("select count(l) from NodeLink l where l.assumption.solution.opportunity.outcome.product.team.name = :team") +
                count("select count(l) from NodeLink l where l.evidence.opportunity.outcome.product.team.name = :team")
        ).isEqualTo(49);

        // Assumption owners map Kira -> user, Ana/Jon -> admin.
        assertThat(
            count(
                "select count(a) from Assumption a where a.owner.login = 'user' and a.solution.opportunity.outcome.product.team.name = :team"
            )
        ).isEqualTo(2);
        assertThat(
            count(
                "select count(a) from Assumption a where a.owner.login = 'admin' and a.solution.opportunity.outcome.product.team.name = :team"
            )
        ).isEqualTo(4);

        // Value and priority as in the prototype (op1: VAL 5, PRI 5 -> 92).
        Object[] op1 = em
            .createQuery("select o.valuerating, o.priority, o.status from Opportunity o where o.title = :title", Object[].class)
            .setParameter("title", "I can never find time to schedule interviews")
            .getSingleResult();
        assertThat(op1[0]).isEqualTo(5);
        assertThat(op1[1]).isEqualTo(92);
        assertThat(op1[2]).hasToString("EXPLORING");
    }

    @Test
    void writesCreatedHistoryForEveryNonProductNode() {
        long createdBefore = historyCount(HistoryEventType.CREATED);
        long allBefore = nodeHistoryRepository.count();
        long productBefore = productHistoryCount();

        seeder(true).seed();

        long nonProductNodes = PROTOTYPE_COUNTS.entrySet()
            .stream()
            .filter(entry -> !"Product".equals(entry.getKey()))
            .mapToLong(Map.Entry::getValue)
            .sum();
        assertThat(historyCount(HistoryEventType.CREATED) - createdBefore).isEqualTo(nonProductNodes);
        // Plus the prototype's second entry per node ("Status set to ..." or "Linked to Confluence").
        assertThat(nodeHistoryRepository.count() - allBefore).isEqualTo(2 * nonProductNodes);
        assertThat(productHistoryCount()).isEqualTo(productBefore);

        // Seeded timestamps are in the past.
        Instant newest = em.createQuery("select max(h.createdDate) from NodeHistory h", Instant.class).getSingleResult();
        assertThat(newest).isBeforeOrEqualTo(Instant.now());
    }

    private long historyCount(HistoryEventType type) {
        return em
            .createQuery("select count(h) from NodeHistory h where h.eventType = :type", Long.class)
            .setParameter("type", type)
            .getSingleResult();
    }

    private long productHistoryCount() {
        return em
            .createQuery("select count(h) from NodeHistory h where h.nodeType = :type", Long.class)
            .setParameter("type", TreeNodeType.PRODUCT)
            .getSingleResult();
    }

    @Test
    void applicationRunnerSeedsOnceLiquibaseIsDone() throws Exception {
        seeder(true).run(null);
        assertThat(nodeCount("Product")).isEqualTo(2);
    }

    @Test
    void secondRunCreatesNothing() {
        assertThat(seeder(true).seed()).isTrue();
        Map<String, Long> afterFirst = tableCounts();

        assertThat(seeder(true).seed()).isFalse();
        assertThat(tableCounts()).isEqualTo(afterFirst);
    }

    @Test
    void existingTeamsAreSkippedPerTeamAndTheTreeNeedsANewJupiter() {
        Team existingJupiter = new Team();
        existingJupiter.setName(DevDataSeeder.TEAM_JUPITER);
        existingJupiter.setCreatedDate(Instant.now());
        teamRepository.saveAndFlush(existingJupiter);
        Team existingMars = new Team();
        existingMars.setName(DevDataSeeder.TEAM_MARS);
        existingMars.setCreatedDate(Instant.now());
        teamRepository.saveAndFlush(existingMars);

        // Venus and Best Team are still missing, so the run writes something...
        assertThat(seeder(true).seed()).isTrue();

        for (String name : List.of(
            DevDataSeeder.TEAM_JUPITER,
            DevDataSeeder.TEAM_VENUS,
            DevDataSeeder.BEST_TEAM,
            DevDataSeeder.TEAM_MARS
        )) {
            assertThat(teamsNamed(name)).as(name).isEqualTo(1);
        }
        // ...but the pre-existing teams are left alone: no members added, and no tree in the old Jupiter.
        assertThat(
            teamMemberRepository
                .findAll()
                .stream()
                .filter(m -> m.getTeam().getId().equals(existingJupiter.getId()))
        ).isEmpty();
        assertThat(
            teamMemberRepository
                .findAll()
                .stream()
                .filter(m -> m.getTeam().getId().equals(existingMars.getId()))
        ).isEmpty();
        assertThat(nodeCount("Product")).isZero();
        assertThat(rolesOf(DevDataSeeder.USER_LOGIN)).isEqualTo(Map.of(DevDataSeeder.TEAM_VENUS, TeamRole.OWNER));

        // Everything exists now: a further run is a no-op.
        Map<String, Long> afterFirst = tableCounts();
        assertThat(seeder(true).seed()).isFalse();
        assertThat(tableCounts()).isEqualTo(afterFirst);
    }

    @Test
    void seededEvidenceCountsAsThisMonth() {
        seeder(true).seed();

        Instant now = Instant.now();
        Instant monthStart = now.atZone(ZoneOffset.UTC).toLocalDate().withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        long evidenceThisMonth = em
            .createQuery(
                "select count(e) from Evidence e where e.opportunity.outcome.product.team.name = :team and e.createdDate >= :start",
                Long.class
            )
            .setParameter("team", DevDataSeeder.TEAM_JUPITER)
            .setParameter("start", monthStart)
            .getSingleResult();
        assertThat(evidenceThisMonth).isEqualTo(3);
        // Every seeded history row sits inside the current month and not in the future.
        List<Instant> seededHistory = em
            .createQuery(
                "select h.createdDate from NodeHistory h where h.nodeType = :type and h.nodeId in " +
                    "(select o.id from Opportunity o where o.outcome.product.team.name = :team)",
                Instant.class
            )
            .setParameter("type", TreeNodeType.OPPORTUNITY)
            .setParameter("team", DevDataSeeder.TEAM_JUPITER)
            .getResultList();
        assertThat(seededHistory)
            .isNotEmpty()
            .allSatisfy(t -> assertThat(t).isBetween(monthStart, now));
    }

    @Test
    void seedAnchorKeepsThePrototypeWeekWhenItIsInTheCurrentMonth() {
        // Saturday 19 Sep 2026: the reference Monday (14 Sep) is this month, so it is used as is.
        assertThat(DevDataSeeder.seedAnchor(Instant.parse("2026-09-19T12:00:00Z"))).isEqualTo(Instant.parse("2026-09-14T00:00:00Z"));
    }

    @Test
    void seedAnchorMovesIntoTheCurrentMonthWhenTheReferenceWeekStartedLastMonth() {
        // Saturday 3 Oct 2026 18:00: the reference Monday is 28 Sep, so "Mon" becomes Fri 2 Oct and "Tue" Sat 3 Oct.
        Instant sat = Instant.parse("2026-10-03T18:00:00Z");
        assertThat(DevDataSeeder.seedAnchor(sat)).isEqualTo(Instant.parse("2026-10-02T00:00:00Z"));
        assertThat(DevDataSeeder.at(DevDataSeeder.seedAnchor(sat), "Tue", "16:18", sat)).isEqualTo(Instant.parse("2026-10-03T16:18:00Z"));

        // Friday 2 Oct 2026 10:00: not two full days into the month yet, so the 1st is the anchor and
        // anything that would land after "now" is clamped to now.
        Instant fri = Instant.parse("2026-10-02T10:00:00Z");
        Instant anchor = DevDataSeeder.seedAnchor(fri);
        assertThat(anchor).isEqualTo(Instant.parse("2026-10-01T00:00:00Z"));
        assertThat(DevDataSeeder.at(anchor, "Mon", "08:31", fri)).isEqualTo(Instant.parse("2026-10-01T08:31:00Z"));
        assertThat(DevDataSeeder.at(anchor, "Tue", "11:43", fri)).isEqualTo(fri);

        // Just after midnight on the 1st: everything collapses to now, which is still this month.
        Instant first = Instant.parse("2026-10-01T00:30:00Z");
        assertThat(DevDataSeeder.at(DevDataSeeder.seedAnchor(first), "Mon", "08:10", first)).isEqualTo(first);
    }

    @Test
    void onlySchemaNotReadyErrorsAreRetried() {
        assertThat(
            DevDataSeeder.isSchemaNotReady(new BadSqlGrammarException("seed", "select 1", new SQLException("no table", "42P01")))
        ).isTrue();
        assertThat(DevDataSeeder.isSchemaNotReady(new RuntimeException("wrapped", new SQLException("no column", "42703")))).isTrue();
        assertThat(DevDataSeeder.isSchemaNotReady(new DataIntegrityViolationException("dup", new SQLException("dup", "23505")))).isFalse();
        assertThat(DevDataSeeder.isSchemaNotReady(new IllegalStateException("Missing authority ROLE_X"))).isFalse();
        assertThat(DevDataSeeder.isSchemaNotReady(new RuntimeException("no state", new SQLException("no state")))).isFalse();
        // H2's missing-table/column states.
        assertThat(DevDataSeeder.isSchemaNotReady(new RuntimeException(new SQLException("no table", "42S02")))).isTrue();
        assertThat(DevDataSeeder.isSchemaNotReady(new RuntimeException(new SQLException("empty db", "42S04")))).isTrue();
        assertThat(DevDataSeeder.isSchemaNotReady(new RuntimeException(new SQLException("no column", "42S22")))).isTrue();
    }

    @Test
    void otherClass42ErrorsAreNotRetried() {
        // Insufficient privilege is class 42 too, but no amount of waiting for Liquibase fixes it.
        assertThat(
            DevDataSeeder.isSchemaNotReady(new BadSqlGrammarException("seed", "select 1", new SQLException("permission denied", "42501")))
        ).isFalse();
        assertThat(DevDataSeeder.isSchemaNotReady(new RuntimeException(new SQLException("syntax error", "42601")))).isFalse();
        // Spring's resource-usage exception without a missing-table/column cause is not enough either.
        assertThat(DevDataSeeder.isSchemaNotReady(new InvalidDataAccessResourceUsageException("bad usage"))).isFalse();
    }

    private long teamsNamed(String name) {
        return teamRepository
            .findAll()
            .stream()
            .filter(team -> name.equals(team.getName()))
            .count();
    }

    @Test
    void referenceMondayIsAMondayWhoseTuesdayAfternoonHasPassed() {
        Instant now = Instant.parse("2026-09-15T10:00:00Z"); // a Tuesday morning
        Instant monday = DevDataSeeder.referenceMonday(now);
        assertThat(monday).isEqualTo(Instant.parse("2026-09-07T00:00:00Z"));
        assertThat(monday.atZone(ZoneOffset.UTC).getDayOfWeek()).hasToString("MONDAY");

        assertThat(DevDataSeeder.referenceMonday(Instant.parse("2026-09-19T12:00:00Z"))).isEqualTo(Instant.parse("2026-09-14T00:00:00Z"));
    }
}
