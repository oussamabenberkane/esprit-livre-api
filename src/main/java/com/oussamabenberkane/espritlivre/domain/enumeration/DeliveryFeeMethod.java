package com.oussamabenberkane.espritlivre.domain.enumeration;

/**
 * The DeliveryFeeMethod enumeration.
 * Indicates how the delivery fee was calculated for an order.
 */
public enum DeliveryFeeMethod {
    /**
     * Delivery fee was taken from the product's fixed delivery fee field.
     */
    FIXED,

    /**
     * Delivery fee was calculated dynamically via shipping provider API.
     */
    AUTOMATIC,
}
