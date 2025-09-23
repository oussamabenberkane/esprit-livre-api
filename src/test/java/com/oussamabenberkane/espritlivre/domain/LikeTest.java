package com.oussamabenberkane.espritlivre.domain;

import static com.oussamabenberkane.espritlivre.domain.BookTestSamples.*;
import static com.oussamabenberkane.espritlivre.domain.LikeTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.oussamabenberkane.espritlivre.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class LikeTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Like.class);
        Like like1 = getLikeSample1();
        Like like2 = new Like();
        assertThat(like1).isNotEqualTo(like2);

        like2.setId(like1.getId());
        assertThat(like1).isEqualTo(like2);

        like2 = getLikeSample2();
        assertThat(like1).isNotEqualTo(like2);
    }

    @Test
    void bookTest() {
        Like like = getLikeRandomSampleGenerator();
        Book bookBack = getBookRandomSampleGenerator();

        like.setBook(bookBack);
        assertThat(like.getBook()).isEqualTo(bookBack);

        like.book(null);
        assertThat(like.getBook()).isNull();
    }
}
