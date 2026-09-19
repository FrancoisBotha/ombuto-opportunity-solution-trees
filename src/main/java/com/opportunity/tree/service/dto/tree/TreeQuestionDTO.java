package com.opportunity.tree.service.dto.tree;

import java.io.Serializable;

/** An open question as returned by the question write endpoints. */
public record TreeQuestionDTO(Long id, String text, Boolean done, Integer sortOrder) implements Serializable {}
