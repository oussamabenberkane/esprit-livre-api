package com.oussamabenberkane.espritlivre.service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.domain.Page;

/**
 * Wrapper response for paginated orders that includes status refresh metadata.
 * Contains the page of orders plus counts of how many orders had their status
 * refreshed from each shipping provider.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderPageResponse {

    private Page<OrderDTO> page;
    private StatusRefreshInfo statusRefreshInfo;

    public OrderPageResponse() {}

    public OrderPageResponse(Page<OrderDTO> page, StatusRefreshInfo statusRefreshInfo) {
        this.page = page;
        this.statusRefreshInfo = statusRefreshInfo;
    }

    public static OrderPageResponse of(Page<OrderDTO> page) {
        return new OrderPageResponse(page, null);
    }

    public static OrderPageResponse of(Page<OrderDTO> page, StatusRefreshInfo statusRefreshInfo) {
        return new OrderPageResponse(page, statusRefreshInfo);
    }

    public Page<OrderDTO> getPage() {
        return page;
    }

    public void setPage(Page<OrderDTO> page) {
        this.page = page;
    }

    public StatusRefreshInfo getStatusRefreshInfo() {
        return statusRefreshInfo;
    }

    public void setStatusRefreshInfo(StatusRefreshInfo statusRefreshInfo) {
        this.statusRefreshInfo = statusRefreshInfo;
    }

    /**
     * Information about status refreshes from shipping providers.
     */
    public static class StatusRefreshInfo {
        private int yalidineRefreshed;
        private int zrExpressRefreshed;
        private int totalRefreshed;

        public StatusRefreshInfo() {}

        public StatusRefreshInfo(int yalidineRefreshed, int zrExpressRefreshed) {
            this.yalidineRefreshed = yalidineRefreshed;
            this.zrExpressRefreshed = zrExpressRefreshed;
            this.totalRefreshed = yalidineRefreshed + zrExpressRefreshed;
        }

        public int getYalidineRefreshed() {
            return yalidineRefreshed;
        }

        public void setYalidineRefreshed(int yalidineRefreshed) {
            this.yalidineRefreshed = yalidineRefreshed;
            this.totalRefreshed = this.yalidineRefreshed + this.zrExpressRefreshed;
        }

        public int getZrExpressRefreshed() {
            return zrExpressRefreshed;
        }

        public void setZrExpressRefreshed(int zrExpressRefreshed) {
            this.zrExpressRefreshed = zrExpressRefreshed;
            this.totalRefreshed = this.yalidineRefreshed + this.zrExpressRefreshed;
        }

        public int getTotalRefreshed() {
            return totalRefreshed;
        }

        public void setTotalRefreshed(int totalRefreshed) {
            this.totalRefreshed = totalRefreshed;
        }

        public boolean hasRefreshes() {
            return totalRefreshed > 0;
        }
    }
}
