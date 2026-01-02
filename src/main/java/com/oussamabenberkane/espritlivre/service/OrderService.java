package com.oussamabenberkane.espritlivre.service;

import com.oussamabenberkane.espritlivre.config.ApplicationProperties;
import com.oussamabenberkane.espritlivre.domain.Book;
import com.oussamabenberkane.espritlivre.domain.BookPack;
import com.oussamabenberkane.espritlivre.domain.Order;
import com.oussamabenberkane.espritlivre.domain.OrderItem;
import com.oussamabenberkane.espritlivre.domain.User;
import com.oussamabenberkane.espritlivre.domain.enumeration.OrderItemType;
import com.oussamabenberkane.espritlivre.domain.enumeration.OrderStatus;
import com.oussamabenberkane.espritlivre.repository.BookPackRepository;
import com.oussamabenberkane.espritlivre.repository.BookRepository;
import com.oussamabenberkane.espritlivre.repository.OrderRepository;
import com.oussamabenberkane.espritlivre.repository.UserRepository;
import com.oussamabenberkane.espritlivre.security.AuthoritiesConstants;
import com.oussamabenberkane.espritlivre.security.SecurityUtils;
import com.oussamabenberkane.espritlivre.service.dto.OrderDTO;
import com.oussamabenberkane.espritlivre.service.dto.OrderItemDTO;
import com.oussamabenberkane.espritlivre.service.mapper.OrderMapper;
import com.oussamabenberkane.espritlivre.service.specs.OrderSpecifications;
import com.oussamabenberkane.espritlivre.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hibernate.annotations.BatchSize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Service Implementation for managing {@link com.oussamabenberkane.espritlivre.domain.Order}.
 */
@Service
@Transactional
public class OrderService {

    private static final Logger LOG = LoggerFactory.getLogger(OrderService.class);
    private static final String ANONYMOUS_USER = "anonymousUser";

    private final OrderRepository orderRepository;

    private final OrderMapper orderMapper;

    private final UniqueIdGeneratorService uniqueIdGeneratorService;

    private final UserRepository userRepository;

    private final BookRepository bookRepository;

    private final BookPackRepository bookPackRepository;

    private final MailService mailService;

    private final ApplicationProperties applicationProperties;

    public OrderService(
        OrderRepository orderRepository,
        OrderMapper orderMapper,
        UniqueIdGeneratorService uniqueIdGeneratorService,
        UserRepository userRepository,
        BookRepository bookRepository,
        BookPackRepository bookPackRepository,
        MailService mailService,
        ApplicationProperties applicationProperties
    ) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.uniqueIdGeneratorService = uniqueIdGeneratorService;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.bookPackRepository = bookPackRepository;
        this.mailService = mailService;
        this.applicationProperties = applicationProperties;
    }

    /**
     * Create a new order.
     * Auto-sets: uniqueId, status (PENDING), createdAt, createdBy, user (if authenticated).
     * Validates: required fields (phone, city, wilaya).
     * Supports: individual books, book packs, or mixed orders.
     * Uses user profile data as fallback for: fullName, phone, email, city, wilaya.
     *
     * @param orderDTO the entity to create.
     * @return the persisted entity.
     */
    public OrderDTO create(OrderDTO orderDTO) {
        LOG.debug("Request to create Order : {}", orderDTO);

        Order order = new Order();

        // Auto-set: uniqueId, status, timestamps
        order.setUniqueId(uniqueIdGeneratorService.generateOrderUniqueId());
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(ZonedDateTime.now());

        // Handle user (authenticated or guest)
        String currentUserLogin = SecurityUtils.getCurrentUserLogin().get();
        User user = null;
        if (currentUserLogin.equals(ANONYMOUS_USER)) {
            order.setCreatedBy("guest");
        } else {
            user = userRepository.findOneByLogin(currentUserLogin)
                .orElseThrow(() -> new BadRequestAlertException("User not found", "order", "usernotfound"));
            order.setUser(user);
            order.setCreatedBy(currentUserLogin);
        }

        // Customer details priority logic: use provided values, fallback to user profile
        order.setFullName(getValueOrUserFallback(orderDTO.getFullName(), user != null ? user.getFirstName() + " " + user.getLastName() : null));
        order.setPhone(getValueOrUserFallback(orderDTO.getPhone(), user != null ? user.getPhone() : null));
        order.setEmail(getValueOrUserFallback(orderDTO.getEmail(), user != null ? user.getEmail() : null));
        order.setCity(getValueOrUserFallback(orderDTO.getCity(), user != null ? user.getCity() : null));
        order.setWilaya(getValueOrUserFallback(orderDTO.getWilaya(), user != null ? user.getWilaya() : null));
        order.setStreetAddress(orderDTO.getStreetAddress());
        order.setPostalCode(orderDTO.getPostalCode());

        // Validate required fields
        if (!StringUtils.hasText(order.getPhone())) {
            throw new BadRequestAlertException("Phone is required", "order", "phonerequired");
        }
        if (!StringUtils.hasText(order.getCity())) {
            throw new BadRequestAlertException("City is required", "order", "cityrequired");
        }
        if (!StringUtils.hasText(order.getWilaya())) {
            throw new BadRequestAlertException("Wilaya is required", "order", "wilayarequired");
        }

        // Shipping and pricing info
        order.setShippingMethod(orderDTO.getShippingMethod());
        order.setShippingProvider(orderDTO.getShippingProvider());
        order.setShippingCost(orderDTO.getShippingCost());
        order.setTotalAmount(orderDTO.getTotalAmount());

        // Process order items - supports both books and book packs
        Set<OrderItem> orderItems = new HashSet<>();
        if (orderDTO.getOrderItems() != null && !orderDTO.getOrderItems().isEmpty()) {
            for (OrderItemDTO itemDTO : orderDTO.getOrderItems()) {
                OrderItem orderItem = new OrderItem();
                orderItem.setItemType(itemDTO.getItemType());
                orderItem.setQuantity(itemDTO.getQuantity());
                orderItem.setUnitPrice(itemDTO.getUnitPrice());
                orderItem.setTotalPrice(itemDTO.getTotalPrice());
                orderItem.setOrder(order);

                // Handle individual book items
                if (itemDTO.getItemType() == OrderItemType.BOOK && itemDTO.getBookId() != null) {
                    Book book = bookRepository.findById(itemDTO.getBookId())
                        .orElseThrow(() -> new BadRequestAlertException("Book not found", "orderItem", "booknotfound"));

                    // Stock availability warning (non-blocking)
                    if (book.getStockQuantity() == null || book.getStockQuantity() < itemDTO.getQuantity()) {
                        LOG.warn("Order created with insufficient stock for book: {} (ID: {}). Available: {}, Requested: {}",
                            book.getTitle(), book.getId(), book.getStockQuantity(), itemDTO.getQuantity());
                    }

                    orderItem.setBook(book);
                }
                // Handle book pack items
                else if (itemDTO.getItemType() == OrderItemType.PACK && itemDTO.getBookPackId() != null) {
                    BookPack bookPack = bookPackRepository.findById(itemDTO.getBookPackId())
                        .orElseThrow(() -> new BadRequestAlertException("Book pack not found", "orderItem", "bookpacknotfound"));

                    orderItem.setBookPack(bookPack);
                }
                else {
                    throw new BadRequestAlertException("Invalid order item: must specify either bookId (for BOOK type) or bookPackId (for PACK type)", "orderItem", "invaliditem");
                }

                orderItems.add(orderItem);
            }
        }
        order.setOrderItems(orderItems);

        order = orderRepository.save(order);
        OrderDTO savedOrderDTO = orderMapper.toDto(order);

        // Send admin notification email
        try {
            String adminPanelUrl = applicationProperties.getAdminPanelUrl() + "/orders/" + savedOrderDTO.getId();
            mailService.sendNewOrderNotificationToAdmin(savedOrderDTO, adminPanelUrl);
            LOG.debug("Admin notification email queued for order: {}", savedOrderDTO.getUniqueId());
        } catch (Exception e) {
            LOG.error("Failed to send admin notification email for order: {}. Error: {}", savedOrderDTO.getUniqueId(), e.getMessage(), e);
        }

        return savedOrderDTO;
    }

    private String getValueOrUserFallback(String providedValue, String userValue) {
        return StringUtils.hasText(providedValue) ? providedValue : userValue;
    }

    private Specification<Order> buildOrderSpecification(
        OrderStatus status,
        ZonedDateTime dateFrom,
        ZonedDateTime dateTo,
        BigDecimal minAmount,
        BigDecimal maxAmount
    ) {
        Specification<Order> spec = Specification.where(OrderSpecifications.activeOnly());

        if (status != null) {
            spec = spec.and(OrderSpecifications.hasStatus(status));
        }

        if (dateFrom != null || dateTo != null) {
            spec = spec.and(OrderSpecifications.createdBetween(dateFrom, dateTo));
        }

        if (minAmount != null || maxAmount != null) {
            spec = spec.and(OrderSpecifications.totalAmountBetween(minAmount, maxAmount));
        }

        return spec;
    }

    /**
     * Update a order (Admin only).
     * Cannot update: id, uniqueId, createdAt, createdBy.
     * Auto-decrements book stock when status changes to DELIVERED.
     *
     * @param orderDTO the entity to save.
     * @return the persisted entity.
     */
    public OrderDTO update(OrderDTO orderDTO) {
        LOG.debug("Request to update Order : {}", orderDTO);

        // Fetch existing order
        Order existingOrder = orderRepository.findById(orderDTO.getId())
            .orElseThrow(() -> new BadRequestAlertException("Order not found", "order", "ordernotfound"));

        OrderStatus oldStatus = existingOrder.getStatus();

        // Update allowed fields (cannot update id, uniqueId, createdAt, createdBy)
        existingOrder.setStatus(orderDTO.getStatus());
        existingOrder.setTotalAmount(orderDTO.getTotalAmount());
        existingOrder.setShippingCost(orderDTO.getShippingCost());
        existingOrder.setShippingProvider(orderDTO.getShippingProvider());
        existingOrder.setShippingMethod(orderDTO.getShippingMethod());
        existingOrder.setFullName(orderDTO.getFullName());
        existingOrder.setPhone(orderDTO.getPhone());
        existingOrder.setEmail(orderDTO.getEmail());
        existingOrder.setStreetAddress(orderDTO.getStreetAddress());
        existingOrder.setWilaya(orderDTO.getWilaya());
        existingOrder.setCity(orderDTO.getCity());
        existingOrder.setPostalCode(orderDTO.getPostalCode());
        existingOrder.setUpdatedAt(ZonedDateTime.now());

        // Update user if provided
        if (orderDTO.getUser() != null && orderDTO.getUser().getId() != null) {
            User user = userRepository.findById(orderDTO.getUser().getId())
                .orElseThrow(() -> new BadRequestAlertException("User not found", "order", "usernotfound"));
            existingOrder.setUser(user);
        }

        // Update order items if provided
        if (orderDTO.getOrderItems() != null) {
            // Clear existing items
            existingOrder.getOrderItems().clear();

            // Add new items
            Set<OrderItem> newOrderItems = new HashSet<>();
            for (OrderItemDTO itemDTO : orderDTO.getOrderItems()) {
                // Use pessimistic lock for consistency
                Book book = bookRepository.findByIdWithLock(itemDTO.getBookId())
                    .orElseThrow(() -> new BadRequestAlertException("Book not found", "orderItem", "booknotfound"));

                OrderItem orderItem = new OrderItem();
                if (itemDTO.getId() != null) {
                    orderItem.setId(itemDTO.getId());
                }
                orderItem.setBook(book);
                orderItem.setQuantity(itemDTO.getQuantity());
                orderItem.setUnitPrice(itemDTO.getUnitPrice());
                orderItem.setTotalPrice(itemDTO.getTotalPrice());
                orderItem.setOrder(existingOrder);
                newOrderItems.add(orderItem);
            }
            existingOrder.setOrderItems(newOrderItems);
        }

        // Stock management: decrement when changing to DELIVERED, restore when changing from DELIVERED
        if (orderDTO.getStatus() == OrderStatus.DELIVERED && oldStatus != OrderStatus.DELIVERED) {
            // Decrement stock using batch update
            LOG.debug("Order status changed to DELIVERED, decrementing stock for order items");
            for (OrderItem item : existingOrder.getOrderItems()) {
                if (item.getItemType() == OrderItemType.BOOK && item.getBook() != null) {
                    // Handle individual book stock
                    int updated = bookRepository.decrementStock(item.getBook().getId(), item.getQuantity());
                    if (updated == 0) {
                        LOG.warn("Insufficient stock for book {} during order delivery. Required: {}",
                            item.getBook().getId(), item.getQuantity());
                    } else {
                        LOG.debug("Decremented stock for book {} by {}", item.getBook().getId(), item.getQuantity());
                    }
                } else if (item.getItemType() == OrderItemType.PACK && item.getBookPack() != null) {
                    // Handle book pack - decrement stock for all books in the pack
                    BookPack bookPack = bookPackRepository.findOneWithEagerRelationships(item.getBookPack().getId())
                        .orElse(item.getBookPack());
                    LOG.debug("Decrementing stock for book pack: {} (contains {} books)",
                        bookPack.getTitle(), bookPack.getBooks().size());
                    for (Book book : bookPack.getBooks()) {
                        int updated = bookRepository.decrementStock(book.getId(), item.getQuantity());
                        if (updated == 0) {
                            LOG.warn("Insufficient stock for book {} (from pack {}) during order delivery. Required: {}",
                                book.getId(), bookPack.getTitle(), item.getQuantity());
                        } else {
                            LOG.debug("Decremented stock for book {} (from pack {}) by {}",
                                book.getId(), bookPack.getTitle(), item.getQuantity());
                        }
                    }
                }
            }
        } else if (oldStatus == OrderStatus.DELIVERED && orderDTO.getStatus() != OrderStatus.DELIVERED) {
            // Restore stock when moving from DELIVERED to another status
            LOG.debug("Order status changed from DELIVERED, restoring stock for order items");
            for (OrderItem item : existingOrder.getOrderItems()) {
                if (item.getItemType() == OrderItemType.BOOK && item.getBook() != null) {
                    // Restore individual book stock
                    bookRepository.incrementStock(item.getBook().getId(), item.getQuantity());
                    LOG.debug("Restored stock for book {} by {}", item.getBook().getId(), item.getQuantity());
                } else if (item.getItemType() == OrderItemType.PACK && item.getBookPack() != null) {
                    // Restore stock for all books in the pack
                    BookPack bookPack = bookPackRepository.findOneWithEagerRelationships(item.getBookPack().getId())
                        .orElse(item.getBookPack());
                    LOG.debug("Restoring stock for book pack: {} (contains {} books)",
                        bookPack.getTitle(), bookPack.getBooks().size());
                    for (Book book : bookPack.getBooks()) {
                        bookRepository.incrementStock(book.getId(), item.getQuantity());
                        LOG.debug("Restored stock for book {} (from pack {}) by {}",
                            book.getId(), bookPack.getTitle(), item.getQuantity());
                    }
                }
            }
        }

        existingOrder = orderRepository.save(existingOrder);
        return orderMapper.toDto(existingOrder);
    }

    /**
     * Get all the orders with optional filtering.
     *
     * @param pageable the pagination information.
     * @param status the order status filter.
     * @param dateFrom the start date filter.
     * @param dateTo the end date filter.
     * @param minAmount the minimum total amount filter.
     * @param maxAmount the maximum total amount filter.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<OrderDTO> findAll(
        Pageable pageable,
        OrderStatus status,
        ZonedDateTime dateFrom,
        ZonedDateTime dateTo,
        BigDecimal minAmount,
        BigDecimal maxAmount
    ) {
        LOG.debug("Request to get all Orders with filters - status: {}, dateRange: [{}, {}], amountRange: [{}, {}]",
            status, dateFrom, dateTo, minAmount, maxAmount);

        Specification<Order> spec = buildOrderSpecification(status, dateFrom, dateTo, minAmount, maxAmount);
        return orderRepository.findAll(spec, pageable).map(orderMapper::toDto);
    }

    /**
     * Get all orders for current user with optional filtering.
     *
     * @param pageable the pagination information.
     * @param status the order status filter.
     * @param dateFrom the start date filter.
     * @param dateTo the end date filter.
     * @param minAmount the minimum total amount filter.
     * @param maxAmount the maximum total amount filter.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<OrderDTO> findAllForCurrentUser(
        Pageable pageable,
        OrderStatus status,
        ZonedDateTime dateFrom,
        ZonedDateTime dateTo,
        BigDecimal minAmount,
        BigDecimal maxAmount
    ) {
        LOG.debug("Request to get orders for current user with filters");

        Specification<Order> spec = buildOrderSpecification(status, dateFrom, dateTo, minAmount, maxAmount);
        return orderRepository.findByCurrentUser(spec, pageable).map(orderMapper::toDto);
    }

    /**
     * Get all the orders with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<OrderDTO> findAllWithEagerRelationships(Pageable pageable) {
        return orderRepository.findAllWithEagerRelationships(pageable).map(orderMapper::toDto);
    }

    /**
     * Get one order by id.
     * For regular users, only returns their own orders.
     * For admins, returns any order.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<OrderDTO> findOne(Long id) {
        LOG.debug("Request to get Order : {}", id);

        // Check if user is admin
        boolean isAdmin = SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN);

        if (isAdmin) {
            return orderRepository.findOneWithEagerRelationships(id).map(orderMapper::toDto);
        } else {
            // Regular user: only return if order belongs to them
            return orderRepository.findOneByIdAndCurrentUser(id).map(orderMapper::toDto);
        }
    }

    /**
     * Soft delete the order by id (sets active = false, deletedAt and deletedBy).
     * OrderItems are cascade deleted due to orphanRemoval=true.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to soft delete Order : {}", id);
        orderRepository.findById(id).ifPresent(order -> {
            order.setActive(false);
            order.setDeletedAt(Instant.now());
            SecurityUtils.getCurrentUserLogin().ifPresent(order::setDeletedBy);
            order.setUpdatedAt(ZonedDateTime.now());
            orderRepository.save(order);
        });
    }

    /**
     * Hard delete the order by id (permanently removes from database).
     * WARNING: This cannot be undone. Only use after cleanup job.
     *
     * @param id the id of the entity.
     */
    public void deleteForever(Long id) {
        LOG.debug("Request to hard delete Order : {}", id);
        orderRepository.deleteById(id);
    }

    /**
     * Normalize phone number to E.164 format for Algeria.
     * Handles two formats: "0555123456" and "+213555123456".
     *
     * @param phone the phone number to normalize
     * @return normalized phone number in format +213XXXXXXXXX, or null if input is null
     */
    public static String normalizePhoneNumber(String phone) {
        if (phone == null) {
            return null;
        }

        // Remove all whitespace, dashes, parentheses
        String normalized = phone.replaceAll("[\\s\\-()]", "");

        // Handle two formats:
        // "0555123456" -> "+213555123456"
        // "+213555123456" -> "+213555123456"
        if (normalized.startsWith("0") && normalized.length() == 10) {
            return "+213" + normalized.substring(1);
        } else if (normalized.startsWith("+213")) {
            return normalized;
        }

        // Return as-is if doesn't match expected formats
        return normalized;
    }

    /**
     * Synchronously link guest orders to a user based on matching phone number.
     * Called when user updates their profile with a phone number.
     * Finds all orders with no userId but matching phone number and links them to the user.
     *
     * @param userId the ID of the user to link orders to
     * @param phoneNumber the phone number to match against guest orders
     * @return the number of orders that were linked
     */
    public int linkGuestOrdersToUser(String userId, String phoneNumber) {
        LOG.debug("Request to link guest orders for user {} with phone {}", userId, phoneNumber);

        if (phoneNumber == null || phoneNumber.isEmpty()) {
            LOG.info("Phone number is empty, skipping order linking");
            return 0;
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            LOG.warn("User {} not found, cannot link orders", userId);
            return 0;
        }

        User user = userOpt.get();

        try {
            String normalizedPhone = normalizePhoneNumber(phoneNumber);
            LOG.debug("Searching for guest orders with normalized phone: {}", normalizedPhone);

            // Find all guest orders with matching phone
            java.util.List<Order> guestOrders = orderRepository.findByUserIsNullAndPhone(normalizedPhone);

            if (guestOrders.isEmpty()) {
                LOG.info("No guest orders found for phone: {}", normalizedPhone);
                return 0;
            }

            ZonedDateTime now = ZonedDateTime.now();
            int linkedCount = 0;

            for (Order order : guestOrders) {
                // Defensive check: skip if already linked
                if (order.getUser() != null) {
                    LOG.warn("Order {} already linked to user {}, skipping", order.getId(), order.getUser().getId());
                    continue;
                }

                order.setUser(user);
                order.setLinkedAt(now);
                orderRepository.save(order);
                linkedCount++;
            }

            LOG.info("Linked {} guest orders to user {}", linkedCount, userId);
            return linkedCount;
        } catch (Exception e) {
            LOG.error("Error linking orders for user {}", userId, e);
            return 0;
        }
    }

    /**
     * Update phone number for all active orders belonging to a user.
     * Active orders are: PENDING, CONFIRMED, SHIPPED.
     * Only updates the phone number field.
     *
     * @param user the user whose orders should be updated
     * @return the number of orders that were updated
     */
    @Transactional
    @BatchSize(size = 100)
    public int updateUserActiveOrdersPhone(User user) {
        LOG.debug("Request to update active orders phone for user {}", user.getId());

        if (user.getPhone() == null || user.getPhone().isEmpty()) {
            LOG.info("User {} has no phone number, skipping order update", user.getId());
            return 0;
        }

        try {
            // Find all active orders for this user
            List<Order> activeOrders = orderRepository.findAll().stream()
                .filter(order -> order.getUser() != null && order.getUser().getId().equals(user.getId()))
                .filter(order -> order.getStatus() == OrderStatus.PENDING ||
                                order.getStatus() == OrderStatus.CONFIRMED ||
                                order.getStatus() == OrderStatus.SHIPPED)
                .toList();

            if (activeOrders.isEmpty()) {
                LOG.info("No active orders found for user {}", user.getId());
                return 0;
            }

            // Update phone number for all orders
            for (Order order : activeOrders) {
                order.setPhone(user.getPhone());
            }

            // Batch save all orders at once
            orderRepository.saveAll(activeOrders);

            int updatedCount = activeOrders.size();
            LOG.info("Updated phone number for {} active orders for user {}", updatedCount, user.getId());
            return updatedCount;
        } catch (Exception e) {
            LOG.error("Error updating orders phone for user {}", user.getId(), e);
            return 0;
        }
    }
}
