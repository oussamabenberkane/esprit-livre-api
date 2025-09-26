package com.oussamabenberkane.espritlivre.service;

import com.oussamabenberkane.espritlivre.domain.Tag;
import com.oussamabenberkane.espritlivre.domain.enumeration.TagType;
import com.oussamabenberkane.espritlivre.repository.TagRepository;
import com.oussamabenberkane.espritlivre.service.dto.TagDTO;
import com.oussamabenberkane.espritlivre.service.mapper.TagMapper;
import com.oussamabenberkane.espritlivre.service.specs.TagSpecifications;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.oussamabenberkane.espritlivre.domain.Tag}.
 */
@Service
@Transactional
public class TagService {

    private static final Logger LOG = LoggerFactory.getLogger(TagService.class);

    private final TagRepository tagRepository;

    private final TagMapper tagMapper;

    public TagService(TagRepository tagRepository, TagMapper tagMapper) {
        this.tagRepository = tagRepository;
        this.tagMapper = tagMapper;
    }

    /**
     * Save a tag.
     *
     * @param tagDTO the entity to save.
     * @return the persisted entity.
     */
    public TagDTO save(TagDTO tagDTO) {
        LOG.debug("Request to save Tag : {}", tagDTO);
        Tag tag = tagMapper.toEntity(tagDTO);
        tag = tagRepository.save(tag);
        return tagMapper.toDto(tag);
    }

    /**
     * Update a tag.
     *
     * @param tagDTO the entity to save.
     * @return the persisted entity.
     */
    public TagDTO update(TagDTO tagDTO) {
        LOG.debug("Request to update Tag : {}", tagDTO);
        Tag tag = tagMapper.toEntity(tagDTO);
        tag = tagRepository.save(tag);
        return tagMapper.toDto(tag);
    }

    /**
     * Get all the tags.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<TagDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all Tags");
        return tagRepository.findAll(pageable).map(tagMapper::toDto);
    }

    /**
     * Get all active tags by type.
     *
     * @param pageable the pagination information.
     * @param type the tag type to filter by.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<TagDTO> findAll(Pageable pageable, TagType type) {
        LOG.debug("Request to get all Tags with type: {}", type);

        Specification<Tag> spec = Specification.where(TagSpecifications.isActive());

        if (type != null) {
            spec = spec.and(TagSpecifications.hasType(type));
        }

        return tagRepository.findAll(spec, pageable).map(tagMapper::toDto);
    }

    /**
     * Get all active tags by type (no pagination).
     *
     * @param type the tag type to filter by.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<TagDTO> findAllByType(TagType type) {
        LOG.debug("Request to get all Tags with type: {}", type);

        Specification<Tag> spec = Specification.where(TagSpecifications.isActive());

        if (type != null) {
            spec = spec.and(TagSpecifications.hasType(type));
        }

        return tagRepository.findAll(spec).stream()
            .map(tagMapper::toDto)
            .toList();
    }

    /**
     * Get all the tags with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<TagDTO> findAllWithEagerRelationships(Pageable pageable) {
        return tagRepository.findAllWithEagerRelationships(pageable).map(tagMapper::toDto);
    }

    /**
     * Get one tag by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<TagDTO> findOne(Long id) {
        LOG.debug("Request to get Tag : {}", id);
        return tagRepository.findOneWithEagerRelationships(id).map(tagMapper::toDto);
    }

    /**
     * Delete the tag by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete Tag : {}", id);
        tagRepository.deleteById(id);
    }
}
