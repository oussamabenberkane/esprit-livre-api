package com.oussamabenberkane.espritlivre.service;

import com.oussamabenberkane.espritlivre.config.ApplicationProperties;
import com.oussamabenberkane.espritlivre.domain.Book;
import com.oussamabenberkane.espritlivre.domain.Order;
import com.oussamabenberkane.espritlivre.domain.OrderItem;
import com.oussamabenberkane.espritlivre.domain.User;
import com.oussamabenberkane.espritlivre.domain.enumeration.OrderStatus;
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
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
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

    private final OrderRepository orderRepository;

    private final OrderMapper orderMapper;

    private final UniqueIdGeneratorService uniqueIdGeneratorService;

    private final UserRepository userRepository;

    private final BookRepository bookRepository;

    private final MailService mailService;

    private final ApplicationProperties applicationProperties;

    public OrderService(
        OrderRepository orderRepository,
        OrderMapper orderMapper,
        UniqueIdGeneratorService uniqueIdGeneratorService,
        UserRepository userRepository,
        BookRepository bookRepository,
        MailService mailService,
        ApplicationProperties applicationProperties
    ) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.uniqueIdGeneratorService = uniqueIdGeneratorService;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.mailService = mailService;
        this.applicationProperties = applicationProperties;
    }

    /**
     * Save a order.
     * Auto-sets: uniqueId, status (PENDING), createdAt, createdBy, user (if authenticated).
     * Validates: required fields (phone, city, wilaya), stock availability.
     * Uses user profile data as fallback for: fullName, phone, email, city, wilaya.
     *
     * @param orderDTO the entity to save.
     * @return the persisted entity.
     */
    public OrderDTO save(OrderDTO orderDTO) {
        LOG.debug("Request to save Order : {}", orderDTO);

        Order order = new Order();

        // Auto-set: uniqueId, status, timestamps
        order.setUniqueId(uniqueIdGeneratorService.generateOrderUniqueId());
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(ZonedDateTime.now());

        // Handle user (authenticated or guest)
        String currentUserLogin = SecurityUtils.getCurrentUserLogin().get();
        User user = null;
        if (currentUserLogin.equals("anonymousUser")) {
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

        // Process order items with pessimistic locking to prevent race conditions
        Set<OrderItem> orderItems = new HashSet<>();
        if (orderDTO.getOrderItems() != null && !orderDTO.getOrderItems().isEmpty()) {
            for (OrderItemDTO itemDTO : orderDTO.getOrderItems()) {
                // Validate stock availability with pessimistic write lock
                Book book = bookRepository.findByIdWithLock(itemDTO.getBookId())
                    .orElseThrow(() -> new BadRequestAlertException("Book not found", "orderItem", "booknotfound"));

                if (book.getStockQuantity() == null || book.getStockQuantity() < itemDTO.getQuantity()) {
                    throw new BadRequestAlertException(
                        "Insufficient stock for book: " + book.getTitle() + " (available: " + book.getStockQuantity() + ", requested: " + itemDTO.getQuantity() + ")",
                        "orderItem",
                        "insufficientstock"
                    );
                }

                OrderItem orderItem = new OrderItem();
                orderItem.setBook(book);
                orderItem.setQuantity(itemDTO.getQuantity());
                orderItem.setUnitPrice(itemDTO.getUnitPrice());
                orderItem.setTotalPrice(itemDTO.getTotalPrice());
                orderItem.setOrder(order);
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
                Book book = bookRepository.findById(itemDTO.getBookId())
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

        // Stock decrement logic: when status changes to DELIVERED
        if (orderDTO.getStatus() == OrderStatus.DELIVERED && oldStatus != OrderStatus.DELIVERED) {
            LOG.debug("Order status changed to DELIVERED, decrementing stock for order items");
            for (OrderItem item : existingOrder.getOrderItems()) {
                Book book = item.getBook();
                Integer currentStock = book.getStockQuantity();
                if (currentStock != null && currentStock >= item.getQuantity()) {
                    book.setStockQuantity(currentStock - item.getQuantity());
                    bookRepository.save(book);
                    LOG.debug("Decremented stock for book {}: {} -> {}", book.getId(), currentStock, book.getStockQuantity());
                } else {
                    LOG.warn("Insufficient stock for book {} during order delivery. Current: {}, Required: {}",
                        book.getId(), currentStock, item.getQuantity());
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

        Specification<Order> spec = Specification.where(null);

        if (status != null) {
            spec = spec.and(OrderSpecifications.hasStatus(status));
        }

        if (dateFrom != null || dateTo != null) {
            spec = spec.and(OrderSpecifications.createdBetween(dateFrom, dateTo));
        }

        if (minAmount != null || maxAmount != null) {
            spec = spec.and(OrderSpecifications.totalAmountBetween(minAmount, maxAmount));
        }

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

        Specification<Order> spec = Specification.where(null);

        if (status != null) {
            spec = spec.and(OrderSpecifications.hasStatus(status));
        }

        if (dateFrom != null || dateTo != null) {
            spec = spec.and(OrderSpecifications.createdBetween(dateFrom, dateTo));
        }

        if (minAmount != null || maxAmount != null) {
            spec = spec.and(OrderSpecifications.totalAmountBetween(minAmount, maxAmount));
        }

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
     * Delete the order by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete Order : {}", id);
        orderRepository.deleteById(id);
    }
}
