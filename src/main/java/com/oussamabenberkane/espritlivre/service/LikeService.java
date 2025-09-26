package com.oussamabenberkane.espritlivre.service;

import com.oussamabenberkane.espritlivre.domain.Like;
import com.oussamabenberkane.espritlivre.repository.LikeRepository;
import com.oussamabenberkane.espritlivre.service.dto.LikeDTO;
import com.oussamabenberkane.espritlivre.service.mapper.LikeMapper;
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

    public LikeService(LikeRepository likeRepository, LikeMapper likeMapper) {
        this.likeRepository = likeRepository;
        this.likeMapper = likeMapper;
    }

    /**
     * Save a like.
     *
     * @param likeDTO the entity to save.
     * @return the persisted entity.
     */
    public LikeDTO save(LikeDTO likeDTO) {
        LOG.debug("Request to save Like : {}", likeDTO);
        Like like = likeMapper.toEntity(likeDTO);
        like = likeRepository.save(like);
        return likeMapper.toDto(like);
    }

    /**
     * Update a like.
     *
     * @param likeDTO the entity to save.
     * @return the persisted entity.
     */
    public LikeDTO update(LikeDTO likeDTO) {
        LOG.debug("Request to update Like : {}", likeDTO);
        Like like = likeMapper.toEntity(likeDTO);
        like = likeRepository.save(like);
        return likeMapper.toDto(like);
    }

    /**
     * Get all the likes.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<LikeDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all Likes");
        return likeRepository.findAll(pageable).map(likeMapper::toDto);
    }

    /**
     * Get all the likes with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<LikeDTO> findAllWithEagerRelationships(Pageable pageable) {
        return likeRepository.findAllWithEagerRelationships(pageable).map(likeMapper::toDto);
    }

    /**
     * Get one like by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<LikeDTO> findOne(Long id) {
        LOG.debug("Request to get Like : {}", id);
        return likeRepository.findOneWithEagerRelationships(id).map(likeMapper::toDto);
    }

    /**
     * Delete the like by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete Like : {}", id);
        likeRepository.deleteById(id);
    }
}
