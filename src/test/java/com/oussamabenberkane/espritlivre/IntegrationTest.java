package com.oussamabenberkane.espritlivre;

import com.oussamabenberkane.espritlivre.config.AsyncSyncConfiguration;
import com.oussamabenberkane.espritlivre.config.EmbeddedSQL;
import com.oussamabenberkane.espritlivre.config.JacksonConfiguration;
import com.oussamabenberkane.espritlivre.config.TestSecurityConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Base composite annotation for integration tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
    classes = { EspritLivreApp.class, JacksonConfiguration.class, AsyncSyncConfiguration.class, TestSecurityConfiguration.class }
)
@EmbeddedSQL
public @interface IntegrationTest {
}
