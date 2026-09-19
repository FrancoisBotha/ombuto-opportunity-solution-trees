package com.opportunity.tree.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class NodeHistoryTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));

    public static NodeHistory getNodeHistorySample1() {
        return new NodeHistory().id(1L).nodeId(1L).summary("summary1");
    }

    public static NodeHistory getNodeHistorySample2() {
        return new NodeHistory().id(2L).nodeId(2L).summary("summary2");
    }

    public static NodeHistory getNodeHistoryRandomSampleGenerator() {
        return new NodeHistory().id(longCount.incrementAndGet()).nodeId(longCount.incrementAndGet()).summary(UUID.randomUUID().toString());
    }
}
