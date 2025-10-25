package com.oussamabenberkane.espritlivre.service;

import com.oussamabenberkane.espritlivre.domain.Book;
import com.oussamabenberkane.espritlivre.domain.BookPack;
import com.oussamabenberkane.espritlivre.repository.BookPackRepository;
import com.oussamabenberkane.espritlivre.repository.BookRepository;
import com.oussamabenberkane.espritlivre.service.dto.BookPackDTO;
import com.oussamabenberkane.espritlivre.service.mapper.BookPackMapper;
import com.oussamabenberkane.espritlivre.web.rest.errors.BadRequestAlertException;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<BookPackDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all BookPacks");
        return bookPackRepository.findAllWithEagerRelationships(pageable).map(bookPackMapper::toDto);
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
        return bookPackRepository.findOneWithEagerRelationships(id).map(bookPackMapper::toDto);
    }

    /**
     * Delete the bookPack by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete BookPack : {}", id);
        bookPackRepository.deleteById(id);
    }
}
