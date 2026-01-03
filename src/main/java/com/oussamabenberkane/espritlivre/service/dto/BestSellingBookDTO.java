package com.oussamabenberkane.espritlivre.service.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO for best selling book information.
 */
public class BestSellingBookDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long bookId;
    private String title;
    private Long soldCount;
    private BigDecimal price;

    public BestSellingBookDTO() {}

    public BestSellingBookDTO(Long bookId, String title, Long soldCount, BigDecimal price) {
        this.bookId = bookId;
        this.title = title;
        this.soldCount = soldCount;
        this.price = price;
    }

    // Getters and Setters

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getSoldCount() {
        return soldCount;
    }

    public void setSoldCount(Long soldCount) {
        this.soldCount = soldCount;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "BestSellingBookDTO{" + "bookId=" + bookId + ", title='" + title + '\'' + ", soldCount=" + soldCount + ", price=" + price + '}';
    }
}
