package com.oussamabenberkane.espritlivre.service.dto.shipping;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response DTO for Yalidine centers API.
 */
public class YalidineCentersResponse {

    @JsonProperty("has_more")
    private Boolean hasMore;

    @JsonProperty("total_data")
    private Integer totalData;

    private List<YalidineCenter> data;

    public Boolean getHasMore() {
        return hasMore;
    }

    public void setHasMore(Boolean hasMore) {
        this.hasMore = hasMore;
    }

    public Integer getTotalData() {
        return totalData;
    }

    public void setTotalData(Integer totalData) {
        this.totalData = totalData;
    }

    public List<YalidineCenter> getData() {
        return data;
    }

    public void setData(List<YalidineCenter> data) {
        this.data = data;
    }

    public static class YalidineCenter {

        @JsonProperty("center_id")
        private Integer centerId;

        private String name;

        private String address;

        private String gps;

        @JsonProperty("commune_id")
        private Integer communeId;

        @JsonProperty("commune_name")
        private String communeName;

        @JsonProperty("wilaya_id")
        private Integer wilayaId;

        @JsonProperty("wilaya_name")
        private String wilayaName;

        public Integer getCenterId() {
            return centerId;
        }

        public void setCenterId(Integer centerId) {
            this.centerId = centerId;
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

        /**
         * Convert to RelayPointDTO.
         */
        public RelayPointDTO toRelayPointDTO() {
            return new RelayPointDTO(
                String.valueOf(centerId),
                name,
                address,
                gps,
                communeId,
                communeName,
                wilayaId,
                wilayaName,
                "YALIDINE"
            );
        }
    }
}
