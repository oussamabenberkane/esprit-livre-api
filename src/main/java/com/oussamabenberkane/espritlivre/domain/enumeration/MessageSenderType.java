package com.oussamabenberkane.espritlivre.domain.enumeration;

/**
 * Who authored a message. CUSTOMER for inbound; BOT or ADMIN for outbound.
 */
public enum MessageSenderType {
    CUSTOMER,
    BOT,
    ADMIN,
}
