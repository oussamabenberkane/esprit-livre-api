package com.oussamabenberkane.espritlivre.service.dto.shipping;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ZrExpressRateResponse#getDeliveryFee(boolean)}.
 *
 * <p>Reproduces the wrong-price bug: the real ZR Express API returns deliveryType
 * values "pickup-point", "home" and "return" (not "PickupPoint"/"HomeDelivery"), so
 * the old code never matched and silently fell back to the first list entry — quoting
 * the pickup-point price for home deliveries.
 */
class ZrExpressRateResponseTest {

    private static ZrExpressRateResponse.DeliveryPrice price(String type, Double base, Double discounted) {
        ZrExpressRateResponse.DeliveryPrice p = new ZrExpressRateResponse.DeliveryPrice();
        p.setDeliveryType(type);
        p.setPrice(base);
        p.setDiscountedPrice(discounted);
        return p;
    }

    private static ZrExpressRateResponse response(ZrExpressRateResponse.DeliveryPrice... prices) {
        ZrExpressRateResponse r = new ZrExpressRateResponse();
        r.setDeliveryPrices(List.of(prices));
        return r;
    }

    @Test
    void homeDeliveryReturnsHomePriceNotFirstEntry() {
        // Real ZR ordering: pickup-point first, then home, then return.
        ZrExpressRateResponse r = response(
            price("pickup-point", 470.0, null),
            price("home", 600.0, null),
            price("return", 200.0, null)
        );
        assertThat(r.getDeliveryFee(false)).isEqualByComparingTo(BigDecimal.valueOf(600));
    }

    @Test
    void pickupReturnsPickupPriceRegardlessOfOrder() {
        ZrExpressRateResponse r = response(
            price("home", 600.0, null),
            price("pickup-point", 470.0, null),
            price("return", 200.0, null)
        );
        assertThat(r.getDeliveryFee(true)).isEqualByComparingTo(BigDecimal.valueOf(470));
    }

    @Test
    void prefersDiscountedPriceWhenPresent() {
        ZrExpressRateResponse r = response(
            price("pickup-point", 470.0, 400.0),
            price("home", 600.0, 500.0)
        );
        assertThat(r.getDeliveryFee(false)).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(r.getDeliveryFee(true)).isEqualByComparingTo(BigDecimal.valueOf(400));
    }

    @Test
    void neverFallsBackToReturnOrWrongType() {
        ZrExpressRateResponse r = response(price("return", 200.0, null));
        assertThat(r.getDeliveryFee(false)).isNull();
        assertThat(r.getDeliveryFee(true)).isNull();
    }

    @Test
    void emptyOrNullPricesReturnNull() {
        assertThat(new ZrExpressRateResponse().getDeliveryFee(false)).isNull();
        assertThat(response().getDeliveryFee(true)).isNull();
    }
}
