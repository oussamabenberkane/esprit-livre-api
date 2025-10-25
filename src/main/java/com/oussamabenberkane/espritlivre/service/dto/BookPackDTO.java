package com.oussamabenberkane.espritlivre.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A DTO for the {@link com.oussamabenberkane.espritlivre.domain.BookPack} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class BookPackDTO implements Serializable {

    private Long id;

    @NotNull
    private String title;

    private String description;

    private String coverUrl;

    @NotNull
    @DecimalMin(value = "0")
    private BigDecimal price;

    private Instant createdDate;

    private Instant lastModifiedDate;

    @NotNull
    @Size(min = 2)
    private Set<BookDTO> books = new HashSet<>();

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public Set<BookDTO> getBooks() {
        return books;
    }

    public void setBooks(Set<BookDTO> books) {
        this.books = books;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BookPackDTO)) {
            return false;
        }

        BookPackDTO bookPackDTO = (BookPackDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, bookPackDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "BookPackDTO{" +
            "id=" + getId() +
            ", title='" + getTitle() + "'" +
            ", description='" + getDescription() + "'" +
            ", coverUrl='" + getCoverUrl() + "'" +
            ", price=" + getPrice() +
            ", createdDate='" + getCreatedDate() + "'" +
            ", lastModifiedDate='" + getLastModifiedDate() + "'" +
            ", books=" + getBooks() +
            "}";
    }
}
