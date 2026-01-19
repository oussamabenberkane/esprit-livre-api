package com.oussamabenberkane.espritlivre.service.dto.shipping;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing a relay point (stopdesk/center) for shipping providers.
 */
public class RelayPointDTO {

    private String id;
    private String name;
    private String address;
    private String gps;
    private Integer communeId;
    private String communeName;
    private Integer wilayaId;
    private String wilayaName;
    private String provider;

    public RelayPointDTO() {}

    public RelayPointDTO(String id, String name, String address, String gps,
                         Integer communeId, String communeName,
                         Integer wilayaId, String wilayaName, String provider) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.gps = gps;
        this.communeId = communeId;
        this.communeName = communeName;
        this.wilayaId = wilayaId;
        this.wilayaName = wilayaName;
        this.provider = provider;
    }

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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getGps() {
        return gps;
    }

    public void setGps(String gps) {
        this.gps = gps;
    }

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

    public Integer getWilayaId() {
        return wilayaId;
    }

    public void setWilayaId(Integer wilayaId) {
        this.wilayaId = wilayaId;
    }

    public String getWilayaName() {
        return wilayaName;
    }

    public void setWilayaName(String wilayaName) {
        this.wilayaName = wilayaName;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    @Override
    public String toString() {
        return "RelayPointDTO{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", address='" + address + '\'' +
            ", wilayaName='" + wilayaName + '\'' +
            ", communeName='" + communeName + '\'' +
            ", provider='" + provider + '\'' +
            '}';
    }
}
