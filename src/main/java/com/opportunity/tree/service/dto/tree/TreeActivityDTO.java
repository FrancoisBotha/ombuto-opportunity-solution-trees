package com.opportunity.tree.service.dto.tree;

import java.io.Serializable;
import java.time.Instant;

/** The newest history event in a product's branch: when and by whom ({@code byLogin} may be null). */
public record TreeActivityDTO(Instant at, String byLogin) implements Serializable {}
