package com.opportunity.tree.service.mcp;

import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.repository.TeamRepository;
import com.opportunity.tree.service.TeamAccessDeniedException;
import com.opportunity.tree.service.TeamAccessService;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Bounded, read-only MCP projection. Pages are ordered by type, then sortOrder and id. */
@Service
@Transactional(readOnly = true)
public class McpProductTreeReader {

    private static final String[] ENTITIES = { "Product p", "Outcome o", "Opportunity op", "Solution s", "Assumption a", "Evidence e" };
    private static final String[] PRODUCT_WHERE = {
        "p.id = :id",
        "o.product.id = :id",
        "op.outcome.product.id = :id",
        "s.opportunity.outcome.product.id = :id",
        "a.solution.opportunity.outcome.product.id = :id",
        "(op.outcome.product.id = :id or a.solution.opportunity.outcome.product.id = :id)",
    };
    private static final String[] TEAM_WHERE = {
        "p.team.id = :id",
        "o.product.team.id = :id",
        "op.outcome.product.team.id = :id",
        "s.opportunity.outcome.product.team.id = :id",
        "a.solution.opportunity.outcome.product.team.id = :id",
        "(op.outcome.product.team.id = :id or a.solution.opportunity.outcome.product.team.id = :id)",
    };
    private static final String[] SORT = { "p.sortOrder", "o.sortOrder", "op.sortOrder", "s.sortOrder", "a.sortOrder", "e.sortOrder" };
    private static final String[] ID = { "p.id", "o.id", "op.id", "s.id", "a.id", "e.id" };
    private static final String[] JOINS = {
        "",
        " left join o.owner ow",
        " left join op.owner ow",
        " left join s.owner ow",
        " left join a.owner ow",
        " left join e.opportunity op left join e.assumption a",
    };
    private static final String[] SELECT = {
        "p.id, p.name, p.description, cast(null as string), cast(null as integer), cast(null as integer), cast(null as integer), cast(null as string), p.createdDate, cast(null as timestamp), cast(null as string), cast(null as long)",
        "o.id, o.title, o.description, cast(null as string), cast(null as integer), cast(null as integer), cast(null as integer), ow.login, o.createdDate, o.lastModifiedDate, 'PRODUCT', o.product.id",
        "op.id, op.title, op.description, cast(op.status as string), op.priority, op.valuerating, cast(null as integer), ow.login, op.createdDate, op.lastModifiedDate, case when op.parent is null then 'OUTCOME' else 'OPPORTUNITY' end, coalesce(op.parent.id, op.outcome.id)",
        "s.id, s.title, s.description, cast(s.status as string), cast(null as integer), cast(null as integer), cast(null as integer), ow.login, s.createdDate, s.lastModifiedDate, 'OPPORTUNITY', s.opportunity.id",
        "a.id, a.statement, a.description, cast(a.status as string), cast(null as integer), cast(null as integer), a.confidence, ow.login, a.createdDate, a.lastModifiedDate, 'SOLUTION', a.solution.id",
        "e.id, e.title, e.description, cast(null as string), cast(null as integer), cast(null as integer), cast(null as integer), cast(null as string), e.createdDate, e.lastModifiedDate, case when op.id is not null then 'OPPORTUNITY' else 'ASSUMPTION' end, coalesce(op.id, a.id)",
    };

    private final EntityManager entityManager;
    private final TeamAccessService accessService;
    private final TeamRepository teamRepository;

    public McpProductTreeReader(EntityManager entityManager, TeamAccessService accessService, TeamRepository teamRepository) {
        this.entityManager = entityManager;
        this.accessService = accessService;
        this.teamRepository = teamRepository;
    }

    public String requireTeamName(Long teamId) {
        accessService.requireReadTeam(teamId);
        return teamRepository.findById(teamId).orElseThrow(TeamAccessDeniedException::new).getName();
    }

    public void requireProductInTeam(Long productId, Long teamId) {
        Long actualTeam = accessService.requireReadNode(TreeNodeType.PRODUCT, productId);
        if (!teamId.equals(actualTeam)) {
            throw new TeamAccessDeniedException();
        }
    }

    public long countTeamNodes(Long teamId) {
        long total = 0;
        for (int i = 0; i < ENTITIES.length; i++) {
            total += count(i, TEAM_WHERE[i], teamId);
        }
        return total;
    }

    public ProductPage readProduct(Long productId, int offset, int limit) {
        long total = 0;
        long[] counts = new long[ENTITIES.length];
        for (int i = 0; i < ENTITIES.length; i++) {
            counts[i] = count(i, PRODUCT_WHERE[i], productId);
            total += counts[i];
        }
        List<TreeTool.TreeNode> nodes = new ArrayList<>(limit);
        long skipped = 0;
        for (int i = 0; i < ENTITIES.length && nodes.size() < limit; i++) {
            long end = skipped + counts[i];
            if (offset >= end) {
                skipped = end;
                continue;
            }
            int localOffset = (int) Math.max(0, offset - skipped);
            int remaining = limit - nodes.size();
            String hql =
                "select " +
                SELECT[i] +
                " from " +
                ENTITIES[i] +
                JOINS[i] +
                " where " +
                PRODUCT_WHERE[i] +
                " order by " +
                SORT[i] +
                " asc, " +
                ID[i] +
                " asc";
            List<Object[]> rows = entityManager
                .createQuery(hql, Object[].class)
                .setParameter("id", productId)
                .setFirstResult(localOffset)
                .setMaxResults(remaining)
                .getResultList();
            for (Object[] row : rows) {
                nodes.add(
                    new TreeTool.TreeNode(
                        ((Number) row[0]).longValue(),
                        TreeNodeType.values()[i].name(),
                        (String) row[1],
                        (String) row[2],
                        (String) row[3],
                        (String) row[10],
                        row[11] == null ? null : ((Number) row[11]).longValue(),
                        (Integer) row[4],
                        (Integer) row[5],
                        (Integer) row[6],
                        (String) row[7],
                        (Instant) row[8],
                        (Instant) row[9]
                    )
                );
            }
            skipped = end;
        }
        return new ProductPage(total, List.copyOf(nodes));
    }

    private long count(int index, String where, Long id) {
        String joins = index == 5 ? JOINS[index] : "";
        return entityManager
            .createQuery("select count(" + ID[index] + ") from " + ENTITIES[index] + joins + " where " + where, Long.class)
            .setParameter("id", id)
            .getSingleResult();
    }

    public record ProductPage(long total, List<TreeTool.TreeNode> nodes) {}
}
