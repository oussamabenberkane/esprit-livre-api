package com.oussamabenberkane.espritlivre.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates the WhatsApp bot's calls to {@code /internal/**} using a shared secret header.
 * On a valid secret the request is granted the {@code INTERNAL} authority; otherwise it stays
 * unauthenticated and the {@code /internal/**} authorization rule rejects it (401/403).
 * <p>
 * These endpoints carry no Keycloak JWT — this filter is their only authentication.
 */
public class InternalAuthFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Token";
    public static final String AUTHORITY = "INTERNAL";
    private static final String PATH_PREFIX = "/internal/";

    private final String expectedSecret;

    public InternalAuthFilter(String expectedSecret) {
        this.expectedSecret = expectedSecret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String token = request.getHeader(HEADER);
        if (StringUtils.hasText(expectedSecret) && constantTimeEquals(expectedSecret, token)) {
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "bot-internal",
                null,
                List.of(new SimpleGrantedAuthority(AUTHORITY))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(PATH_PREFIX);
    }

    private static boolean constantTimeEquals(String expected, String provided) {
        if (provided == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8));
    }
}
