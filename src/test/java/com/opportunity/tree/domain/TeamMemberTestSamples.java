package com.opportunity.tree.domain;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class TeamMemberTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));

    public static TeamMember getTeamMemberSample1() {
        return new TeamMember().id(1L);
    }

    public static TeamMember getTeamMemberSample2() {
        return new TeamMember().id(2L);
    }

    public static TeamMember getTeamMemberRandomSampleGenerator() {
        return new TeamMember().id(longCount.incrementAndGet());
    }
}
