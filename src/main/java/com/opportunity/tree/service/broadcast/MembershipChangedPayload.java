package com.opportunity.tree.service.broadcast;

import com.opportunity.tree.domain.enumeration.TeamRole;

/**
 * Payload of a {@link TreeChangeType#MEMBERSHIP_CHANGED} event: the affected user's {@code login}
 * and their new {@code role} in the team. When {@code removed} is true the user is no longer a
 * member of the team ({@code role} is null in that case). Every subscribed client on the team
 * topic receives it; the client whose login matches the payload recomputes {@code canEdit} and
 * {@code currentUserRole} live (FR-037), and if {@code removed} is true unsubscribes so that
 * subsequent tree events do not reach it.
 */
public record MembershipChangedPayload(String login, TeamRole role, boolean removed) {}
