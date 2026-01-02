package com.oussamabenberkane.espritlivre.service;

import com.oussamabenberkane.espritlivre.domain.Book;
import com.oussamabenberkane.espritlivre.domain.BookPack;
import com.oussamabenberkane.espritlivre.repository.BookPackRepository;
import com.oussamabenberkane.espritlivre.repository.BookRepository;
import com.oussamabenberkane.espritlivre.security.SecurityUtils;
import com.oussamabenberkane.espritlivre.service.dto.BookPackDTO;
import com.oussamabenberkane.espritlivre.service.mapper.BookPackMapper;
import com.oussamabenberkane.espritlivre.service.specs.BookPackSpecifications;
import com.oussamabenberkane.espritlivre.web.rest.errors.BadRequestAlertException;
import java.io.IOException;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service Implementation for managing {@link com.oussamabenberkane.espritlivre.domain.BookPack}.
 */
@Service
@Transactional
public class BookPackService {

    private static final Logger LOG = LoggerFactory.getLogger(BookPackService.class);

    private final BookPackRepository bookPackRepository;

    private final BookPackMapper bookPackMapper;

    private final BookRepository bookRepository;

    private final FileStorageService fileStorageService;

    public BookPackService(BookPackRepository bookPackRepository, BookPackMapper bookPackMapper, BookRepository bookRepository, FileStorageService fileStorageService) {
        this.bookPackRepository = bookPackRepository;
        this.bookPackMapper = bookPackMapper;
        this.bookRepository = bookRepository;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Save a bookPack.
     *
     * @param bookPackDTO the entity to save.
     * @return the persisted entity.
     */
    public BookPackDTO save(BookPackDTO bookPackDTO) {
        LOG.debug("Request to save BookPack : {}", bookPackDTO);

        // Validate minimum 2 books
        if (bookPackDTO.getBooks() == null || bookPackDTO.getBooks().size() < 2) {
            throw new BadRequestAlertException("Book pack must contain at least 2 books", "bookPack", "minbooks");
        }

        // Validate price is positive
        if (bookPackDTO.getPrice() == null || bookPackDTO.getPrice().signum() < 0) {
            throw new BadRequestAlertException("Price must be a positive number", "bookPack", "invalidprice");
        }

        // Validate title is unique (for new packs)
        if (bookPackDTO.getId() == null) {
            bookPackRepository
                .findAll()
                .stream()
                .filter(pack -> pack.getTitle().equalsIgnoreCase(bookPackDTO.getTitle()))
                .findFirst()
                .ifPresent(pack -> {
                    throw new BadRequestAlertException("Book pack with this title already exists", "bookPack", "titleexists");
                });
        }

        BookPack bookPack = bookPackMapper.toEntity(bookPackDTO);

        // Fetch and set the books
        Set<Book> books = bookPackDTO
            .getBooks()
            .stream()
            .map(bookDTO ->
                bookRepository
                    .findById(bookDTO.getId())
                    .orElseThrow(() -> new BadRequestAlertException("Book not found: " + bookDTO.getId(), "bookPack", "booknotfound"))
            )
            .collect(Collectors.toSet());

        bookPack.setBooks(books);

        bookPack = bookPackRepository.save(bookPack);
        return bookPackMapper.toDto(bookPack);
    }

    /**
     * Update a bookPack.
     *
     * @param bookPackDTO the entity to save.
     * @return the persisted entity.
     */
    public BookPackDTO update(BookPackDTO bookPackDTO) {
        LOG.debug("Request to update BookPack : {}", bookPackDTO);

        // Validate book pack exists
        if (!bookPackRepository.existsById(bookPackDTO.getId())) {
            throw new BadRequestAlertException("Entity not found", "bookPack", "idnotfound");
        }

        // Validate minimum 2 books
        if (bookPackDTO.getBooks() == null || bookPackDTO.getBooks().size() < 2) {
            throw new BadRequestAlertException("Book pack must contain at least 2 books", "bookPack", "minbooks");
        }

        // Validate price is positive
        if (bookPackDTO.getPrice() == null || bookPackDTO.getPrice().signum() < 0) {
            throw new BadRequestAlertException("Price must be a positive number", "bookPack", "invalidprice");
        }

        // Validate title is unique (excluding current pack)
        bookPackRepository
            .findAll()
            .stream()
            .filter(pack -> !pack.getId().equals(bookPackDTO.getId()) && pack.getTitle().equalsIgnoreCase(bookPackDTO.getTitle()))
            .findFirst()
            .ifPresent(pack -> {
                throw new BadRequestAlertException("Book pack with this title already exists", "bookPack", "titleexists");
            });

        BookPack bookPack = bookPackMapper.toEntity(bookPackDTO);

        // Fetch and set the books
        Set<Book> books = bookPackDTO
            .getBooks()
            .stream()
            .map(bookDTO ->
                bookRepository
                    .findById(bookDTO.getId())
                    .orElseThrow(() -> new BadRequestAlertException("Book not found: " + bookDTO.getId(), "bookPack", "booknotfound"))
            )
            .collect(Collectors.toSet());

        bookPack.setBooks(books);

        bookPack = bookPackRepository.save(bookPack);
        return bookPackMapper.toDto(bookPack);
    }

    /**
     * Save or update book pack with cover image.
     *
     * @param bookPackDTO the book pack data.
     * @param coverImage the cover image file (required for create, optional for update).
     * @param isUpdate whether this is an update operation.
     * @return the persisted entity.
     */
    public BookPackDTO saveWithCover(BookPackDTO bookPackDTO, MultipartFile coverImage, boolean isUpdate) {
        LOG.debug("Request to save BookPack with cover image : {}", bookPackDTO);

        // Validate minimum 2 books
        if (bookPackDTO.getBooks() == null || bookPackDTO.getBooks().size() < 2) {
            throw new BadRequestAlertException("Book pack must contain at least 2 books", "bookPack", "minbooks");
        }

        // Validate price is positive
        if (bookPackDTO.getPrice() == null || bookPackDTO.getPrice().signum() < 0) {
            throw new BadRequestAlertException("Price must be a positive number", "bookPack", "invalidprice");
        }

        BookPack bookPack;

        if (isUpdate) {
            // Update existing book pack
            if (bookPackDTO.getId() == null) {
                throw new BadRequestAlertException("Invalid id", "bookPack", "idnull");
            }
            if (!bookPackRepository.existsById(bookPackDTO.getId())) {
                throw new BadRequestAlertException("Entity not found", "bookPack", "idnotfound");
            }

            // Validate title is unique (excluding current pack)
            bookPackRepository
                .findAll()
                .stream()
                .filter(pack -> !pack.getId().equals(bookPackDTO.getId()) && pack.getTitle().equalsIgnoreCase(bookPackDTO.getTitle()))
                .findFirst()
                .ifPresent(pack -> {
                    throw new BadRequestAlertException("Book pack with this title already exists", "bookPack", "titleexists");
                });

            bookPack = bookPackMapper.toEntity(bookPackDTO);
        } else {
            // Create new book pack - cover is REQUIRED
            if (bookPackDTO.getId() != null) {
                throw new BadRequestAlertException("A new book pack cannot already have an ID", "bookPack", "idexists");
            }
            if (coverImage == null || coverImage.isEmpty()) {
                throw new BadRequestAlertException("Cover image is required for new book packs", "bookPack", "coverrequired");
            }

            // Validate title is unique
            bookPackRepository
                .findAll()
                .stream()
                .filter(pack -> pack.getTitle().equalsIgnoreCase(bookPackDTO.getTitle()))
                .findFirst()
                .ifPresent(pack -> {
                    throw new BadRequestAlertException("Book pack with this title already exists", "bookPack", "titleexists");
                });

            bookPack = bookPackMapper.toEntity(bookPackDTO);
        }

        // Fetch and set the books
        Set<Book> books = bookPackDTO
            .getBooks()
            .stream()
            .map(bookDTO ->
                bookRepository
                    .findById(bookDTO.getId())
                    .orElseThrow(() -> new BadRequestAlertException("Book not found: " + bookDTO.getId(), "bookPack", "booknotfound"))
            )
            .collect(Collectors.toSet());

        bookPack.setBooks(books);
        bookPack = bookPackRepository.save(bookPack);

        // Handle cover image if provided
        if (coverImage != null && !coverImage.isEmpty()) {
            String oldCoverUrl = bookPack.getCoverUrl();

            try {
                String newCoverUrl = fileStorageService.storeBookPackCover(coverImage, bookPack.getId());
                bookPack.setCoverUrl(newCoverUrl);

                // Delete old cover if different
                if (oldCoverUrl != null && !oldCoverUrl.equals(newCoverUrl)) {
                    fileStorageService.deleteBookPackCover(oldCoverUrl);
                }
            } catch (IOException e) {
                LOG.error("Failed to store book pack cover", e);
                throw new BadRequestAlertException("Failed to store cover image: " + e.getMessage(), "bookPack", "filestoragefailed");
            }
        }

        bookPack = bookPackRepository.save(bookPack);
        return bookPackMapper.toDto(bookPack);
    }

    /**
     * Get all the bookPacks with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @param search the search term.
     * @param author the author IDs filter.
     * @param minPrice the minimum price filter.
     * @param maxPrice the maximum price filter.
     * @param categoryId the category tag id filter.
     * @param mainDisplayId the main display tag id filter.
     * @param language the language filter.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<BookPackDTO> findAll(
        Pageable pageable,
        String search,
        List<Long> author,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Long categoryId,
        Long mainDisplayId,
        List<String> language
    ) {
        LOG.debug("Request to get all BookPacks with filters - search: {}, author: {}, priceRange: [{}, {}], categoryId: {}, mainDisplayId: {}, language: {}",
            search, author, minPrice, maxPrice, categoryId, mainDisplayId, language);

        Specification<BookPack> spec = Specification.where(BookPackSpecifications.activeOnly());

        // Apply search filter if provided
        if (StringUtils.hasText(search)) {
            spec = spec.and(BookPackSpecifications.searchByText(search));
        }

        // Apply individual filters only if parameters are provided
        if (author != null && !author.isEmpty()) {
            spec = spec.and(BookPackSpecifications.hasAuthor(author));
        }

        if (minPrice != null || maxPrice != null) {
            spec = spec.and(BookPackSpecifications.hasPriceBetween(minPrice, maxPrice));
        }

        // Language filtering
        if (language != null && !language.isEmpty()) {
            spec = spec.and(BookPackSpecifications.hasLanguage(language));
        }

        // Tag filtering - book packs that have books with EITHER category OR mainDisplay tag
        if (categoryId != null || mainDisplayId != null) {
            Specification<BookPack> tagSpec = buildTagSpecification(categoryId, mainDisplayId);
            if (tagSpec != null) {
                spec = spec.and(tagSpec);
            }
        }

        // First, get the page of BookPack IDs with filtering
        Page<BookPack> page = bookPackRepository.findAll(spec, pageable);

        // If no results, return empty page
        if (page.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // Extract IDs from the page
        List<Long> ids = page.getContent().stream()
            .map(BookPack::getId)
            .collect(Collectors.toList());

        // Fetch the BookPacks with books (will be lazy loaded)
        List<BookPack> bookPacksWithBooks = bookPackRepository.findAllByIdsWithEagerRelationships(ids);

        // Trigger lazy loading of books
        bookPacksWithBooks.forEach(bp -> bp.getBooks().size());

        // Convert to DTOs maintaining the original order
        List<BookPackDTO> dtos = ids.stream()
            .map(id -> bookPacksWithBooks.stream()
                .filter(bp -> bp.getId().equals(id))
                .findFirst()
                .map(bookPackMapper::toDto)
                .orElse(null)
            )
            .filter(dto -> dto != null)
            .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    private Specification<BookPack> buildTagSpecification(Long categoryId, Long mainDisplayId) {
        Specification<BookPack> tagSpec = null;

        if (categoryId != null) {
            tagSpec = BookPackSpecifications.hasCategory(categoryId);
        }

        if (mainDisplayId != null) {
            Specification<BookPack> mainDisplaySpec = BookPackSpecifications.hasMainDisplay(mainDisplayId);
            tagSpec = tagSpec != null ? tagSpec.or(mainDisplaySpec) : mainDisplaySpec;
        }

        return tagSpec;
    }

    /**
     * Get one bookPack by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<BookPackDTO> findOne(Long id) {
        LOG.debug("Request to get BookPack : {}", id);
        return bookPackRepository.findOneWithEagerRelationships(id).map(bp -> {
            // Trigger lazy loading of books
            bp.getBooks().size();
            return bookPackMapper.toDto(bp);
        });
    }

    /**
     * Soft delete the bookPack by id (sets active = false, deletedAt and deletedBy).
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to soft delete BookPack : {}", id);
        bookPackRepository.findById(id).ifPresent(bookPack -> {
            bookPack.setActive(false);
            bookPack.setDeletedAt(Instant.now());
            SecurityUtils.getCurrentUserLogin().ifPresent(bookPack::setDeletedBy);
            bookPackRepository.save(bookPack);
        });
    }

    /**
     * Hard delete the bookPack by id (permanently removes from database).
     * WARNING: This cannot be undone. Only use after cleanup job has nullified FKs.
     *
     * @param id the id of the entity.
     */
    public void deleteForever(Long id) {
        LOG.debug("Request to hard delete BookPack : {}", id);
        bookPackRepository.deleteById(id);
    }

    /**
     * Get book pack recommendations for a book (packs that contain the book).
     *
     * @param bookId the id of the book.
     * @param pageable the pagination information.
     * @return the list of book packs containing the book.
     */
    @Transactional(readOnly = true)
    public Page<BookPackDTO> findRecommendationsForBook(Long bookId, Pageable pageable) {
        LOG.debug("Request to get BookPack recommendations for Book : {}", bookId);

        // Verify the book exists
        if (!bookRepository.existsById(bookId)) {
            throw new BadRequestAlertException("Book not found", "book", "idnotfound");
        }

        // First, get the page of BookPack IDs (without eager loading)
        Page<BookPack> page = bookPackRepository.findByBookId(bookId, pageable);

        // If no results, return empty page
        if (page.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // Extract IDs from the page
        List<Long> ids = page.getContent().stream()
            .map(BookPack::getId)
            .collect(Collectors.toList());

        // Fetch the BookPacks with books (will be lazy loaded)
        List<BookPack> bookPacksWithBooks = bookPackRepository.findByIdsWithEagerRelationships(ids);

        // Trigger lazy loading of books
        bookPacksWithBooks.forEach(bp -> bp.getBooks().size());

        // Convert to DTOs maintaining the original order
        List<BookPackDTO> dtos = ids.stream()
            .map(id -> bookPacksWithBooks.stream()
                .filter(bp -> bp.getId().equals(id))
                .findFirst()
                .map(bookPackMapper::toDto)
                .orElse(null)
            )
            .filter(dto -> dto != null)
            .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }
}
