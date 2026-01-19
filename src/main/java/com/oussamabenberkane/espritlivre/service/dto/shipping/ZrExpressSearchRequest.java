package com.oussamabenberkane.espritlivre.service.dto.shipping;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/**
 * Generic search request for ZR Express API.
 * Used for territory search, hub search, etc.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ZrExpressSearchRequest {

    private String keyword;
    private Map<String, Object> filters;
    private Integer page;
    private Integer pageSize;

    public ZrExpressSearchRequest() {}

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Map<String, Object> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, Object> filters) {
        this.filters = filters;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Builder for creating search requests.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ZrExpressSearchRequest request = new ZrExpressSearchRequest();

        public Builder keyword(String keyword) {
            request.keyword = keyword;
            return this;
        }

        public Builder filters(Map<String, Object> filters) {
            request.filters = filters;
            return this;
        }

        public Builder page(Integer page) {
            request.page = page;
            return this;
        }

        public Builder pageSize(Integer pageSize) {
            request.pageSize = pageSize;
            return this;
        }

        public ZrExpressSearchRequest build() {
            return request;
        }
    }
}
