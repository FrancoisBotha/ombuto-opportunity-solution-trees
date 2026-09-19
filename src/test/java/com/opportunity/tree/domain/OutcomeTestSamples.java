package com.opportunity.tree.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class OutcomeTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static Outcome getOutcomeSample1() {
        return new Outcome().id(1L).title("title1").sortOrder(1);
    }

    public static Outcome getOutcomeSample2() {
        return new Outcome().id(2L).title("title2").sortOrder(2);
    }

    public static Outcome getOutcomeRandomSampleGenerator() {
        return new Outcome().id(longCount.incrementAndGet()).title(UUID.randomUUID().toString()).sortOrder(intCount.incrementAndGet());
    }
}
