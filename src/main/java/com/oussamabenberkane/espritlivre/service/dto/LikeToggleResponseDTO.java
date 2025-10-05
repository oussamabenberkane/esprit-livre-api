package com.oussamabenberkane.espritlivre.service.dto;

import java.io.Serializable;

/**
 * A DTO for the toggle like response.
 */
public class LikeToggleResponseDTO implements Serializable {

    private Long bookId;
    private Boolean isLiked;
    private Long likeCount;

    public LikeToggleResponseDTO() {}

    public LikeToggleResponseDTO(Long bookId, Boolean isLiked, Long likeCount) {
        this.bookId = bookId;
        this.isLiked = isLiked;
        this.likeCount = likeCount;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public Boolean getIsLiked() {
        return isLiked;
    }

    public void setIsLiked(Boolean isLiked) {
        this.isLiked = isLiked;
    }

    public Long getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Long likeCount) {
        this.likeCount = likeCount;
    }

    @Override
    public String toString() {
        return "LikeToggleResponseDTO{" +
            "bookId=" + bookId +
            ", isLiked=" + isLiked +
            ", likeCount=" + likeCount +
            '}';
    }
}
