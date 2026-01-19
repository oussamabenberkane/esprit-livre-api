package com.oussamabenberkane.espritlivre.service.dto.shipping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DTO for Yalidine batch parcels GET response.
 * Used for fetching multiple parcels in a single request.
 *
 * GET /v1/parcels/?tracking=track1,track2,track3 returns this structure.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class YalidineParcelsResponse {

    @JsonProperty("has_more")
    private Boolean hasMore;

    @JsonProperty("total_data")
    private Integer totalData;

    private List<YalidineParcelStatusResponse> data;

    public YalidineParcelsResponse() {}

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

    public List<YalidineParcelStatusResponse> getData() {
        return data;
    }

    public void setData(List<YalidineParcelStatusResponse> data) {
        this.data = data;
    }
}
