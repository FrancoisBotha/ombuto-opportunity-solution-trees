package com.opportunity.tree.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class AssumptionTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static Assumption getAssumptionSample1() {
        return new Assumption().id(1L).statement("statement1").confidence(1).sortOrder(1);
    }

    public static Assumption getAssumptionSample2() {
        return new Assumption().id(2L).statement("statement2").confidence(2).sortOrder(2);
    }

    public static Assumption getAssumptionRandomSampleGenerator() {
        return new Assumption()
            .id(longCount.incrementAndGet())
            .statement(UUID.randomUUID().toString())
            .confidence(intCount.incrementAndGet())
            .sortOrder(intCount.incrementAndGet());
    }
}
