package com.oussamabenberkane.espritlivre.repository;

import com.oussamabenberkane.espritlivre.domain.Order;
import com.oussamabenberkane.espritlivre.service.specs.OrderSpecifications;
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

    /**
     * Override findAll() to only return active (non-soft-deleted) orders.
     */
    @Override
    default List<Order> findAll() {
        return findAll(OrderSpecifications.activeOnly());
    }

    /**
     * Override findAll(Pageable) to only return active (non-soft-deleted) orders.
     */
    @Override
    default Page<Order> findAll(Pageable pageable) {
        return findAll(OrderSpecifications.activeOnly(), pageable);
    }

    /**
     * Override findById() to only return active (non-soft-deleted) orders.
     */
    @Override
    default Optional<Order> findById(Long id) {
        return findOne(OrderSpecifications.activeOnly().and((root, query, builder) ->
            builder.equal(root.get("id"), id)));
    }

    @Query("select jhiOrder from Order jhiOrder where jhiOrder.user.login = ?#{authentication.name} and jhiOrder.active = true")
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
        value = "select jhiOrder from Order jhiOrder left join fetch jhiOrder.user where jhiOrder.active = true",
        countQuery = "select count(jhiOrder) from Order jhiOrder where jhiOrder.active = true"
    )
    Page<Order> findAllWithToOneRelationships(Pageable pageable);

    @Query("select jhiOrder from Order jhiOrder left join fetch jhiOrder.user where jhiOrder.active = true")
    List<Order> findAllWithToOneRelationships();

    @Query("select jhiOrder from Order jhiOrder left join fetch jhiOrder.user where jhiOrder.id =:id and jhiOrder.active = true")
    Optional<Order> findOneWithToOneRelationships(@Param("id") Long id);

    @Query("select distinct jhiOrder from Order jhiOrder left join fetch jhiOrder.user where jhiOrder.user.login = ?#{authentication.name} and jhiOrder.active = true")
    Page<Order> findByCurrentUser(Specification<Order> spec, Pageable pageable);

    @Query("select jhiOrder from Order jhiOrder where jhiOrder.id = :id and jhiOrder.user.login = ?#{authentication.name} and jhiOrder.active = true")
    Optional<Order> findOneByIdAndCurrentUser(@Param("id") Long id);

    @Query("select o.uniqueId from Order o where o.active = true and o.uniqueId like :prefix% order by o.uniqueId desc")
    List<String> findUniqueIdsByPrefix(@Param("prefix") String prefix);

    default String findMaxUniqueIdByPrefix(String prefix) {
        List<String> uniqueIds = findUniqueIdsByPrefix(prefix);
        return uniqueIds.isEmpty() ? null : uniqueIds.get(0);
    }

    /**
     * Find all orders with no user (guest orders) and matching phone number.
     *
     * @param phone the normalized phone number to search for
     * @return list of guest orders with matching phone number
     */
    @Query("select o from Order o where o.user is null and o.phone = :phone and o.active = true")
    List<Order> findByUserIsNullAndPhone(@Param("phone") String phone);

    /**
     * Find an order by its unique ID (active orders only).
     *
     * @param uniqueId the unique order ID (e.g., "EL-2026-0001")
     * @return the order if found and active
     */
    @Query("select o from Order o where o.uniqueId = :uniqueId and o.active = true")
    Optional<Order> findByUniqueIdAndActiveTrue(@Param("uniqueId") String uniqueId);
}
