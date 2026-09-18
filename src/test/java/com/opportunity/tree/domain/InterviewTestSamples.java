package com.opportunity.tree.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class InterviewTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));

    public static Interview getInterviewSample1() {
        return new Interview().id(1L).title("title1").participant("participant1").recordingUrl("recordingUrl1");
    }

    public static Interview getInterviewSample2() {
        return new Interview().id(2L).title("title2").participant("participant2").recordingUrl("recordingUrl2");
    }

    public static Interview getInterviewRandomSampleGenerator() {
        return new Interview()
            .id(longCount.incrementAndGet())
            .title(UUID.randomUUID().toString())
            .participant(UUID.randomUUID().toString())
            .recordingUrl(UUID.randomUUID().toString());
    }
}
