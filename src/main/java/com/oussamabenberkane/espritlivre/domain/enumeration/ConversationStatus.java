package com.oussamabenberkane.espritlivre.domain.enumeration;

/**
 * Lifecycle status of a messaging conversation.
 * <ul>
 *     <li>{@code BOT_HANDLING} — the bot answers automatically (default).</li>
 *     <li>{@code NEEDS_HUMAN} — the bot escalated; waiting for a human. Bot stays silent.</li>
 *     <li>{@code HUMAN_HANDLING} — an admin took over. Bot stays silent.</li>
 *     <li>{@code RESOLVED} — closed. A new inbound message reopens it to {@code BOT_HANDLING}.</li>
 * </ul>
 */
public enum ConversationStatus {
    BOT_HANDLING,
    NEEDS_HUMAN,
    HUMAN_HANDLING,
    RESOLVED,
}
