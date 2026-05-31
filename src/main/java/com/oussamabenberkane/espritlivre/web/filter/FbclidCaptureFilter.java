package com.oussamabenberkane.espritlivre.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Captures the {@code fbclid} URL query parameter and writes it to a first-party
 * {@code _fbc} cookie in the canonical Meta CAPI format: {@code fb.1.<creationTime>.<fbclid>}.
 *
 * Closes the race window where {@code fbevents.js} has not yet loaded and set the cookie
 * by the time the first server-side CAPI event is sent. The fbclid value is preserved verbatim
 * (no lowercasing, no truncation) which is what Meta requires.
 *
 * Cookie is set only if {@code _fbc} is not already present on the request, so {@code fbevents.js}
 * retains authority once it does load.
 */
public class FbclidCaptureFilter extends OncePerRequestFilter {

    private static final String FBCLID_PARAM = "fbclid";
    private static final String FBC_COOKIE = "_fbc";
    // 90 days, matches Meta's documented attribution window.
    private static final int FBC_COOKIE_MAX_AGE_SECONDS = 90 * 24 * 60 * 60;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String fbclid = request.getParameter(FBCLID_PARAM);
        if (StringUtils.hasText(fbclid) && !hasFbcCookie(request)) {
            String fbcValue = "fb.1." + System.currentTimeMillis() + "." + fbclid;
            Cookie cookie = new Cookie(FBC_COOKIE, fbcValue);
            cookie.setPath("/");
            cookie.setMaxAge(FBC_COOKIE_MAX_AGE_SECONDS);
            cookie.setHttpOnly(false); // fbevents.js needs to be able to read it
            cookie.setSecure(request.isSecure());
            response.addCookie(cookie);
        }
        filterChain.doFilter(request, response);
    }

    private boolean hasFbcCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return false;
        for (Cookie c : cookies) {
            if (FBC_COOKIE.equals(c.getName()) && StringUtils.hasText(c.getValue())) {
                return true;
            }
        }
        return false;
    }
}
