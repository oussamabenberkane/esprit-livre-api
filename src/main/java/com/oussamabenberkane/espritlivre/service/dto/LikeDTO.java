package com.oussamabenberkane.espritlivre.service.dto;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * A DTO for the {@link com.oussamabenberkane.espritlivre.domain.Like} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class LikeDTO implements Serializable {

    private Long id;

    private ZonedDateTime createdAt;

    private UserDTO user;

    private BookDTO book;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    public BookDTO getBook() {
        return book;
    }

    public void setBook(BookDTO book) {
        this.book = book;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LikeDTO)) {
            return false;
        }

        LikeDTO likeDTO = (LikeDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, likeDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "LikeDTO{" +
            "id=" + getId() +
            ", createdAt='" + getCreatedAt() + "'" +
            ", user=" + getUser() +
            ", book=" + getBook() +
            "}";
    }
}
