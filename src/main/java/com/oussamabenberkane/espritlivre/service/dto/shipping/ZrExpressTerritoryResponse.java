package com.oussamabenberkane.espritlivre.service.dto.shipping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO for ZR Express territory (wilaya/commune) search response.
 * ZR Express uses UUIDs for territories instead of numeric IDs.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ZrExpressTerritoryResponse {

    private String id;        // UUID
    private Integer code;     // Numeric code (e.g., wilaya code)
    private String name;
    private String level;     // "wilaya" or "commune" (ZR Express territory level)
    private String parentId;  // Parent territory UUID

    public ZrExpressTerritoryResponse() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    /**
     * Check if this is a city-level territory (wilaya).
     * ZR Express returns "wilaya"; "city" is tolerated for backwards safety.
     */
    public boolean isCity() {
        return "wilaya".equalsIgnoreCase(level) || "city".equalsIgnoreCase(level);
    }

    /**
     * Check if this is a district-level territory (commune).
     * ZR Express returns "commune"; "district" is tolerated for backwards safety.
     */
    public boolean isDistrict() {
        return "commune".equalsIgnoreCase(level) || "district".equalsIgnoreCase(level);
    }

    @Override
    public String toString() {
        return "ZrExpressTerritoryResponse{" +
            "id='" + id + '\'' +
            ", code=" + code +
            ", name='" + name + '\'' +
            ", level='" + level + '\'' +
            ", parentId='" + parentId + '\'' +
            '}';
    }
}
