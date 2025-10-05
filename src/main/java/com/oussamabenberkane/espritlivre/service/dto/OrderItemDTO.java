package com.oussamabenberkane.espritlivre.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * A DTO for the {@link com.oussamabenberkane.espritlivre.domain.OrderItem} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OrderItemDTO implements Serializable {

    private Long id;

    @NotNull
    @Min(1)
    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal totalPrice;

    private OrderDTO order;

    @JsonIgnore
    private BookDTO book;

    @NotNull
    private Long bookId;

    private String bookTitle;

    private String bookAuthor;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public OrderDTO getOrder() {
        return order;
    }

    public void setOrder(OrderDTO order) {
        this.order = order;
    }

    public BookDTO getBook() {
        return book;
    }

    public void setBook(BookDTO book) {
        this.book = book;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public String getBookAuthor() {
        return bookAuthor;
    }

    public void setBookAuthor(String bookAuthor) {
        this.bookAuthor = bookAuthor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OrderItemDTO)) {
            return false;
        }

        OrderItemDTO orderItemDTO = (OrderItemDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, orderItemDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OrderItemDTO{" +
            "id=" + getId() +
            ", quantity=" + getQuantity() +
            ", unitPrice=" + getUnitPrice() +
            ", totalPrice=" + getTotalPrice() +
            ", order=" + getOrder() +
            ", book=" + getBook() +
            ", bookId=" + getBookId() +
            ", bookTitle='" + getBookTitle() + "'" +
            ", bookAuthor='" + getBookAuthor() + "'" +
            "}";
    }
}
