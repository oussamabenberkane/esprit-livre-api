package com.oussamabenberkane.espritlivre.service.dto;

import com.oussamabenberkane.espritlivre.domain.enumeration.DiscountType;
import com.oussamabenberkane.espritlivre.domain.enumeration.Language;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A DTO for the {@link com.oussamabenberkane.espritlivre.domain.Book} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class BookDTO implements Serializable {

    private Long id;

    @NotNull
    private String title;

    private AuthorDTO author;

    private BigDecimal price;

    private Integer stockQuantity;

    private String coverImageUrl;

    private String description;

    private Boolean active;

    private Language language;

    private ZonedDateTime createdAt;

    private ZonedDateTime updatedAt;

    private Set<TagDTO> tags = new HashSet<>();

    private LocalDate preorderDate;

    private BigDecimal deliveryFee;

    private Boolean automaticDeliveryFee;

    private Long likeCount;

    private Boolean isLikedByCurrentUser;

    private Boolean onSale;

    private DiscountType discountType;

    private BigDecimal discountValue;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public AuthorDTO getAuthor() {
        return author;
    }

    public void setAuthor(AuthorDTO author) {
        this.author = author;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<TagDTO> getTags() {
        return tags;
    }

    public void setTags(Set<TagDTO> tags) {
        this.tags = tags;
    }

    public Long getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Long likeCount) {
        this.likeCount = likeCount;
    }

    public Boolean getIsLikedByCurrentUser() {
        return isLikedByCurrentUser;
    }

    public void setIsLikedByCurrentUser(Boolean isLikedByCurrentUser) {
        this.isLikedByCurrentUser = isLikedByCurrentUser;
    }

    public LocalDate getPreorderDate() {
        return preorderDate;
    }

    public void setPreorderDate(LocalDate preorderDate) {
        this.preorderDate = preorderDate;
    }

    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(BigDecimal deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public Boolean getAutomaticDeliveryFee() {
        return automaticDeliveryFee;
    }

    public void setAutomaticDeliveryFee(Boolean automaticDeliveryFee) {
        this.automaticDeliveryFee = automaticDeliveryFee;
    }

    public Boolean getOnSale() {
        return onSale;
    }

    public void setOnSale(Boolean onSale) {
        this.onSale = onSale;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public void setDiscountType(DiscountType discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public BigDecimal getSalePrice() {
        if (!Boolean.TRUE.equals(onSale) || price == null || discountValue == null || discountType == null) {
            return price;
        }
        if (discountType == DiscountType.PERCENTAGE) {
            BigDecimal factor = BigDecimal.ONE.subtract(discountValue.divide(new java.math.BigDecimal("100")));
            return price.multiply(factor).setScale(2, java.math.RoundingMode.HALF_UP);
        }
        // FIXED_AMOUNT
        BigDecimal result = price.subtract(discountValue);
        return result.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : result.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BookDTO)) {
            return false;
        }

        BookDTO bookDTO = (BookDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, bookDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "BookDTO{" +
            "id=" + getId() +
            ", title='" + getTitle() + "'" +
            ", author=" + getAuthor() +
            ", price=" + getPrice() +
            ", stockQuantity=" + getStockQuantity() +
            ", coverImageUrl='" + getCoverImageUrl() + "'" +
            ", description='" + getDescription() + "'" +
            ", active='" + getActive() + "'" +
            ", language='" + getLanguage() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            ", tags=" + getTags() +
            "}";
    }
}
