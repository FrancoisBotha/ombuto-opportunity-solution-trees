package com.opportunity.tree.service;

import com.opportunity.tree.domain.enumeration.AssumptionStatus;
import com.opportunity.tree.domain.enumeration.OpportunityStatus;
import com.opportunity.tree.domain.enumeration.SolutionStatus;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import java.math.BigDecimal;
import java.math.BigInteger;
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
     * This is the single parser for every {@code {type}} path segment and type body field under
     * {@code /api/tree}.
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
    /**
     * The exact value of a JSON number as Jackson binds it (Integer, Long, BigInteger, Double,
     * Float or BigDecimal); {@code null} for anything else, NaN or infinity.
     */
    static BigDecimal exactNumber(Object value) {
        if (value instanceof BigDecimal b) {
            return b;
        }
        if (value instanceof BigInteger b) {
            return new BigDecimal(b);
        }
        if (value instanceof Double d) {
            return d.isNaN() || d.isInfinite() ? null : BigDecimal.valueOf(d);
        }
        if (value instanceof Float f) {
            return f.isNaN() || f.isInfinite() ? null : BigDecimal.valueOf(f.doubleValue());
        }
        if (value instanceof Long || value instanceof Integer || value instanceof Short || value instanceof Byte) {
            return BigDecimal.valueOf(((Number) value).longValue());
        }
        return null;
    }

    /**
     * A request id or index that must be a whole number ({@code 7} or {@code 7.0}; not {@code 7.5}
     * and not outside the long range). {@code null} stays {@code null}; anything else is a 400
     * with {@code errorKey}.
     */
    public static Long wholeLong(Number value, String field, String errorKey) {
        if (value == null) {
            return null;
        }
        BigDecimal exact = exactNumber(value);
        if (exact != null && exact.stripTrailingZeros().scale() <= 0) {
            try {
                return exact.longValueExact();
            } catch (ArithmeticException e) {
                // Outside the long range: fall through to the 400.
            }
        }
        throw new NodeWriteRuleException(field + " must be a whole number", ENTITY_NAME, errorKey);
    }

    /** As {@link #wholeLong}, limited to the int range. */
    public static Integer wholeInt(Number value, String field, String errorKey) {
        Long whole = wholeLong(value, field, errorKey);
        if (whole == null) {
            return null;
        }
        if (whole < Integer.MIN_VALUE || whole > Integer.MAX_VALUE) {
            throw new NodeWriteRuleException(field + " must be a whole number", ENTITY_NAME, errorKey);
        }
        return whole.intValue();
    }

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

    /**
     * Status as the prototype writes it in history (its status vocabulary is lower case),
     * e.g. {@code VALIDATED} → {@code validated}.
     */
    public static String statusLabel(Enum<?> status) {
        return status.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    /** History summary for a status change (prototype {@code patch()}): {@code Status changed to “exploring”}. */
    public static String statusChangedSummary(Enum<?> status) {
        return "Status changed to “" + statusLabel(status) + "”";
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
