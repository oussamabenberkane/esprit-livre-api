package com.oussamabenberkane.espritlivre.domain;

import static com.oussamabenberkane.espritlivre.domain.BookTestSamples.*;
import static com.oussamabenberkane.espritlivre.domain.TagTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.oussamabenberkane.espritlivre.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BookTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Book.class);
        Book book1 = getBookSample1();
        Book book2 = new Book();
        assertThat(book1).isNotEqualTo(book2);

        book2.setId(book1.getId());
        assertThat(book1).isEqualTo(book2);

        book2 = getBookSample2();
        assertThat(book1).isNotEqualTo(book2);
    }

    @Test
    void tagTest() {
        Book book = getBookRandomSampleGenerator();
        Tag tagBack = getTagRandomSampleGenerator();

        book.addTag(tagBack);
        assertThat(book.getTags()).containsOnly(tagBack);
        assertThat(tagBack.getBooks()).containsOnly(book);

        book.removeTag(tagBack);
        assertThat(book.getTags()).doesNotContain(tagBack);
        assertThat(tagBack.getBooks()).doesNotContain(book);

        book.tags(new HashSet<>(Set.of(tagBack)));
        assertThat(book.getTags()).containsOnly(tagBack);
        assertThat(tagBack.getBooks()).containsOnly(book);

        book.setTags(new HashSet<>());
        assertThat(book.getTags()).doesNotContain(tagBack);
        assertThat(tagBack.getBooks()).doesNotContain(book);
    }
}
