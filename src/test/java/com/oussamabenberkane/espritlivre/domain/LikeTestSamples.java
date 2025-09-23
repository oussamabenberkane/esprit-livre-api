package com.oussamabenberkane.espritlivre.domain;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class LikeTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static Like getLikeSample1() {
        return new Like().id(1L);
    }

    public static Like getLikeSample2() {
        return new Like().id(2L);
    }

    public static Like getLikeRandomSampleGenerator() {
        return new Like().id(longCount.incrementAndGet());
    }
}
