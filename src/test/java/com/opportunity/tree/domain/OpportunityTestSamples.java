package com.opportunity.tree.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class OpportunityTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static Opportunity getOpportunitySample1() {
        return new Opportunity().id(1L).title("title1").valuerating(1).priority(1).sortOrder(1);
    }

    public static Opportunity getOpportunitySample2() {
        return new Opportunity().id(2L).title("title2").valuerating(2).priority(2).sortOrder(2);
    }

    public static Opportunity getOpportunityRandomSampleGenerator() {
        return new Opportunity()
            .id(longCount.incrementAndGet())
            .title(UUID.randomUUID().toString())
            .valuerating(intCount.incrementAndGet())
            .priority(intCount.incrementAndGet())
            .sortOrder(intCount.incrementAndGet());
    }
}
