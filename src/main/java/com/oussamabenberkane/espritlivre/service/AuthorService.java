package com.oussamabenberkane.espritlivre.service;

import com.oussamabenberkane.espritlivre.domain.Author;
import com.oussamabenberkane.espritlivre.repository.AuthorRepository;
import com.oussamabenberkane.espritlivre.service.dto.AuthorDTO;
import com.oussamabenberkane.espritlivre.service.mapper.AuthorMapper;
import com.oussamabenberkane.espritlivre.service.specs.AuthorSpecifications;
import com.oussamabenberkane.espritlivre.web.rest.errors.BadRequestAlertException;

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
 * Service Implementation for managing {@link com.oussamabenberkane.espritlivre.domain.Author}.
 */
@Service
@Transactional
public class AuthorService {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorService.class);

    private final AuthorRepository authorRepository;

    private final AuthorMapper authorMapper;

    public AuthorService(AuthorRepository authorRepository, AuthorMapper authorMapper) {
        this.authorRepository = authorRepository;
        this.authorMapper = authorMapper;
    }

    /**
     * Save a author.
     *
     * @param authorDTO the entity to save.
     * @return the persisted entity.
     */
    public AuthorDTO save(AuthorDTO authorDTO) {
        LOG.debug("Request to save Author : {}", authorDTO);

        Author author = authorMapper.toEntity(authorDTO);
        author = authorRepository.save(author);
        return authorMapper.toDto(author);
    }

    /**
     * Update a author.
     *
     * @param authorDTO the entity to save.
     * @return the persisted entity.
     */
    public AuthorDTO update(AuthorDTO authorDTO) {
        LOG.debug("Request to update Author : {}", authorDTO);

        if (authorDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", "author", "idnull");
        }

        // Validate author exists
        Author existingAuthor = authorRepository.findById(authorDTO.getId())
            .orElseThrow(() -> new BadRequestAlertException("Entity not found", "author", "idnotfound"));

        // Validate required fields
        if (authorDTO.getName() == null || authorDTO.getName().trim().isEmpty()) {
            throw new BadRequestAlertException("Author name is required", "author", "namerequired");
        }

        // Update fields
        existingAuthor.setName(authorDTO.getName());

        Author updatedAuthor = authorRepository.save(existingAuthor);
        return authorMapper.toDto(updatedAuthor);
    }

    /**
     * Get all authors with filters and pagination.
     *
     * @param pageable the pagination information.
     * @param search the search term for author name.
     * @return the page of entities.
     */
    @Transactional(readOnly = true)
    public Page<AuthorDTO> findAllWithFilters(Pageable pageable, String search) {
        LOG.debug("Request to get all Authors with search: {}", search);

        Specification<Author> spec = Specification.where(null);

        if (search != null && !search.trim().isEmpty()) {
            spec = spec.and(AuthorSpecifications.searchByName(search));
        }

        return authorRepository.findAll(spec, pageable).map(authorMapper::toDto);
    }

    /**
     * Get one author by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<AuthorDTO> findOne(Long id) {
        LOG.debug("Request to get Author : {}", id);
        return authorRepository.findById(id).map(authorMapper::toDto);
    }

    /**
     * Delete the author by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete Author : {}", id);

        Author author = authorRepository.findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Entity not found", "author", "idnotfound"));

        // Check if author has any books
        if (author.getBooks() != null && !author.getBooks().isEmpty()) {
            throw new BadRequestAlertException(
                "Cannot delete author that has books. Remove or reassign books first.",
                "author",
                "authorinuse"
            );
        }

        authorRepository.deleteById(id);
    }

    /**
     * Get top 10 authors with most books.
     *
     * @return the list of authors.
     */
    @Transactional(readOnly = true)
    public List<AuthorDTO> findTop10AuthorsByBookCount() {
        LOG.debug("Request to get top 10 authors by book count");
        List<Author> authors = authorRepository.findTop10AuthorsByBookCount();
        return authors.stream()
            .limit(10)
            .map(authorMapper::toDto)
            .toList();
    }

    /**
     * Find or create author by name.
     *
     * @param name the author name.
     * @return the author entity.
     */
    @Transactional
    public AuthorDTO findOrCreateByName(String name) {
        LOG.debug("Request to find or create Author by name: {}", name);
        return authorMapper.toDto(
            authorRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> {
                    Author newAuthor = new Author();
                    newAuthor.setName(name);
                    return authorRepository.save(newAuthor);
                })
        );
    }
}
