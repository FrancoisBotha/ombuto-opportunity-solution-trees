package com.opportunity.tree.domain;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class CommentTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));

    public static Comment getCommentSample1() {
        return new Comment().id(1L);
    }

    public static Comment getCommentSample2() {
        return new Comment().id(2L);
    }

    public static Comment getCommentRandomSampleGenerator() {
        return new Comment().id(longCount.incrementAndGet());
    }
}
