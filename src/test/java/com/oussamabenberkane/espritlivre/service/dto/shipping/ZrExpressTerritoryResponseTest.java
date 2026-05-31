package com.oussamabenberkane.espritlivre.service.dto.shipping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Locks the ZR Express territory {@code level} values. The live API returns "wilaya"
 * and "commune" (not "city"/"district"); a previous bug compared against the wrong
 * literals so {@code isCity()}/{@code isDistrict()} always returned false, which broke
 * commune disambiguation during parcel creation.
 */
class ZrExpressTerritoryResponseTest {

    private static ZrExpressTerritoryResponse withLevel(String level) {
        ZrExpressTerritoryResponse t = new ZrExpressTerritoryResponse();
        t.setLevel(level);
        return t;
    }

    @Test
    void recognizesWilayaAsCity() {
        assertThat(withLevel("wilaya").isCity()).isTrue();
        assertThat(withLevel("WILAYA").isCity()).isTrue();
        assertThat(withLevel("wilaya").isDistrict()).isFalse();
    }

    @Test
    void recognizesCommuneAsDistrict() {
        assertThat(withLevel("commune").isDistrict()).isTrue();
        assertThat(withLevel("COMMUNE").isDistrict()).isTrue();
        assertThat(withLevel("commune").isCity()).isFalse();
    }

    @Test
    void toleratesLegacyCityDistrictLiterals() {
        assertThat(withLevel("city").isCity()).isTrue();
        assertThat(withLevel("district").isDistrict()).isTrue();
    }

    @Test
    void nullLevelIsNeitherCityNorDistrict() {
        assertThat(withLevel(null).isCity()).isFalse();
        assertThat(withLevel(null).isDistrict()).isFalse();
    }
}
