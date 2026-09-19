package com.opportunity.tree.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class OpenQuestionTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static OpenQuestion getOpenQuestionSample1() {
        return new OpenQuestion().id(1L).questionText("questionText1").sortOrder(1);
    }

    public static OpenQuestion getOpenQuestionSample2() {
        return new OpenQuestion().id(2L).questionText("questionText2").sortOrder(2);
    }

    public static OpenQuestion getOpenQuestionRandomSampleGenerator() {
        return new OpenQuestion()
            .id(longCount.incrementAndGet())
            .questionText(UUID.randomUUID().toString())
            .sortOrder(intCount.incrementAndGet());
    }
}
