package com.oussamabenberkane.espritlivre.service;

import com.oussamabenberkane.espritlivre.domain.Author;
import com.oussamabenberkane.espritlivre.repository.AuthorRepository;
import com.oussamabenberkane.espritlivre.security.SecurityUtils;
import com.oussamabenberkane.espritlivre.service.dto.AuthorDTO;
import com.oussamabenberkane.espritlivre.service.mapper.AuthorMapper;
import com.oussamabenberkane.espritlivre.service.specs.AuthorSpecifications;
import com.oussamabenberkane.espritlivre.web.rest.errors.BadRequestAlertException;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service Implementation for managing {@link com.oussamabenberkane.espritlivre.domain.Author}.
 */
@Service
@Transactional
public class AuthorService {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorService.class);

    private final AuthorRepository authorRepository;

    private final AuthorMapper authorMapper;

    private final FileStorageService fileStorageService;

    public AuthorService(AuthorRepository authorRepository, AuthorMapper authorMapper, FileStorageService fileStorageService) {
        this.authorRepository = authorRepository;
        this.authorMapper = authorMapper;
        this.fileStorageService = fileStorageService;
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
        if (authorDTO.getProfilePictureUrl() != null) {
            existingAuthor.setProfilePictureUrl(authorDTO.getProfilePictureUrl());
        }

        Author updatedAuthor = authorRepository.save(existingAuthor);
        return authorMapper.toDto(updatedAuthor);
    }

    /**
     * Save or update author with profile picture.
     *
     * @param authorDTO the author data.
     * @param profilePicture the profile picture file (required for create, optional for update).
     * @param isUpdate whether this is an update operation.
     * @return the persisted entity.
     */
    public AuthorDTO saveWithPicture(AuthorDTO authorDTO, MultipartFile profilePicture, boolean isUpdate) {
        LOG.debug("Request to save Author with profile picture : {}", authorDTO);

        Author author;

        if (isUpdate) {
            // Update existing author
            if (authorDTO.getId() == null) {
                throw new BadRequestAlertException("Invalid id", "author", "idnull");
            }
            author = authorRepository.findById(authorDTO.getId())
                .orElseThrow(() -> new BadRequestAlertException("Entity not found", "author", "idnotfound"));
            author.setName(authorDTO.getName());
        } else {
            // Create new author - picture is REQUIRED
            if (authorDTO.getId() != null) {
                throw new BadRequestAlertException("A new author cannot already have an ID", "author", "idexists");
            }
            if (profilePicture == null || profilePicture.isEmpty()) {
                throw new BadRequestAlertException("Profile picture is required for new authors", "author", "picturerequired");
            }
            author = authorMapper.toEntity(authorDTO);
            author = authorRepository.save(author);
        }

        // Handle profile picture if provided
        if (profilePicture != null && !profilePicture.isEmpty()) {
            String oldProfilePictureUrl = author.getProfilePictureUrl();

            try {
                String newProfilePictureUrl = fileStorageService.storeAuthorPicture(profilePicture, author.getId());
                author.setProfilePictureUrl(newProfilePictureUrl);

                // Delete old picture if different
                if (oldProfilePictureUrl != null && !oldProfilePictureUrl.equals(newProfilePictureUrl)) {
                    fileStorageService.deleteAuthorPicture(oldProfilePictureUrl);
                }
            } catch (IOException e) {
                LOG.error("Failed to store profile picture for author", e);
                throw new BadRequestAlertException("Failed to store profile picture: " + e.getMessage(), "author", "filestoragefailed");
            }
        }

        author = authorRepository.save(author);
        return authorMapper.toDto(author);
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
     * Soft delete the author by id (sets active = false, deletedAt and deletedBy).
     * Relationships with books are preserved.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to soft delete Author : {}", id);
        authorRepository.findById(id).ifPresent(author -> {
            author.setActive(false);
            author.setDeletedAt(Instant.now());
            SecurityUtils.getCurrentUserLogin().ifPresent(author::setDeletedBy);
            authorRepository.save(author);
        });
    }

    /**
     * Hard delete the author by id (permanently removes from database).
     * WARNING: This cannot be undone. Only use after cleanup job has nullified FKs.
     *
     * @param id the id of the entity.
     */
    public void deleteForever(Long id) {
        LOG.debug("Request to hard delete Author : {}", id);
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
