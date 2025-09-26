package com.oussamabenberkane.espritlivre.service.dto;

import jakarta.validation.constraints.NotNull;

/**
 * A DTO for book search suggestions.
 */
public class BookSuggestionDTO {

    @NotNull
    private String suggestion;

    @NotNull
    private SuggestionType type;

    public BookSuggestionDTO() {}

    public BookSuggestionDTO(String suggestion, SuggestionType type) {
        this.suggestion = suggestion;
        this.type = type;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public SuggestionType getType() {
        return type;
    }

    public void setType(SuggestionType type) {
        this.type = type;
    }

    /**
     * Enumeration for suggestion types
     */
    public enum SuggestionType {
        BOOK_TITLE,
        AUTHOR,
        CATEGORY
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookSuggestionDTO)) return false;

        BookSuggestionDTO that = (BookSuggestionDTO) o;

        if (!suggestion.equals(that.suggestion)) return false;
        return type == that.type;
    }

    @Override
    public int hashCode() {
        int result = suggestion.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "BookSuggestionDTO{" +
            "suggestion='" + suggestion + '\'' +
            ", type=" + type +
            '}';
    }
}