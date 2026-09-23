package com.opportunity.tree.config.seed;

import com.opportunity.tree.config.ApplicationProperties;
import com.opportunity.tree.domain.Assumption;
import com.opportunity.tree.domain.Authority;
import com.opportunity.tree.domain.Comment;
import com.opportunity.tree.domain.Evidence;
import com.opportunity.tree.domain.NodeHistory;
import com.opportunity.tree.domain.NodeLink;
import com.opportunity.tree.domain.OpenQuestion;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.TeamMember;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.AssumptionStatus;
import com.opportunity.tree.domain.enumeration.HistoryEventType;
import com.opportunity.tree.domain.enumeration.OpportunityStatus;
import com.opportunity.tree.domain.enumeration.SolutionStatus;
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
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Seeds the dev database with the Ombuto OST prototype's data (OST step 2, amendments A3 + A6).
 * <p>
 * Runs once at start-up under the {@code dev} profile when {@code application.seed.enabled} is true
 * (it is in {@code application-dev.yml}). Idempotent per team: each of the four teams is only
 * created when no team of that name exists, and the tree is only seeded into a {@value #TEAM_JUPITER}
 * created by the same run. Everything is written in one transaction through the generated
 * repositories. Start-up retries only while the schema is not migrated yet (missing tables, Liquibase
 * lock held); any other failure is logged once and seeding stops.
 * <p>
 * What it creates:
 * <ul>
 *   <li>{@code jhi_user} rows for {@code admin} and {@code user}, if missing by login, with the
 *   Keycloak ids from {@code src/main/docker/realm-config/jhipster-realm.json}.
 *   {@code UserService.syncUserWithIdP} looks users up by login, so on first login these rows are
 *   adopted and updated, not duplicated.</li>
 *   <li>Four teams: "Team Jupiter" and "Team Venus" ({@code user} OWNER, {@code admin} VIEWER),
 *   "Best Team" and "Team Mars" ({@code admin} OWNER only).</li>
 *   <li>In Team Jupiter, the full tree from {@code Ombuto OST.dc.html} ({@code SEED}): both
 *   products with every outcome, opportunity, solution, assumption and evidence node, plus the
 *   prototype's links, open questions, comments and history.</li>
 * </ul>
 * Mapping from the prototype (see the handoff's {@code Ombuto OST.dc.html}):
 * <ul>
 *   <li>People: the dev realm only has two accounts, so {@code KP} (Kira P., the prototype's
 *   signed-in user and outcome owner) becomes {@code user}; {@code AR} (Ana R.) and {@code JS}
 *   (Jon S.) both become {@code admin}. This applies to comment authors, history authors and
 *   assumption owners alike.</li>
 *   <li>Priority is the prototype's {@code PRI * 20 - 8} (default {@code PRI} 3), value rating is
 *   {@code VAL} (default 3), assumption confidence is {@code conf}.</li>
 *   <li>Dropped because the model has no field for them: outcome status ("at risk", "on track"),
 *   confidence on outcomes, opportunities and solutions, and priority on anything but opportunities.</li>
 *   <li>Evidence: the prototype places all three evidence nodes under opportunities, so none had
 *   to be re-homed (the model forbids evidence under a solution).</li>
 *   <li>Links, open questions and the two history entries per node follow the prototype's
 *   initial-state rules (link URLs and Jira keys use the node's index in {@code SEED}).
 *   Products get links but no history.</li>
 *   <li>Times: the prototype's "Mon 09:12" / "Tue 11:20" become UTC times in the most recent
 *   week whose Tuesday 17:00 has already passed — unless that Monday is in the previous calendar
 *   month, in which case they move to the latest two days of the current month (see
 *   {@link #seedAnchor(Instant)}), so everything seeded is in the current month and in the past.</li>
 *   <li>{@code sortOrder} follows array order within each parent (0-based, like products).</li>
 * </ul>
 */
@Component
@Profile("dev")
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DevDataSeeder.class);

    public static final String TEAM_JUPITER = "Team Jupiter";
    public static final String TEAM_VENUS = "Team Venus";
    public static final String BEST_TEAM = "Best Team";
    public static final String TEAM_MARS = "Team Mars";

    public static final String ADMIN_LOGIN = "admin";
    public static final String USER_LOGIN = "user";
    /** Keycloak user ids (== OIDC {@code sub}) from jhipster-realm.json. */
    public static final String ADMIN_ID = "4c973896-5761-41fc-8217-07c5d13a004b";
    public static final String USER_ID = "c4af4e2f-b432-4c3b-8405-cca86cd5b97b";

    /** Prototype people -> seeded logins. */
    static final Map<String, String> PEOPLE = Map.of("KP", USER_LOGIN, "AR", ADMIN_LOGIN, "JS", ADMIN_LOGIN);
    /** Prototype assumption owner names -> the same people. */
    private static final Map<String, String> OWNER_INITIALS = Map.of("Kira P.", "KP", "Ana R.", "AR", "Jon S.", "JS");

    private record Chat(String who, String day, String time, String text) {}

    private record Question(String text, boolean done) {}

    private record Node(
        String id,
        TreeNodeType type,
        String parent,
        String title,
        String note,
        String status,
        int conf,
        String owner,
        List<Chat> comments
    ) {}

    private static Node n(String id, TreeNodeType type, String parent, String title) {
        return new Node(id, type, parent, title, null, null, 40, null, List.of());
    }

    private static Node n(String id, TreeNodeType type, String parent, String title, String status, int conf) {
        return new Node(id, type, parent, title, null, status, conf, null, List.of());
    }

    private static Node a(String id, String parent, String title, String status, int conf, String owner) {
        return new Node(id, TreeNodeType.ASSUMPTION, parent, title, null, status, conf, owner, List.of());
    }

    private static final TreeNodeType P = TreeNodeType.PRODUCT;
    private static final TreeNodeType O = TreeNodeType.OUTCOME;
    private static final TreeNodeType OP = TreeNodeType.OPPORTUNITY;
    private static final TreeNodeType S = TreeNodeType.SOLUTION;
    private static final TreeNodeType E = TreeNodeType.EVIDENCE;

    /** The prototype's {@code SEED} array, in order. */
    private static final List<Node> SEED = List.of(
        n("p1", P, null, "Discovery Canvas"),
        new Node(
            "o1",
            O,
            "p1",
            "Teams running ≥1 interview a week: 34% → 60%",
            "Outcome owner: Kira. Reviewed fortnightly against the discovery habit metric.",
            null,
            60,
            null,
            List.of()
        ),
        new Node(
            "op1",
            OP,
            "o1",
            "I can never find time to schedule interviews",
            null,
            "exploring",
            60,
            null,
            List.of(
                new Chat("AR", "Mon", "09:12", "Heard this in 7 of 9 interviews — biggest single blocker."),
                new Chat("JS", "Mon", "09:41", "Same in the enterprise segment, though there it’s procurement, not calendars."),
                new Chat("KP", "Mon", "10:02", "Splitting the recruiting pain into its own opportunity so we can size it separately."),
                new Chat("AR", "Tue", "08:30", "Two more interviews booked for Thursday — I’ll test the in-app invite script."),
                new Chat("KP", "Tue", "16:18", "Nice. Let’s hold the calendar solution until that comes back.")
            )
        ),
        n("op1a", OP, "op1", "Recruiting a participant takes me a week", "validated", 80),
        n("s1", S, "op1a", "Auto-recruit from an in-product prompt", "building", 60),
        a("a1", "s1", "Users will accept an in-app interview invite", "testing", 40, "Kira P."),
        a("a2", "s1", "Sales won’t block outreach to enterprise accounts", "untested", 20, "Jon S."),
        n("s2", S, "op1", "Calendar-linked standing interview slots", "candidate", 40),
        a("a3", "s2", "Teams keep a shared discovery calendar", "refuted", 20, "Ana R."),
        n("e1", E, "op1", "“I book interviews at 11pm because that’s when I have a gap.” — PM, Acme"),
        n("op2", OP, "o1", "I don’t know what to ask once I’m in the room", "exploring", 40),
        n("s3", S, "op2", "Story-based interview prompts in the note pad", "candidate", 60),
        a("a4", "s3", "Prompts increase story specificity", "supported", 80, "Ana R."),
        n("e2", E, "op2", "6 of 9 recorded interviews drifted into solution talk by minute 4"),
        n("p2", P, null, "Insight Library"),
        n("o2", O, "p2", "Time from interview to shared insight: 9 days → 2", null, 60),
        new Node(
            "op3",
            OP,
            "o2",
            "My insights get lost in a doc nobody reopens",
            null,
            "validated",
            80,
            null,
            List.of(new Chat("JS", "Tue", "14:40", "Analytics back this: 71% of snippets are never reopened."))
        ),
        n("s5", S, "op3", "Auto-tag snippets onto opportunities", "building", 60),
        a("a5", "s5", "Auto-tags are accurate enough to be trusted", "testing", 40, "Kira P."),
        n("op3a", OP, "op3", "I can’t tell if a quote is still true six months on", "unexplored", 20),
        n("e3", E, "op3", "71% of saved snippets were never opened a second time"),
        n("op4", OP, "o2", "I can’t tell which opportunity matters most", "exploring", 40),
        n("s6", S, "op4", "Opportunity sizing from snippet volume + reach", "candidate", 40),
        a("a6", "s6", "Snippet volume correlates with customer pain", "untested", 20, "Jon S.")
    );

    /** The prototype's {@code QUESTIONS}; other opportunities get {@link #DEFAULT_QUESTIONS}. */
    private static final Map<String, List<Question>> QUESTIONS = Map.of(
        "op1",
        List.of(
            new Question("Does this hold for teams outside the trio model?", false),
            new Question("Interview two enterprise PMs about procurement", true),
            new Question("Split recruiting pain into its own opportunity", true)
        ),
        "op3",
        List.of(
            new Question("Which snippets are never reopened, and why?", false),
            new Question("Ask three teams how they search past research", false)
        )
    );
    private static final List<Question> DEFAULT_QUESTIONS = List.of(
        new Question("Who else has said this, and in what context?", false),
        new Question("Is this sized — how many customers does it touch?", false)
    );

    /** The prototype's {@code VAL} (value rating, default 3). */
    private static final Map<String, Integer> VAL = Map.of("op1", 5, "op1a", 4, "op2", 3, "op3", 5, "op3a", 2, "op4", 4);
    /**
     * The prototype's {@code PRI} (1..5, default 3; stored as PRI * 20 - 8). Kept whole for fidelity, but only
     * opportunities have a priority field, so the solution/assumption/evidence entries are not stored.
     */
    private static final Map<String, Integer> PRI = Map.ofEntries(
        Map.entry("op1", 5),
        Map.entry("op1a", 4),
        Map.entry("op2", 3),
        Map.entry("op3", 5),
        Map.entry("op3a", 2),
        Map.entry("op4", 4),
        Map.entry("s1", 5),
        Map.entry("s2", 2),
        Map.entry("s3", 3),
        Map.entry("s5", 4),
        Map.entry("s6", 3),
        Map.entry("a1", 5),
        Map.entry("a2", 2),
        Map.entry("a3", 3),
        Map.entry("a4", 4),
        Map.entry("a5", 4),
        Map.entry("a6", 2),
        Map.entry("e1", 2),
        Map.entry("e2", 2),
        Map.entry("e3", 3)
    );

    /** Retries while the dev profile's asynchronous Liquibase run is still creating the schema. */
    private static final int MAX_ATTEMPTS = 60;
    private static final long RETRY_DELAY_MS = 2_000;

    private final ApplicationProperties applicationProperties;
    private final TransactionTemplate transactionTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ProductRepository productRepository;
    private final OutcomeRepository outcomeRepository;
    private final OpportunityRepository opportunityRepository;
    private final SolutionRepository solutionRepository;
    private final AssumptionRepository assumptionRepository;
    private final EvidenceRepository evidenceRepository;
    private final NodeLinkRepository nodeLinkRepository;
    private final OpenQuestionRepository openQuestionRepository;
    private final CommentRepository commentRepository;
    private final NodeHistoryRepository nodeHistoryRepository;

    public DevDataSeeder(
        ApplicationProperties applicationProperties,
        PlatformTransactionManager transactionManager,
        JdbcTemplate jdbcTemplate,
        UserRepository userRepository,
        AuthorityRepository authorityRepository,
        TeamRepository teamRepository,
        TeamMemberRepository teamMemberRepository,
        ProductRepository productRepository,
        OutcomeRepository outcomeRepository,
        OpportunityRepository opportunityRepository,
        SolutionRepository solutionRepository,
        AssumptionRepository assumptionRepository,
        EvidenceRepository evidenceRepository,
        NodeLinkRepository nodeLinkRepository,
        OpenQuestionRepository openQuestionRepository,
        CommentRepository commentRepository,
        NodeHistoryRepository nodeHistoryRepository
    ) {
        this.applicationProperties = applicationProperties;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.productRepository = productRepository;
        this.outcomeRepository = outcomeRepository;
        this.opportunityRepository = opportunityRepository;
        this.solutionRepository = solutionRepository;
        this.assumptionRepository = assumptionRepository;
        this.evidenceRepository = evidenceRepository;
        this.nodeLinkRepository = nodeLinkRepository;
        this.openQuestionRepository = openQuestionRepository;
        this.commentRepository = commentRepository;
        this.nodeHistoryRepository = nodeHistoryRepository;
    }

    /**
     * Under the dev profile Liquibase runs asynchronously ({@code AsyncSpringLiquibase}), so on a fresh
     * database the schema may not exist yet when this runner starts. Wait for the Liquibase lock to be
     * released and retry on schema errors; a failed attempt rolls back completely.
     */
    @Override
    public void run(ApplicationArguments args) throws InterruptedException {
        if (!applicationProperties.getSeed().isEnabled()) {
            return;
        }
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                if (!liquibaseLocked()) {
                    seed();
                    return;
                }
                LOG.info("Dev seed waiting for Liquibase to release its lock (attempt {}/{})", attempt, MAX_ATTEMPTS);
            } catch (RuntimeException e) {
                if (!isSchemaNotReady(e)) {
                    // A real failure (bad data, constraint violation, bug): report it once and stop — retrying won't help.
                    LOG.error("Dev seed failed; nothing was seeded by this attempt", e);
                    return;
                }
                LOG.warn("Dev seed attempt {}/{}: schema not ready yet ({}), retrying", attempt, MAX_ATTEMPTS, e.getMessage());
                if (attempt == MAX_ATTEMPTS) {
                    LOG.warn("Dev seed gave up after {} attempts", MAX_ATTEMPTS, e);
                    return;
                }
            }
            Thread.sleep(RETRY_DELAY_MS);
        }
        LOG.warn("Dev seed gave up: Liquibase still running after {} attempts", MAX_ATTEMPTS);
    }

    /**
     * SQLStates of a missing table or column — the only errors a not-yet-migrated schema produces.
     * PostgreSQL: {@code 42P01} undefined_table, {@code 42703} undefined_column. H2 (verified on
     * 2.4): {@code 42S02} table not found, {@code 42S03} not found but a differently-cased
     * candidate exists, {@code 42S04} not found in an empty database, {@code 42S22} column not found.
     */
    static final Set<String> SCHEMA_NOT_READY_STATES = Set.of("42P01", "42703", "42S02", "42S03", "42S04", "42S22");

    /**
     * True only when the cause chain holds a {@link SQLException} whose SQLState is in
     * {@link #SCHEMA_NOT_READY_STATES}. Other class-42 errors (syntax errors, {@code 42501}
     * insufficient privilege, …) and Spring's {@code InvalidDataAccessResourceUsageException}
     * on its own are genuine failures that a retry would not fix.
     */
    static boolean isSchemaNotReady(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof SQLException sql && sql.getSQLState() != null && SCHEMA_NOT_READY_STATES.contains(sql.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    /** True while Liquibase holds its lock; throws if the lock table does not exist yet. */
    private boolean liquibaseLocked() {
        Integer locked = jdbcTemplate.queryForObject("select count(*) from databasechangeloglock where locked = true", Integer.class);
        return locked != null && locked > 0;
    }

    /**
     * Seeds everything in one transaction. Idempotent per team: each seeded team is only created
     * when no team of that name exists, and the Team Jupiter tree is only seeded when Team Jupiter
     * is created by this run.
     *
     * @return {@code true} if data was written, {@code false} if seeding is disabled or already done.
     */
    public boolean seed() {
        if (!applicationProperties.getSeed().isEnabled()) {
            LOG.debug("Dev seed disabled (application.seed.enabled=false)");
            return false;
        }
        Boolean seeded = transactionTemplate.execute(status -> seedAll(Instant.now()));
        return Boolean.TRUE.equals(seeded);
    }

    private boolean seedAll(Instant now) {
        Set<String> existingTeams = new HashSet<>();
        teamRepository.findAll().forEach(team -> existingTeams.add(team.getName()));
        if (existingTeams.containsAll(List.of(TEAM_JUPITER, TEAM_VENUS, BEST_TEAM, TEAM_MARS))) {
            LOG.info("Dev seed skipped: all seeded teams already exist");
            return false;
        }
        User admin = ensureUser(
            ADMIN_ID,
            ADMIN_LOGIN,
            "Admin",
            "Administrator",
            "admin@localhost",
            AuthoritiesConstants.ADMIN,
            AuthoritiesConstants.USER
        );
        User user = ensureUser(USER_ID, USER_LOGIN, "", "User", "user@localhost", AuthoritiesConstants.USER);
        Map<String, User> byLogin = Map.of(ADMIN_LOGIN, admin, USER_LOGIN, user);

        List<String> created = new ArrayList<>();
        if (!existingTeams.contains(TEAM_JUPITER)) {
            Team jupiter = team(TEAM_JUPITER, "Discovery Canvas · Insight Library", now);
            member(jupiter, user, TeamRole.OWNER, now);
            member(jupiter, admin, TeamRole.VIEWER, now);
            // The tree only goes into a Team Jupiter this run created, never into an existing one.
            seedTree(jupiter, byLogin, seedAnchor(now), now);
            created.add(TEAM_JUPITER);
        }
        if (!existingTeams.contains(TEAM_VENUS)) {
            Team venus = team(TEAM_VENUS, null, now);
            member(venus, user, TeamRole.OWNER, now);
            member(venus, admin, TeamRole.VIEWER, now);
            created.add(TEAM_VENUS);
        }
        if (!existingTeams.contains(BEST_TEAM)) {
            member(team(BEST_TEAM, null, now), admin, TeamRole.OWNER, now);
            created.add(BEST_TEAM);
        }
        if (!existingTeams.contains(TEAM_MARS)) {
            member(team(TEAM_MARS, null, now), admin, TeamRole.OWNER, now);
            created.add(TEAM_MARS);
        }
        LOG.info(
            "Dev seed complete: created {}{}",
            created,
            created.contains(TEAM_JUPITER) ? " with " + SEED.size() + " tree nodes in '" + TEAM_JUPITER + "'" : ""
        );
        return true;
    }

    private User ensureUser(String id, String login, String firstName, String lastName, String email, String... authorities) {
        return userRepository
            .findOneByLogin(login)
            .orElseGet(() -> {
                User u = new User();
                u.setId(id);
                u.setLogin(login);
                u.setFirstName(firstName);
                u.setLastName(lastName);
                u.setEmail(email);
                u.setActivated(true);
                u.setLangKey("en");
                Set<Authority> set = new HashSet<>();
                for (String name : authorities) {
                    set.add(authorityRepository.findById(name).orElseThrow(() -> new IllegalStateException("Missing authority " + name)));
                }
                u.setAuthorities(set);
                return userRepository.save(u);
            });
    }

    private Team team(String name, String description, Instant now) {
        Team t = new Team();
        t.setName(name);
        t.setDescription(description);
        t.setCreatedDate(now);
        return teamRepository.save(t);
    }

    private void member(Team team, User user, TeamRole role, Instant now) {
        TeamMember m = new TeamMember();
        m.setTeam(team);
        m.setUser(user);
        m.setRole(role);
        m.setJoinedDate(now);
        teamMemberRepository.save(m);
    }

    /** Monday 00:00 UTC of the latest week whose Tuesday 17:00 (the prototype's last timestamp) is in the past. */
    static Instant referenceMonday(Instant now) {
        LocalDate monday = now.atZone(ZoneOffset.UTC).toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        ZonedDateTime start = monday.atStartOfDay(ZoneOffset.UTC);
        if (start.plusDays(1).plusHours(17).toInstant().isAfter(now)) {
            start = start.minusWeeks(1);
        }
        return start.toInstant();
    }

    /**
     * Midnight (UTC) of the day the prototype's "Mon" maps to; "Tue" is the day after.
     * <p>
     * Normally {@link #referenceMonday(Instant)}, so the seed keeps the prototype's weekdays. When that
     * Monday falls in the previous calendar month, the seed is pulled into the current month (so
     * "evidence this month" is non-zero straight after seeding): the anchor becomes the latest day of
     * this month whose following day's 17:00 has passed — keeping the times of day but not the
     * weekday — or the 1st when the month is not that old yet (see {@link #at}, which clamps to now).
     */
    static Instant seedAnchor(Instant now) {
        Instant monday = referenceMonday(now);
        LocalDate today = now.atZone(ZoneOffset.UTC).toLocalDate();
        Instant monthStart = today.withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        if (!monday.isBefore(monthStart)) {
            return monday;
        }
        ZonedDateTime day = today.minusDays(1).atStartOfDay(ZoneOffset.UTC);
        if (day.plusDays(1).plusHours(17).toInstant().isAfter(now)) {
            day = day.minusDays(1);
        }
        Instant anchor = day.toInstant();
        return anchor.isBefore(monthStart) ? monthStart : anchor;
    }

    /** The prototype's "Mon 09:12" / "Tue 11:20" relative to {@code anchor}, never later than {@code now}. */
    static Instant at(Instant anchor, String day, String time, Instant now) {
        int offset = "Tue".equals(day) ? 1 : 0;
        LocalTime t = LocalTime.parse(time);
        Instant when = anchor.plusSeconds(offset * 86_400L + t.toSecondOfDay());
        return when.isAfter(now) ? now : when;
    }

    private void seedTree(Team team, Map<String, User> byLogin, Instant monday, Instant now) {
        Map<String, Object> entities = new HashMap<>();
        Map<String, Integer> nextSort = new HashMap<>();
        int productSort = 0;

        for (int i = 0; i < SEED.size(); i++) {
            Node node = SEED.get(i);
            // Prototype history: "Mon 08:(10+i)" created, "Tue 11:(20+i)" status/link.
            Instant created = at(monday, "Mon", "08:" + (10 + i), now);
            Instant second = at(monday, "Tue", "11:" + (20 + i), now);
            int sortOrder = node.parent() == null ? productSort++ : nextSort.merge(node.parent() + "/" + node.type(), 1, Integer::sum) - 1;
            Object entity = switch (node.type()) {
                case PRODUCT -> {
                    Product p = new Product();
                    p.setName(node.title());
                    p.setArchived(false);
                    p.setSortOrder(sortOrder);
                    p.setCreatedDate(created);
                    p.setTeam(team);
                    yield productRepository.save(p);
                }
                case OUTCOME -> {
                    Outcome o = new Outcome();
                    o.setTitle(node.title());
                    o.setDescription(node.note());
                    o.setSortOrder(sortOrder);
                    o.setCreatedDate(created);
                    o.setLastModifiedDate(second);
                    o.setProduct((Product) entities.get(node.parent()));
                    yield outcomeRepository.save(o);
                }
                case OPPORTUNITY -> {
                    Opportunity o = new Opportunity();
                    o.setTitle(node.title());
                    o.setDescription(node.note());
                    o.setStatus(OpportunityStatus.valueOf(upper(node.status())));
                    o.setValuerating(VAL.getOrDefault(node.id(), 3));
                    o.setPriority(priority(node.id()));
                    o.setSortOrder(sortOrder);
                    o.setCreatedDate(created);
                    o.setLastModifiedDate(second);
                    Object parent = entities.get(node.parent());
                    if (parent instanceof Opportunity parentOpportunity) {
                        o.setParent(parentOpportunity);
                        o.setOutcome(parentOpportunity.getOutcome());
                    } else {
                        o.setOutcome((Outcome) parent);
                    }
                    yield opportunityRepository.save(o);
                }
                case SOLUTION -> {
                    Solution s = new Solution();
                    s.setTitle(node.title());
                    s.setDescription(node.note());
                    s.setStatus(SolutionStatus.valueOf(upper(node.status())));
                    s.setSortOrder(sortOrder);
                    s.setCreatedDate(created);
                    s.setLastModifiedDate(second);
                    s.setOpportunity((Opportunity) entities.get(node.parent()));
                    yield solutionRepository.save(s);
                }
                case ASSUMPTION -> {
                    Assumption as = new Assumption();
                    as.setStatement(node.title());
                    as.setDescription(node.note());
                    as.setStatus(AssumptionStatus.valueOf(upper(node.status())));
                    as.setConfidence(node.conf());
                    as.setSortOrder(sortOrder);
                    as.setCreatedDate(created);
                    as.setLastModifiedDate(second);
                    as.setSolution((Solution) entities.get(node.parent()));
                    if (node.owner() != null) {
                        as.setOwner(byLogin.get(PEOPLE.get(OWNER_INITIALS.get(node.owner()))));
                    }
                    yield assumptionRepository.save(as);
                }
                case EVIDENCE -> {
                    Evidence e = new Evidence();
                    e.setTitle(node.title());
                    e.setDescription(node.note());
                    e.setSortOrder(sortOrder);
                    e.setCreatedDate(created);
                    e.setLastModifiedDate(second);
                    // The prototype has evidence only under opportunities; the model allows opportunity or assumption.
                    Object parent = entities.get(node.parent());
                    if (parent instanceof Assumption assumption) {
                        e.setAssumption(assumption);
                    } else {
                        e.setOpportunity((Opportunity) parent);
                    }
                    yield evidenceRepository.save(e);
                }
            };
            entities.put(node.id(), entity);

            seedLinks(node, i, entity, created);
            if (node.type() == TreeNodeType.OPPORTUNITY) {
                List<Question> questions = QUESTIONS.getOrDefault(node.id(), DEFAULT_QUESTIONS);
                for (int q = 0; q < questions.size(); q++) {
                    OpenQuestion oq = new OpenQuestion();
                    oq.setQuestionText(questions.get(q).text());
                    oq.setDone(questions.get(q).done());
                    oq.setSortOrder(q);
                    oq.setCreatedDate(created);
                    oq.setOpportunity((Opportunity) entity);
                    openQuestionRepository.save(oq);
                }
            }
            for (Chat chat : node.comments()) {
                Comment c = new Comment();
                c.setBody(chat.text());
                c.setCreatedDate(at(monday, chat.day(), chat.time(), now));
                c.setAuthor(byLogin.get(PEOPLE.get(chat.who())));
                c.setOpportunity((Opportunity) entity);
                commentRepository.save(c);
            }
            if (node.type() != TreeNodeType.PRODUCT) {
                Long nodeId = id(entity);
                String label = node.type().name().toLowerCase(Locale.ROOT);
                history(
                    node.type(),
                    nodeId,
                    HistoryEventType.CREATED,
                    "Node created as " + label,
                    created,
                    byLogin.get(PEOPLE.get(i % 3 == 0 ? "AR" : "KP"))
                );
                // Outcomes and evidence have no status in the prototype's initial state, so their second entry is the link.
                if (node.status() != null && node.type() != TreeNodeType.OUTCOME) {
                    history(
                        node.type(),
                        nodeId,
                        HistoryEventType.STATUS_CHANGED,
                        "Status set to " + node.status(),
                        second,
                        byLogin.get(USER_LOGIN)
                    );
                } else {
                    history(node.type(), nodeId, HistoryEventType.LINK_ADDED, "Linked to Confluence", second, byLogin.get(USER_LOGIN));
                }
            }
        }
        seedTags(team, entities);
    }

    /**
     * LABEL-001: give the demo tree a small, realistic label set (persona / segment / theme) so
     * the feature is visible on first launch without hand-setup. Tags are attached to the first
     * opportunity and the first solution in the seeded tree; identical text within a team collapses
     * (uniqueness constraint), so re-runs are safe.
     */
    private void seedTags(Team team, Map<String, Object> entities) {
        String[] tagNames = { "Mobile Value Stream", "Onboarding", "Enterprise", "Retention" };
        Long teamId = team.getId();
        java.util.Map<String, Long> tagIdByName = new java.util.LinkedHashMap<>();
        for (String name : tagNames) {
            String normalized = com.opportunity.tree.domain.Tag.normalize(name);
            Long existing = null;
            try {
                existing = jdbcTemplate.queryForObject(
                    "select id from tag where team_id = ? and normalized_name = ?",
                    Long.class,
                    teamId,
                    normalized
                );
            } catch (org.springframework.dao.EmptyResultDataAccessException ignore) {
                // no existing tag
            }
            if (existing != null) {
                tagIdByName.put(name, existing);
                continue;
            }
            Long tagId = jdbcTemplate.queryForObject("select nextval('sequence_generator')", Long.class);
            jdbcTemplate.update(
                "insert into tag(id, name, colour, normalized_name, team_id) values (?, ?, null, ?, ?)",
                tagId,
                name,
                normalized,
                teamId
            );
            tagIdByName.put(name, tagId);
        }
        Opportunity firstOpp = null;
        Solution firstSol = null;
        for (Object entity : entities.values()) {
            if (firstOpp == null && entity instanceof Opportunity o) {
                firstOpp = o;
            }
            if (firstSol == null && entity instanceof Solution s) {
                firstSol = s;
            }
        }
        if (firstOpp != null) {
            for (String name : new String[] { "Mobile Value Stream", "Onboarding" }) {
                Long tagId = tagIdByName.get(name);
                try {
                    jdbcTemplate.update("insert into rel_opportunity__tag(opportunity_id, tag_id) values (?, ?)", firstOpp.getId(), tagId);
                } catch (org.springframework.dao.DataIntegrityViolationException ignored) {
                    // Idempotent seeding — the join row already exists on a re-run.
                }
            }
        }
        if (firstSol != null) {
            Long tagId = tagIdByName.get("Mobile Value Stream");
            try {
                jdbcTemplate.update("insert into rel_solution__tag(solution_id, tag_id) values (?, ?)", firstSol.getId(), tagId);
            } catch (org.springframework.dao.DataIntegrityViolationException ignored) {
                // idempotent
            }
        }
    }

    private void seedLinks(Node node, int index, Object entity, Instant created) {
        String confluence = "https://ombuto.atlassian.net/wiki/discovery/" + node.id();
        List<String[]> links = switch (node.type()) {
            case PRODUCT -> List.<String[]>of(new String[] { "Product space", "https://ombuto.atlassian.net/wiki/spaces/" + node.id() });
            case OUTCOME, ASSUMPTION -> List.<String[]>of(new String[] { "Confluence", confluence });
            case EVIDENCE -> List.of(
                new String[] { "Confluence", confluence },
                new String[] { "Jira Ticket", "https://ombuto.atlassian.net/browse/DISC-" + (700 + index) }
            );
            case OPPORTUNITY, SOLUTION -> List.of(
                new String[] { "Confluence", confluence },
                new String[] { "Jira Initiative", "https://ombuto.atlassian.net/browse/DISC-" + (100 + index) },
                new String[] { "Jira Epic", "https://ombuto.atlassian.net/browse/DISC-" + (400 + index) }
            );
        };
        for (int l = 0; l < links.size(); l++) {
            NodeLink link = new NodeLink();
            link.setName(links.get(l)[0]);
            link.setUrl(links.get(l)[1]);
            link.setSortOrder(l);
            link.setCreatedDate(created);
            switch (entity) {
                case Product p -> link.setProduct(p);
                case Outcome o -> link.setOutcome(o);
                case Opportunity o -> link.setOpportunity(o);
                case Solution s -> link.setSolution(s);
                case Assumption a -> link.setAssumption(a);
                case Evidence e -> link.setEvidence(e);
                default -> throw new IllegalStateException("Unexpected node " + entity);
            }
            nodeLinkRepository.save(link);
        }
    }

    private void history(TreeNodeType type, Long nodeId, HistoryEventType event, String summary, Instant when, User author) {
        NodeHistory h = new NodeHistory();
        h.setNodeType(type);
        h.setNodeId(nodeId);
        h.setEventType(event);
        h.setSummary(summary);
        h.setCreatedDate(when);
        h.setAuthor(author);
        nodeHistoryRepository.save(h);
    }

    private static Long id(Object entity) {
        return switch (entity) {
            case Outcome o -> o.getId();
            case Opportunity o -> o.getId();
            case Solution s -> s.getId();
            case Assumption a -> a.getId();
            case Evidence e -> e.getId();
            default -> throw new IllegalStateException("No history for " + entity);
        };
    }

    private static int priority(String id) {
        return PRI.getOrDefault(id, 3) * 20 - 8;
    }

    private static String upper(String status) {
        return status.toUpperCase(Locale.ROOT);
    }

    /** Number of prototype nodes of the given type (used by tests). */
    static long count(TreeNodeType type) {
        return SEED.stream()
            .filter(node -> node.type() == type)
            .count();
    }
}
