package com.oussamabenberkane.espritlivre.repository;

import com.oussamabenberkane.espritlivre.domain.Like;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Like entity.
 */
@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
    @Query("select jhiLike from Like jhiLike where jhiLike.user.login = ?#{authentication.name}")
    List<Like> findByUserIsCurrentUser();

    @Query("select count(jhiLike) from Like jhiLike where jhiLike.book.id = :bookId")
    Long countByBookId(@Param("bookId") Long bookId);

    @Query("select case when count(jhiLike) > 0 then true else false end from Like jhiLike where jhiLike.book.id = :bookId and jhiLike.user.login = ?#{authentication.name}")
    Boolean existsByBookIdAndCurrentUser(@Param("bookId") Long bookId);

    @Query("select jhiLike from Like jhiLike where jhiLike.book.id = :bookId and jhiLike.user.login = ?#{authentication.name}")
    Optional<Like> findByBookIdAndCurrentUser(@Param("bookId") Long bookId);

    /**
     * Count likes for multiple books in a single query
     */
    @Query("select l.book.id, count(l) from Like l where l.book.id in :bookIds group by l.book.id")
    List<Object[]> countByBookIds(@Param("bookIds") List<Long> bookIds);

    /**
     * Check which books are liked by current user from a list of book IDs
     */
    @Query("select l.book.id from Like l where l.book.id in :bookIds and l.user.login = ?#{authentication.name}")
    List<Long> findBookIdsLikedByCurrentUser(@Param("bookIds") List<Long> bookIds);

    default Optional<Like> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Like> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Like> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(value = "select jhiLike from Like jhiLike left join fetch jhiLike.user", countQuery = "select count(jhiLike) from Like jhiLike")
    Page<Like> findAllWithToOneRelationships(Pageable pageable);

    @Query("select jhiLike from Like jhiLike left join fetch jhiLike.user")
    List<Like> findAllWithToOneRelationships();

    @Query("select jhiLike from Like jhiLike left join fetch jhiLike.user where jhiLike.id =:id")
    Optional<Like> findOneWithToOneRelationships(@Param("id") Long id);
}
