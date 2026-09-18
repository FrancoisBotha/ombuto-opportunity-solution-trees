package com.opportunity.tree.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class SolutionLinkTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static SolutionLink getSolutionLinkSample1() {
        return new SolutionLink().id(1L).name("name1").url("url1").sortOrder(1);
    }

    public static SolutionLink getSolutionLinkSample2() {
        return new SolutionLink().id(2L).name("name2").url("url2").sortOrder(2);
    }

    public static SolutionLink getSolutionLinkRandomSampleGenerator() {
        return new SolutionLink()
            .id(longCount.incrementAndGet())
            .name(UUID.randomUUID().toString())
            .url(UUID.randomUUID().toString())
            .sortOrder(intCount.incrementAndGet());
    }
}
