package com.oussamabenberkane.espritlivre.service.mapper;

import com.oussamabenberkane.espritlivre.domain.Book;
import com.oussamabenberkane.espritlivre.domain.BookPack;
import com.oussamabenberkane.espritlivre.service.dto.BookDTO;
import com.oussamabenberkane.espritlivre.service.dto.BookPackDTO;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link BookPack} and its DTO {@link BookPackDTO}.
 */
@Mapper(componentModel = "spring")
public interface BookPackMapper extends EntityMapper<BookPackDTO, BookPack> {
    @Mapping(target = "books", source = "books", qualifiedByName = "bookIdTitleSet")
    BookPackDTO toDto(BookPack s);

    @Mapping(target = "removeBooks", ignore = true)
    BookPack toEntity(BookPackDTO bookPackDTO);

    @Named("bookIdTitle")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "price", source = "price")
    @Mapping(target = "coverImageUrl", source = "coverImageUrl")
    @Mapping(target = "stockQuantity", source = "stockQuantity")
    BookDTO toDtoBookIdTitle(Book book);

    @Named("bookIdTitleSet")
    default Set<BookDTO> toDtoBookIdTitleSet(Set<Book> books) {
        return books.stream().map(this::toDtoBookIdTitle).collect(Collectors.toSet());
    }
}
