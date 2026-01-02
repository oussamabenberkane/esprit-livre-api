package com.oussamabenberkane.espritlivre.repository;

import com.oussamabenberkane.espritlivre.domain.Tag;
import com.oussamabenberkane.espritlivre.service.specs.TagSpecifications;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Tag entity.
 *
 * When extending this class, extend TagRepositoryWithBagRelationships too.
 * For more information refer to https://github.com/jhipster/generator-jhipster/issues/17990.
 */
@Repository
public interface TagRepository extends TagRepositoryWithBagRelationships, JpaRepository<Tag, Long>, JpaSpecificationExecutor<Tag> {

    /**
     * Override findAll() to only return active (non-soft-deleted) tags.
     */
    @Override
    default List<Tag> findAll() {
        return findAll(TagSpecifications.activeOnly());
    }

    /**
     * Override findAll(Pageable) to only return active (non-soft-deleted) tags.
     */
    @Override
    default Page<Tag> findAll(Pageable pageable) {
        return findAll(TagSpecifications.activeOnly(), pageable);
    }

    /**
     * Override findById() to only return active (non-soft-deleted) tags.
     */
    @Override
    default Optional<Tag> findById(Long id) {
        return findOne(TagSpecifications.activeOnly().and((root, query, builder) ->
            builder.equal(root.get("id"), id)));
    }

    default Optional<Tag> findOneWithEagerRelationships(Long id) {
        return this.fetchBagRelationships(this.findById(id));
    }

    default List<Tag> findAllWithEagerRelationships() {
        return this.fetchBagRelationships(this.findAll());
    }

    default Page<Tag> findAllWithEagerRelationships(Pageable pageable) {
        return this.fetchBagRelationships(this.findAll(pageable));
    }
}
