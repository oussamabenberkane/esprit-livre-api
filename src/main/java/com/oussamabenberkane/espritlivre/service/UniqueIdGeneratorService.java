package com.oussamabenberkane.espritlivre.service;

import com.oussamabenberkane.espritlivre.repository.OrderRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for generating unique order IDs.
 */
@Service
@Transactional
public class UniqueIdGeneratorService {

    private static final Logger LOG = LoggerFactory.getLogger(UniqueIdGeneratorService.class);
    private static final String ORDER_PREFIX = "ORD-";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final OrderRepository orderRepository;

    public UniqueIdGeneratorService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Generate a unique order ID in the format: ORD-YYYYMMDD-XXXX
     * where XXXX is a sequential number starting from 0001 for each day.
     *
     * @return the generated unique order ID
     */
    public String generateOrderUniqueId() {
        String today = LocalDate.now().format(DATE_FORMATTER);
        String prefix = ORDER_PREFIX + today + "-";

        // Find the highest sequence number for today
        String maxUniqueId = orderRepository.findMaxUniqueIdByPrefix(prefix);

        int nextSequence = 1;
        if (maxUniqueId != null && maxUniqueId.startsWith(prefix)) {
            try {
                // Extract the sequence number from the unique ID
                String sequencePart = maxUniqueId.substring(prefix.length());
                int currentSequence = Integer.parseInt(sequencePart);
                nextSequence = currentSequence + 1;
            } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                LOG.warn("Failed to parse sequence from uniqueId: {}, starting from 1", maxUniqueId);
            }
        }

        // Format sequence as 4-digit number with leading zeros
        String uniqueId = prefix + String.format("%04d", nextSequence);
        LOG.debug("Generated order unique ID: {}", uniqueId);

        return uniqueId;
    }
}
