package com.oussamabenberkane.espritlivre.repository;

import com.oussamabenberkane.espritlivre.domain.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Order entity.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    @Query("select jhiOrder from Order jhiOrder where jhiOrder.user.login = ?#{authentication.name}")
    List<Order> findByUserIsCurrentUser();

    default Optional<Order> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Order> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Order> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select jhiOrder from Order jhiOrder left join fetch jhiOrder.user",
        countQuery = "select count(jhiOrder) from Order jhiOrder"
    )
    Page<Order> findAllWithToOneRelationships(Pageable pageable);

    @Query("select jhiOrder from Order jhiOrder left join fetch jhiOrder.user")
    List<Order> findAllWithToOneRelationships();

    @Query("select jhiOrder from Order jhiOrder left join fetch jhiOrder.user where jhiOrder.id =:id")
    Optional<Order> findOneWithToOneRelationships(@Param("id") Long id);

    @Query("select jhiOrder from Order jhiOrder where jhiOrder.user.login = ?#{authentication.name}")
    Page<Order> findByCurrentUser(Specification<Order> spec, Pageable pageable);

    @Query("select jhiOrder from Order jhiOrder where jhiOrder.id = :id and jhiOrder.user.login = ?#{authentication.name}")
    Optional<Order> findOneByIdAndCurrentUser(@Param("id") Long id);

    @Query("select o.uniqueId from Order o where o.uniqueId like :prefix% order by o.uniqueId desc")
    List<String> findUniqueIdsByPrefix(@Param("prefix") String prefix);

    default String findMaxUniqueIdByPrefix(String prefix) {
        List<String> uniqueIds = findUniqueIdsByPrefix(prefix);
        return uniqueIds.isEmpty() ? null : uniqueIds.get(0);
    }
}
