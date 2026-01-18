package com.oussamabenberkane.espritlivre.service.dto.shipping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO for ZR Express hub (relay point/pickup point) response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ZrExpressHubResponse {

    private String id;
    private String name;
    private String type;
    private Boolean isPickupPoint;
    private ZrHubAddress address;
    private ZrHubPhone phone;
    private String openingHours;

    public ZrExpressHubResponse() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getIsPickupPoint() {
        return isPickupPoint;
    }

    public void setIsPickupPoint(Boolean isPickupPoint) {
        this.isPickupPoint = isPickupPoint;
    }

    public ZrHubAddress getAddress() {
        return address;
    }

    public void setAddress(ZrHubAddress address) {
        this.address = address;
    }

    public ZrHubPhone getPhone() {
        return phone;
    }

    public void setPhone(ZrHubPhone phone) {
        this.phone = phone;
    }

    public String getOpeningHours() {
        return openingHours;
    }

    public void setOpeningHours(String openingHours) {
        this.openingHours = openingHours;
    }

    /**
     * Convert to generic RelayPointDTO.
     */
    public RelayPointDTO toRelayPointDTO() {
        RelayPointDTO dto = new RelayPointDTO();
        dto.setId(this.id);
        dto.setName(this.name);
        dto.setProvider("ZR");

        if (this.address != null) {
            dto.setAddress(this.address.getStreet());
            // ZR Express uses "city" for wilaya name and "district" for commune name
            dto.setWilayaName(this.address.getCity());
            dto.setCommuneName(this.address.getDistrict());

            // Coordinates are nested inside address
            if (this.address.getCoordinates() != null) {
                dto.setGps(this.address.getCoordinates().getLat() + "," + this.address.getCoordinates().getLng());
            }
        }

        return dto;
    }

    /**
     * Nested class for hub address.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ZrHubAddress {
        private String street;
        private String city;              // Wilaya name
        private String cityTerritoryId;
        private String district;          // Commune name
        private String districtTerritoryId;
        private String postalCode;
        private String country;
        private ZrCoordinates coordinates;

        public ZrHubAddress() {}

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getCityTerritoryId() {
            return cityTerritoryId;
        }

        public void setCityTerritoryId(String cityTerritoryId) {
            this.cityTerritoryId = cityTerritoryId;
        }

        public String getDistrict() {
            return district;
        }

        public void setDistrict(String district) {
            this.district = district;
        }

        public String getDistrictTerritoryId() {
            return districtTerritoryId;
        }

        public void setDistrictTerritoryId(String districtTerritoryId) {
            this.districtTerritoryId = districtTerritoryId;
        }

        public String getPostalCode() {
            return postalCode;
        }

        public void setPostalCode(String postalCode) {
            this.postalCode = postalCode;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public ZrCoordinates getCoordinates() {
            return coordinates;
        }

        public void setCoordinates(ZrCoordinates coordinates) {
            this.coordinates = coordinates;
        }
    }

    /**
     * Nested class for hub phone.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ZrHubPhone {
        private String number1;
        private String number2;

        public ZrHubPhone() {}

        public String getNumber1() {
            return number1;
        }

        public void setNumber1(String number1) {
            this.number1 = number1;
        }

        public String getNumber2() {
            return number2;
        }

        public void setNumber2(String number2) {
            this.number2 = number2;
        }
    }

    /**
     * Nested class for coordinates.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ZrCoordinates {
        private Double lat;
        private Double lng;

        public ZrCoordinates() {}

        public Double getLat() {
            return lat;
        }

        public void setLat(Double lat) {
            this.lat = lat;
        }

        public Double getLng() {
            return lng;
        }

        public void setLng(Double lng) {
            this.lng = lng;
        }
    }
}
