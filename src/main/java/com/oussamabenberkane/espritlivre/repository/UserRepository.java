package com.oussamabenberkane.espritlivre.repository;

import com.oussamabenberkane.espritlivre.domain.User;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the {@link User} entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {
    String USERS_BY_LOGIN_CACHE = "usersByLogin";

    String USERS_BY_EMAIL_CACHE = "usersByEmail";

    Optional<User> findOneByLogin(String login);

    @EntityGraph(attributePaths = "authorities")
    @Cacheable(cacheNames = USERS_BY_LOGIN_CACHE, unless = "#result == null")
    Optional<User> findOneWithAuthoritiesByLogin(String login);

    @Cacheable(cacheNames = USERS_BY_EMAIL_CACHE, unless = "#result == null")
    Optional<User> findOneByEmailIgnoreCase(String email);

    Page<User> findAllByIdNotNullAndActivatedIsTrue(Pageable pageable);

    @EntityGraph(attributePaths = "authorities")
    @Query("SELECT u FROM User u WHERE u.id IS NOT NULL AND NOT EXISTS " +
           "(SELECT a FROM u.authorities a WHERE a.name = :excludedAuthority)")
    Page<User> findAllByAuthoritiesNotContaining(@Param("excludedAuthority") String excludedAuthority, Pageable pageable);

    @EntityGraph(attributePaths = "authorities")
    @Query("SELECT u FROM User u WHERE u.id IS NOT NULL AND u.activated = :activated AND NOT EXISTS " +
           "(SELECT a FROM u.authorities a WHERE a.name = :excludedAuthority)")
    Page<User> findAllByActivatedAndAuthoritiesNotContaining(
        @Param("activated") Boolean activated,
        @Param("excludedAuthority") String excludedAuthority,
        Pageable pageable
    );
}
