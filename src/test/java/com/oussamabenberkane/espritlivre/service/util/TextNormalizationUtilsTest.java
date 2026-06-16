package com.oussamabenberkane.espritlivre.service.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TextNormalizationUtils#stripBookFormatKeywords(String)}.
 *
 * <p>Reproduces the empty-results bug: searching "La femme de ménage-broché" returned 0 results
 * while "La femme de ménage" returned 5. After accent/hyphen normalization the first query becomes
 * "la femme de menage broche", but "broché" (paperback) is a binding format appended by external
 * catalog feeds and is not part of any stored title, so the contiguous LIKE match found nothing.
 */
class TextNormalizationUtilsTest {

    @Test
    void stripsTrailingBrocheFormat() {
        String normalized = TextNormalizationUtils.normalizeForSearch("La femme de ménage-broché");
        assertThat(TextNormalizationUtils.stripBookFormatKeywords(normalized)).isEqualTo("la femme de menage");
    }

    @Test
    void leavesPlainTitleUnchanged() {
        String normalized = TextNormalizationUtils.normalizeForSearch("La femme de ménage");
        assertThat(TextNormalizationUtils.stripBookFormatKeywords(normalized)).isEqualTo("la femme de menage");
    }

    @Test
    void stripsOtherCommonFrenchFormats() {
        assertThat(TextNormalizationUtils.stripBookFormatKeywords("le petit prince relie")).isEqualTo("le petit prince");
        assertThat(TextNormalizationUtils.stripBookFormatKeywords("le petit prince poche")).isEqualTo("le petit prince");
        assertThat(TextNormalizationUtils.stripBookFormatKeywords("titre cartonne")).isEqualTo("titre");
    }

    @Test
    void stripsMultipleTrailingFormatTokens() {
        assertThat(TextNormalizationUtils.stripBookFormatKeywords("un titre broche relie")).isEqualTo("un titre");
    }

    @Test
    void keepsFormatWordWhenItIsTheOnlyToken() {
        // A deliberate one-word search must not be reduced to nothing.
        assertThat(TextNormalizationUtils.stripBookFormatKeywords("broche")).isEqualTo("broche");
    }

    @Test
    void doesNotStripFormatWordInTheMiddle() {
        // Only trailing descriptors are stripped; an interior word stays.
        assertThat(TextNormalizationUtils.stripBookFormatKeywords("broche du roi")).isEqualTo("broche du roi");
    }

    @Test
    void handlesNullAndBlank() {
        assertThat(TextNormalizationUtils.stripBookFormatKeywords(null)).isEmpty();
        assertThat(TextNormalizationUtils.stripBookFormatKeywords("   ")).isBlank();
    }
}
