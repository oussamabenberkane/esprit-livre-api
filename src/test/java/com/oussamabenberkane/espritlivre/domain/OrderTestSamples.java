package com.oussamabenberkane.espritlivre.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class OrderTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static Order getOrderSample1() {
        return new Order()
            .id(1L)
            .uniqueId("uniqueId1")
            .fullName("fullName1")
            .phone("phone1")
            .email("email1")
            .address("address1")
            .createdBy("createdBy1");
    }

    public static Order getOrderSample2() {
        return new Order()
            .id(2L)
            .uniqueId("uniqueId2")
            .fullName("fullName2")
            .phone("phone2")
            .email("email2")
            .address("address2")
            .createdBy("createdBy2");
    }

    public static Order getOrderRandomSampleGenerator() {
        return new Order()
            .id(longCount.incrementAndGet())
            .uniqueId(UUID.randomUUID().toString())
            .fullName(UUID.randomUUID().toString())
            .phone(UUID.randomUUID().toString())
            .email(UUID.randomUUID().toString())
            .address(UUID.randomUUID().toString())
            .createdBy(UUID.randomUUID().toString());
    }
}
