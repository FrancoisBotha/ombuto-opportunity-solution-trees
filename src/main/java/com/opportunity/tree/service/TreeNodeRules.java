package com.opportunity.tree.service;

import com.opportunity.tree.domain.enumeration.AssumptionStatus;
import com.opportunity.tree.domain.enumeration.OpportunityStatus;
import com.opportunity.tree.domain.enumeration.SolutionStatus;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Server-side copy of the OST structural rules (handoff {@code rules.ts} with the plan's edits):
 * the legal parent → child relationships, default titles, title lengths and history wording.
 *
 * <p>Evidence is only allowed under an Opportunity (customer evidence) or an Assumption (test
 * result) — never under a Solution.
 */
public final class TreeNodeRules {

    /** Entity name reported in 400 problem details for tree node write-rule violations. */
    public static final String ENTITY_NAME = "treeNode";

    /** Minimum title length (all types). */
    public static final int MIN_TITLE_LENGTH = 2;

    private static final Map<TreeNodeType, Set<TreeNodeType>> ALLOWED = new EnumMap<>(TreeNodeType.class);

    static {
        ALLOWED.put(TreeNodeType.PRODUCT, EnumSet.of(TreeNodeType.OUTCOME));
        ALLOWED.put(TreeNodeType.OUTCOME, EnumSet.of(TreeNodeType.OPPORTUNITY));
        ALLOWED.put(TreeNodeType.OPPORTUNITY, EnumSet.of(TreeNodeType.OPPORTUNITY, TreeNodeType.SOLUTION, TreeNodeType.EVIDENCE));
        ALLOWED.put(TreeNodeType.SOLUTION, EnumSet.of(TreeNodeType.ASSUMPTION));
        ALLOWED.put(TreeNodeType.ASSUMPTION, EnumSet.of(TreeNodeType.EVIDENCE));
        ALLOWED.put(TreeNodeType.EVIDENCE, EnumSet.noneOf(TreeNodeType.class));
    }

    private TreeNodeRules() {}

    /** True when {@code child} may hang directly under {@code parent}. */
    public static boolean isAllowed(TreeNodeType parent, TreeNodeType child) {
        return parent != null && child != null && ALLOWED.get(parent).contains(child);
    }

    /** Throws a 400 ({@code error.invalidparent}) unless {@code child} may hang under {@code parent}. */
    public static void requireAllowed(TreeNodeType parent, TreeNodeType child) {
        if (!isAllowed(parent, child)) {
            throw new NodeWriteRuleException(
                "A " + label(child) + " cannot be placed under a " + label(parent),
                ENTITY_NAME,
                "invalidparent"
            );
        }
    }

    /**
     * Parses a node type from a path or body value, case-insensitively ({@code opportunity} and
     * {@code OPPORTUNITY} both work). Missing or unknown values are a 400 ({@code error.unknowntype}).
     */
    public static TreeNodeType parseType(String value, String what) {
        if (value == null || value.isBlank()) {
            throw new NodeWriteRuleException(what + " is required", ENTITY_NAME, "unknowntype");
        }
        try {
            return TreeNodeType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new NodeWriteRuleException("Unknown " + what + ": " + value, ENTITY_NAME, "unknowntype");
        }
    }

    /** Lower-case display label, e.g. {@code opportunity}. */
    public static String label(TreeNodeType type) {
        return type == null ? "node" : type.name().toLowerCase(Locale.ROOT);
    }

    /** Title given to a node created without one (prototype wording). */
    public static String defaultTitle(TreeNodeType type) {
        return type == TreeNodeType.EVIDENCE ? "New snippet" : "New " + label(type);
    }

    /** Maximum title length per type (JDL: product name 100, outcome/opportunity/solution 200, assumption/evidence 500). */
    public static int maxTitleLength(TreeNodeType type) {
        return switch (type) {
            case PRODUCT -> 100;
            case OUTCOME, OPPORTUNITY, SOLUTION -> 200;
            case ASSUMPTION, EVIDENCE -> 500;
        };
    }

    /** True when the type carries a status. */
    public static boolean hasStatus(TreeNodeType type) {
        return type == TreeNodeType.OPPORTUNITY || type == TreeNodeType.SOLUTION || type == TreeNodeType.ASSUMPTION;
    }

    /** Parses a status for the type, case-insensitively; a 400 ({@code error.invalidstatus}) when not in the vocabulary. */
    public static Enum<?> parseStatus(TreeNodeType type, String value) {
        if (value == null) {
            throw new NodeWriteRuleException("status cannot be null", ENTITY_NAME, "invalidstatus");
        }
        String v = value.trim().toUpperCase(Locale.ROOT);
        try {
            return switch (type) {
                case OPPORTUNITY -> OpportunityStatus.valueOf(v);
                case SOLUTION -> SolutionStatus.valueOf(v);
                case ASSUMPTION -> AssumptionStatus.valueOf(v);
                default -> throw new IllegalArgumentException();
            };
        } catch (IllegalArgumentException e) {
            throw new NodeWriteRuleException("Invalid status for a " + label(type) + ": " + value, ENTITY_NAME, "invalidstatus");
        }
    }

    /** Status label as shown in history, e.g. {@code VALIDATED} → {@code Validated}. */
    public static String statusLabel(Enum<?> status) {
        String s = status.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** Priority label (prototype {@code priLabel}). */
    public static String priorityLabel(int p) {
        if (p >= 85) {
            return "Critical";
        }
        if (p >= 65) {
            return "High";
        }
        if (p >= 40) {
            return "Medium";
        }
        if (p >= 20) {
            return "Low";
        }
        return "Lowest";
    }
}
