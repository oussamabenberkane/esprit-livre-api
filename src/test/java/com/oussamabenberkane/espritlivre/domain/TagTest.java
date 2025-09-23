package com.oussamabenberkane.espritlivre.domain;

import static com.oussamabenberkane.espritlivre.domain.BookTestSamples.*;
import static com.oussamabenberkane.espritlivre.domain.TagTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.oussamabenberkane.espritlivre.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TagTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Tag.class);
        Tag tag1 = getTagSample1();
        Tag tag2 = new Tag();
        assertThat(tag1).isNotEqualTo(tag2);

        tag2.setId(tag1.getId());
        assertThat(tag1).isEqualTo(tag2);

        tag2 = getTagSample2();
        assertThat(tag1).isNotEqualTo(tag2);
    }

    @Test
    void bookTest() {
        Tag tag = getTagRandomSampleGenerator();
        Book bookBack = getBookRandomSampleGenerator();

        tag.addBook(bookBack);
        assertThat(tag.getBooks()).containsOnly(bookBack);

        tag.removeBook(bookBack);
        assertThat(tag.getBooks()).doesNotContain(bookBack);

        tag.books(new HashSet<>(Set.of(bookBack)));
        assertThat(tag.getBooks()).containsOnly(bookBack);

        tag.setBooks(new HashSet<>());
        assertThat(tag.getBooks()).doesNotContain(bookBack);
    }
}
