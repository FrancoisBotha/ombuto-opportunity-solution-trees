package com.opportunity.tree.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class MeetingTranscriptTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));

    public static MeetingTranscript getMeetingTranscriptSample1() {
        return new MeetingTranscript().id(1L).title("title1").attendees("attendees1");
    }

    public static MeetingTranscript getMeetingTranscriptSample2() {
        return new MeetingTranscript().id(2L).title("title2").attendees("attendees2");
    }

    public static MeetingTranscript getMeetingTranscriptRandomSampleGenerator() {
        return new MeetingTranscript()
            .id(longCount.incrementAndGet())
            .title(UUID.randomUUID().toString())
            .attendees(UUID.randomUUID().toString());
    }
}
