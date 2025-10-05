package com.oussamabenberkane.espritlivre.service;

import com.oussamabenberkane.espritlivre.domain.Book;
import com.oussamabenberkane.espritlivre.domain.Like;
import com.oussamabenberkane.espritlivre.domain.User;
import com.oussamabenberkane.espritlivre.repository.BookRepository;
import com.oussamabenberkane.espritlivre.repository.LikeRepository;
import com.oussamabenberkane.espritlivre.repository.UserRepository;
import com.oussamabenberkane.espritlivre.security.SecurityUtils;
import com.oussamabenberkane.espritlivre.service.dto.LikeDTO;
import com.oussamabenberkane.espritlivre.service.dto.LikeToggleResponseDTO;
import com.oussamabenberkane.espritlivre.service.mapper.LikeMapper;
import com.oussamabenberkane.espritlivre.web.rest.errors.BadRequestAlertException;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.oussamabenberkane.espritlivre.domain.Like}.
 */
@Service
@Transactional
public class LikeService {

    private static final Logger LOG = LoggerFactory.getLogger(LikeService.class);

    private final LikeRepository likeRepository;

    private final LikeMapper likeMapper;

    private final UserRepository userRepository;

    private final BookRepository bookRepository;

    public LikeService(LikeRepository likeRepository, LikeMapper likeMapper, UserRepository userRepository, BookRepository bookRepository) {
        this.likeRepository = likeRepository;
        this.likeMapper = likeMapper;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
    }

    /**
     * Toggle like for a book (like if not liked, unlike if already liked).
     * Automatically uses the current authenticated user.
     *
     * @param bookId the id of the book.
     * @return the toggle response with book id, like status, and total like count.
     */
    public LikeToggleResponseDTO toggleLike(Long bookId) {
        LOG.debug("Request to toggle like for book : {}", bookId);

        // Get current authenticated user
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", "like", "notauthenticated"));

        User user = userRepository.findOneByLogin(login)
            .orElseThrow(() -> new BadRequestAlertException("User not found", "like", "usernotfound"));

        // Check if book exists
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BadRequestAlertException("Book not found", "like", "booknotfound"));

        // Check if user already liked this book
        Optional<Like> existingLike = likeRepository.findByBookIdAndCurrentUser(bookId);

        boolean isLiked;
        if (existingLike.isPresent()) {
            // Unlike: delete the existing like
            likeRepository.delete(existingLike.get());
            isLiked = false;
            LOG.debug("User {} unliked book {}", login, bookId);
        } else {
            // Like: create a new like
            Like newLike = new Like();
            newLike.setUser(user);
            newLike.setBook(book);
            newLike.setCreatedAt(ZonedDateTime.now());
            likeRepository.save(newLike);
            isLiked = true;
            LOG.debug("User {} liked book {}", login, bookId);
        }

        // Get updated like count
        Long likeCount = likeRepository.countByBookId(bookId);

        return new LikeToggleResponseDTO(bookId, isLiked, likeCount);
    }
}
