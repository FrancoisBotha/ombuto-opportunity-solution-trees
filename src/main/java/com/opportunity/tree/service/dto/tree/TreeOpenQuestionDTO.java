package com.opportunity.tree.service.dto.tree;

import java.io.Serializable;

/** An open question on an opportunity. */
public record TreeOpenQuestionDTO(Long id, String text, Boolean done) implements Serializable {}
