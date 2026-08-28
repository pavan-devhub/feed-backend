package com.feedstartup.security;

import com.feedstartup.service.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SessionService sessionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String jwt = parseJwt(request);
            if (jwt != null && jwtUtil.validateToken(jwt)) {
                String jti = jwtUtil.extractJti(jwt);

                // The JWT itself is long-lived; the UserSession row is the real source of truth for
                // whether this device is still logged in (it's removed on logout or remote revoke).
                if (sessionService.isActive(jti)) {
                    String email = jwtUtil.extractEmail(jwt);
                    Long userId = jwtUtil.extractUserId(jwt);

                    // Create an authentication token with email as principal and userId as credential/detail
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            email, userId, new ArrayList<>());

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    sessionService.touch(jti);
                }
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e);
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        // The publication PDF/thumbnail are loaded via plain <img>/<a>/pdf.js URLs (including
        // byte-range requests pdf.js issues itself), none of which can attach an Authorization
        // header, so those two routes alone also accept the JWT as a query parameter.
        String uri = request.getRequestURI();
        if (uri != null && uri.matches(".*/api/publications/\\d+/(file|thumbnail)$")) {
            String tokenParam = request.getParameter("token");
            if (StringUtils.hasText(tokenParam)) {
                return tokenParam;
            }
        }

        return null;
    }
}
