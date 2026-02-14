package com.oussamabenberkane.espritlivre.service.dto.shipping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO for Yalidine fees API response.
 * GET /v1/fees/?from_wilaya_id=X&to_wilaya_id=Y
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class YalidineFeesResponse {

    @JsonProperty("from_wilaya_name")
    private String fromWilayaName;

    @JsonProperty("to_wilaya_name")
    private String toWilayaName;

    private Integer zone;

    @JsonProperty("retour_fee")
    private Integer returnFee;

    @JsonProperty("cod_percentage")
    private Double codPercentage;

    @JsonProperty("insurance_percentage")
    private Double insurancePercentage;

    @JsonProperty("oversize_fee")
    private Integer oversizeFee;

    @JsonProperty("per_commune")
    private Map<String, CommuneFees> perCommune;

    public String getFromWilayaName() {
        return fromWilayaName;
    }

    public void setFromWilayaName(String fromWilayaName) {
        this.fromWilayaName = fromWilayaName;
    }

    public String getToWilayaName() {
        return toWilayaName;
    }

    public void setToWilayaName(String toWilayaName) {
        this.toWilayaName = toWilayaName;
    }

    public Integer getZone() {
        return zone;
    }

    public void setZone(Integer zone) {
        this.zone = zone;
    }

    public Integer getReturnFee() {
        return returnFee;
    }

    public void setReturnFee(Integer returnFee) {
        this.returnFee = returnFee;
    }

    public Double getCodPercentage() {
        return codPercentage;
    }

    public void setCodPercentage(Double codPercentage) {
        this.codPercentage = codPercentage;
    }

    public Double getInsurancePercentage() {
        return insurancePercentage;
    }

    public void setInsurancePercentage(Double insurancePercentage) {
        this.insurancePercentage = insurancePercentage;
    }

    public Integer getOversizeFee() {
        return oversizeFee;
    }

    public void setOversizeFee(Integer oversizeFee) {
        this.oversizeFee = oversizeFee;
    }

    public Map<String, CommuneFees> getPerCommune() {
        return perCommune;
    }

    public void setPerCommune(Map<String, CommuneFees> perCommune) {
        this.perCommune = perCommune;
    }

    /**
     * Get delivery fee for a specific commune.
     *
     * @param communeId The commune ID
     * @param isStopDesk Whether this is a stop desk (relay point) delivery
     * @return The delivery fee, or null if commune not found
     */
    public BigDecimal getDeliveryFee(String communeId, boolean isStopDesk) {
        if (perCommune == null || communeId == null) {
            return null;
        }

        CommuneFees communeFees = perCommune.get(communeId);
        if (communeFees == null) {
            return null;
        }

        Integer fee = isStopDesk ? communeFees.getExpressDesk() : communeFees.getExpressHome();
        return fee != null ? BigDecimal.valueOf(fee) : null;
    }

    /**
     * Get delivery fee for a commune by name (case-insensitive).
     *
     * @param communeName The commune name
     * @param isStopDesk Whether this is a stop desk (relay point) delivery
     * @return The delivery fee, or null if commune not found
     */
    public BigDecimal getDeliveryFeeByName(String communeName, boolean isStopDesk) {
        if (perCommune == null || communeName == null) {
            return null;
        }

        String normalizedName = communeName.toLowerCase().trim();
        for (CommuneFees fees : perCommune.values()) {
            if (fees.getCommuneName() != null &&
                fees.getCommuneName().toLowerCase().trim().equals(normalizedName)) {
                Integer fee = isStopDesk ? fees.getExpressDesk() : fees.getExpressHome();
                return fee != null ? BigDecimal.valueOf(fee) : null;
            }
        }

        // Try partial match
        for (CommuneFees fees : perCommune.values()) {
            if (fees.getCommuneName() != null) {
                String feeName = fees.getCommuneName().toLowerCase().trim();
                if (feeName.contains(normalizedName) || normalizedName.contains(feeName)) {
                    Integer fee = isStopDesk ? fees.getExpressDesk() : fees.getExpressHome();
                    return fee != null ? BigDecimal.valueOf(fee) : null;
                }
            }
        }

        return null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CommuneFees {

        @JsonProperty("commune_id")
        private Integer communeId;

        @JsonProperty("commune_name")
        private String communeName;

        @JsonProperty("express_home")
        private Integer expressHome;

        @JsonProperty("express_desk")
        private Integer expressDesk;

        @JsonProperty("economic_home")
        private Integer economicHome;

        @JsonProperty("economic_desk")
        private Integer economicDesk;

        public Integer getCommuneId() {
            return communeId;
        }

        public void setCommuneId(Integer communeId) {
            this.communeId = communeId;
        }

        public String getCommuneName() {
            return communeName;
        }

        public void setCommuneName(String communeName) {
            this.communeName = communeName;
        }

        public Integer getExpressHome() {
            return expressHome;
        }

        public void setExpressHome(Integer expressHome) {
            this.expressHome = expressHome;
        }

        public Integer getExpressDesk() {
            return expressDesk;
        }

        public void setExpressDesk(Integer expressDesk) {
            this.expressDesk = expressDesk;
        }

        public Integer getEconomicHome() {
            return economicHome;
        }

        public void setEconomicHome(Integer economicHome) {
            this.economicHome = economicHome;
        }

        public Integer getEconomicDesk() {
            return economicDesk;
        }

        public void setEconomicDesk(Integer economicDesk) {
            this.economicDesk = economicDesk;
        }
    }
}
