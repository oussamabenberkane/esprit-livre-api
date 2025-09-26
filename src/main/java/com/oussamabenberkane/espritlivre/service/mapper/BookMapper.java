package com.oussamabenberkane.espritlivre.service.mapper;

import com.oussamabenberkane.espritlivre.domain.Book;
import com.oussamabenberkane.espritlivre.domain.Tag;
import com.oussamabenberkane.espritlivre.service.dto.BookDTO;
import com.oussamabenberkane.espritlivre.service.dto.TagDTO;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Book} and its DTO {@link BookDTO}.
 */
@Mapper(componentModel = "spring")
public interface BookMapper extends EntityMapper<BookDTO, Book> {
    @Mapping(target = "tags", source = "tags", qualifiedByName = "tagFullSet")
    BookDTO toDto(Book s);

    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "removeTag", ignore = true)
    Book toEntity(BookDTO bookDTO);

    @Named("tagFull")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "nameEn", source = "nameEn")
    @Mapping(target = "nameFr", source = "nameFr")
    @Mapping(target = "type", source = "type")
    @Mapping(target = "active", source = "active")
    TagDTO toDtoTagFull(Tag tag);

    @Named("tagFullSet")
    default Set<TagDTO> toDtoTagFullSet(Set<Tag> tag) {
        return tag.stream().map(this::toDtoTagFull).collect(Collectors.toSet());
    }
}
