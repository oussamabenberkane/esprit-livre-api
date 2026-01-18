package com.oussamabenberkane.espritlivre.service.dto.shipping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO for ZR Express customer response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ZrExpressCustomerResponse {

    private String id;
    private String name;
    private ZrExpressParcelRequest.ZrPhone phone;

    public ZrExpressCustomerResponse() {}

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

    public ZrExpressParcelRequest.ZrPhone getPhone() {
        return phone;
    }

    public void setPhone(ZrExpressParcelRequest.ZrPhone phone) {
        this.phone = phone;
    }
}
