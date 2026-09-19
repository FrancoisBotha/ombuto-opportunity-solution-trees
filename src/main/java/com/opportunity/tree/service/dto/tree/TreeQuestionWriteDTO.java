package com.opportunity.tree.service.dto.tree;

import java.io.Serializable;

/** Request body for adding ({@code text} required) or editing (both optional) an open question. */
public record TreeQuestionWriteDTO(String text, Boolean done) implements Serializable {}
