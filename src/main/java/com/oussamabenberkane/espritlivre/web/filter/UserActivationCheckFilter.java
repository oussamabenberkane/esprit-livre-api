package com.oussamabenberkane.espritlivre.web.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oussamabenberkane.espritlivre.repository.UserRepository;
import com.oussamabenberkane.espritlivre.security.SecurityUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class UserActivationCheckFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public UserActivationCheckFilter(UserRepository userRepository, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            String login = SecurityUtils.getCurrentUserLogin().orElse(null);

            if (login != null) {
                boolean activated = userRepository.findOneByLogin(login).map(user -> user.isActivated()).orElse(true);

                if (!activated) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    objectMapper.writeValue(response.getWriter(), Map.of("code", "account_deactivated"));
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
