package com.oussamabenberkane.espritlivre.service;

import com.oussamabenberkane.espritlivre.domain.Book;
import com.oussamabenberkane.espritlivre.domain.Tag;
import com.oussamabenberkane.espritlivre.domain.enumeration.TagType;
import com.oussamabenberkane.espritlivre.repository.BookRepository;
import com.oussamabenberkane.espritlivre.repository.TagRepository;
import com.oussamabenberkane.espritlivre.service.dto.TagDTO;
import com.oussamabenberkane.espritlivre.service.mapper.TagMapper;
import com.oussamabenberkane.espritlivre.service.specs.TagSpecifications;
import com.oussamabenberkane.espritlivre.web.rest.errors.BadRequestAlertException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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

    private final BookRepository bookRepository;

    public TagService(TagRepository tagRepository, TagMapper tagMapper, BookRepository bookRepository) {
        this.tagRepository = tagRepository;
        this.tagMapper = tagMapper;
        this.bookRepository = bookRepository;
    }

    /**
     * Save a tag.
     *
     * @param tagDTO the entity to save.
     * @return the persisted entity.
     */
    @CacheEvict(value = {"tags-by-type", "tags-all"}, allEntries = true)
    public TagDTO save(TagDTO tagDTO) {
        LOG.debug("Request to save Tag : {}", tagDTO);

        // Validate required fields
        if (tagDTO.getNameEn() == null || tagDTO.getNameEn().trim().isEmpty()) {
            throw new BadRequestAlertException("English name is required", "tag", "nameenrequired");
        }

        if (tagDTO.getNameFr() == null || tagDTO.getNameFr().trim().isEmpty()) {
            throw new BadRequestAlertException("French name is required", "tag", "namefrrequired");
        }

        if (tagDTO.getType() == null) {
            throw new BadRequestAlertException("Tag type is required", "tag", "typerequired");
        }

        // Set active to true by default if not provided
        if (tagDTO.getActive() == null) {
            tagDTO.setActive(true);
        }

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
    @CacheEvict(value = {"tags-by-type", "tags-all"}, allEntries = true)
    public TagDTO update(TagDTO tagDTO) {
        LOG.debug("Request to update Tag : {}", tagDTO);

        if (tagDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", "tag", "idnull");
        }

        // Validate tag exists
        Tag existingTag = tagRepository.findById(tagDTO.getId())
            .orElseThrow(() -> new BadRequestAlertException("Entity not found", "tag", "idnotfound"));

        // Validate required fields
        if (tagDTO.getNameEn() == null || tagDTO.getNameEn().trim().isEmpty()) {
            throw new BadRequestAlertException("English name is required", "tag", "nameenrequired");
        }

        if (tagDTO.getNameFr() == null || tagDTO.getNameFr().trim().isEmpty()) {
            throw new BadRequestAlertException("French name is required", "tag", "namefrrequired");
        }

        if (tagDTO.getType() == null) {
            throw new BadRequestAlertException("Tag type is required", "tag", "typerequired");
        }

        // Update fields
        existingTag.setNameEn(tagDTO.getNameEn());
        existingTag.setNameFr(tagDTO.getNameFr());
        existingTag.setType(tagDTO.getType());
        existingTag.setActive(tagDTO.getActive() != null ? tagDTO.getActive() : true);

        Tag updatedTag = tagRepository.save(existingTag);
        return tagMapper.toDto(updatedTag);
    }

    /**
     * Get all the tags.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "tags-all", unless = "#result == null")
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
    @Cacheable(value = "tags-by-type", key = "#type + '-' + #pageable.pageNumber + '-' + #pageable.pageSize", unless = "#result == null")
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
    @Cacheable(value = "tags-by-type", key = "'list-' + #type", unless = "#result == null")
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
    @CacheEvict(value = {"tags-by-type", "tags-all"}, allEntries = true)
    public void delete(Long id) {
        LOG.debug("Request to delete Tag : {}", id);

        Tag tag = tagRepository.findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Entity not found", "tag", "idnotfound"));

        // Check if tag is assigned to any books
        if (tag.getBooks() != null && !tag.getBooks().isEmpty()) {
            throw new BadRequestAlertException(
                "Cannot delete tag that is assigned to books. Remove books first.",
                "tag",
                "taginuse"
            );
        }

        tagRepository.deleteById(id);
    }

    /**
     * Add books to a tag.
     *
     * @param tagId the tag ID.
     * @param bookIds the list of book IDs to add.
     * @return the updated tag DTO.
     */
    @CacheEvict(value = {"tags-by-type", "tags-all"}, allEntries = true)
    public TagDTO addBooksToTag(Long tagId, List<Long> bookIds) {
        LOG.debug("Request to add books {} to tag {}", bookIds, tagId);

        Tag tag = tagRepository.findById(tagId)
            .orElseThrow(() -> new BadRequestAlertException("Tag not found", "tag", "idnotfound"));

        for (Long bookId : bookIds) {
            Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BadRequestAlertException("Book not found with id: " + bookId, "book", "booknotfound"));

            // Validate book is active
            if (book.getActive() == null || !book.getActive()) {
                throw new BadRequestAlertException("Cannot assign inactive book with id: " + bookId, "book", "bookinactive");
            }

            // Add book to tag if not already present
            tag.addBook(book);
        }

        tag = tagRepository.save(tag);
        return tagMapper.toDto(tag);
    }

    /**
     * Remove books from a tag.
     *
     * @param tagId the tag ID.
     * @param bookIds the list of book IDs to remove.
     * @return the updated tag DTO.
     */
    @CacheEvict(value = {"tags-by-type", "tags-all"}, allEntries = true)
    public TagDTO removeBooksFromTag(Long tagId, List<Long> bookIds) {
        LOG.debug("Request to remove books {} from tag {}", bookIds, tagId);

        Tag tag = tagRepository.findById(tagId)
            .orElseThrow(() -> new BadRequestAlertException("Tag not found", "tag", "idnotfound"));

        for (Long bookId : bookIds) {
            Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BadRequestAlertException("Book not found with id: " + bookId, "book", "booknotfound"));

            // Remove book from tag
            tag.removeBook(book);
        }

        tag = tagRepository.save(tag);
        return tagMapper.toDto(tag);
    }
}
