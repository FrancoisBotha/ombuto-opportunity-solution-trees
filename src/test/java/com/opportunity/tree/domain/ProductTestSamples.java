package com.opportunity.tree.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class ProductTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));

    public static Product getProductSample1() {
        return new Product().id(1L).name("name1");
    }

    public static Product getProductSample2() {
        return new Product().id(2L).name("name2");
    }

    public static Product getProductRandomSampleGenerator() {
        return new Product().id(longCount.incrementAndGet()).name(UUID.randomUUID().toString());
    }
}
