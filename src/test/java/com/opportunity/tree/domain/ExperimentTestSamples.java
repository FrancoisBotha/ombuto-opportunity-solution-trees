package com.opportunity.tree.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class ExperimentTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));

    public static Experiment getExperimentSample1() {
        return new Experiment().id(1L).title("title1").method("method1");
    }

    public static Experiment getExperimentSample2() {
        return new Experiment().id(2L).title("title2").method("method2");
    }

    public static Experiment getExperimentRandomSampleGenerator() {
        return new Experiment().id(longCount.incrementAndGet()).title(UUID.randomUUID().toString()).method(UUID.randomUUID().toString());
    }
}
