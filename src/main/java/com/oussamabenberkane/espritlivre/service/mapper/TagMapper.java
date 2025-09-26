package com.oussamabenberkane.espritlivre.service.mapper;

import com.oussamabenberkane.espritlivre.domain.Book;
import com.oussamabenberkane.espritlivre.domain.Tag;
import com.oussamabenberkane.espritlivre.service.dto.BookDTO;
import com.oussamabenberkane.espritlivre.service.dto.TagDTO;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Tag} and its DTO {@link TagDTO}.
 */
@Mapper(componentModel = "spring")
public interface TagMapper extends EntityMapper<TagDTO, Tag> {
    @Override
    @Mapping(target = "books", ignore = true)
    TagDTO toDto(Tag s);

    @Mapping(target = "removeBook", ignore = true)
    Tag toEntity(TagDTO tagDTO);
}
