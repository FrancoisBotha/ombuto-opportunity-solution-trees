package com.opportunity.tree.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class SolutionTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static Solution getSolutionSample1() {
        return new Solution().id(1L).title("title1").effort(1).sortOrder(1);
    }

    public static Solution getSolutionSample2() {
        return new Solution().id(2L).title("title2").effort(2).sortOrder(2);
    }

    public static Solution getSolutionRandomSampleGenerator() {
        return new Solution()
            .id(longCount.incrementAndGet())
            .title(UUID.randomUUID().toString())
            .effort(intCount.incrementAndGet())
            .sortOrder(intCount.incrementAndGet());
    }
}
