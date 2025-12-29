package com.oussamabenberkane.espritlivre.service.mapper;

import com.oussamabenberkane.espritlivre.domain.Book;
import com.oussamabenberkane.espritlivre.domain.BookPack;
import com.oussamabenberkane.espritlivre.domain.Order;
import com.oussamabenberkane.espritlivre.domain.OrderItem;
import com.oussamabenberkane.espritlivre.domain.enumeration.OrderItemType;
import com.oussamabenberkane.espritlivre.service.dto.BookDTO;
import com.oussamabenberkane.espritlivre.service.dto.BookPackDTO;
import com.oussamabenberkane.espritlivre.service.dto.OrderDTO;
import com.oussamabenberkane.espritlivre.service.dto.OrderItemDTO;
import org.mapstruct.*;

import java.util.Comparator;

/**
 * Mapper for the entity {@link OrderItem} and its DTO {@link OrderItemDTO}.
 */
@Mapper(componentModel = "spring")
public interface OrderItemMapper extends EntityMapper<OrderItemDTO, OrderItem> {
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "book", ignore = true)
    @Mapping(target = "bookPack", ignore = true)
    @Mapping(target = "bookId", source = ".", qualifiedByName = "mapBookId")
    @Mapping(target = "bookTitle", source = "book.title")
    @Mapping(target = "bookAuthor", source = "book.author.name")
    @Mapping(target = "bookPackId", source = "bookPack.id")
    @Mapping(target = "bookPackTitle", source = "bookPack.title")
    OrderItemDTO toDto(OrderItem s);

    @Named("mapBookId")
    default Long mapBookId(OrderItem orderItem) {
        if (orderItem.getItemType() == OrderItemType.PACK && orderItem.getBookPack() != null) {
            // For bookpack items, get the first book (lowest ID) from the bookpack
            return orderItem.getBookPack().getBooks().stream()
                .min(Comparator.comparing(Book::getId))
                .map(Book::getId)
                .orElse(null);
        } else if (orderItem.getBook() != null) {
            // For regular book items, use the book ID
            return orderItem.getBook().getId();
        }
        return null;
    }

    @Named("orderId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    OrderDTO toDtoOrderId(Order order);

    @Named("bookId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    BookDTO toDtoBookId(Book book);

    @Named("bookPackId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    BookPackDTO toDtoBookPackId(BookPack bookPack);
}
