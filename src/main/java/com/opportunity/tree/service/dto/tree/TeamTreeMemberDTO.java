package com.opportunity.tree.service.dto.tree;

import com.opportunity.tree.domain.enumeration.TeamRole;
import java.io.Serializable;

/** A team member as shown by the tree builder (avatars, owner picker). */
public record TeamTreeMemberDTO(String login, String firstName, String lastName, String initials, TeamRole role) implements Serializable {}
