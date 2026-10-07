package com.feedstartup.security;

import com.feedstartup.service.SessionService;
import com.feedstartup.service.SystemAdminSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private SystemAdminSessionService systemAdminSessionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String jwt = parseJwt(request);
            if (jwt != null && jwtUtil.validateToken(jwt)) {
                String jti = jwtUtil.extractJti(jwt);

                // The JWT itself is long-lived; the session row is the real source of truth for
                // whether this device is still logged in (it's removed on logout or remote revoke).
                // Which table holds it also decides the role: a user's login lives in
                // user_sessions, an admin's in system_admin_sessions - so a user's token is never
                // an admin's, whatever role its claims carry (including tokens of the accounts that
                // were admins before system_admins existed). ROLE_ADMIN is what SecurityConfig's
                // hasRole("ADMIN") checks.
                if (sessionService.isActive(jti)) {
                    // The user's email as principal and userId as credentials.
                    authenticate(request, jwtUtil.extractEmail(jwt), jwtUtil.extractUserId(jwt), "ROLE_USER");
                    sessionService.touch(jti);
                } else if (systemAdminSessionService.isActive(jti)) {
                    // The admin's username as principal and adminId as credentials.
                    authenticate(request, jwtUtil.extractSubject(jwt), jwtUtil.extractAdminId(jwt), "ROLE_ADMIN");
                    systemAdminSessionService.touch(jti);
                }
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String principal, Long accountId, String role) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal, accountId, List.of(new SimpleGrantedAuthority(role)));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        // The publication PDF/thumbnail are loaded via plain <img>/<a>/pdf.js URLs (including
        // byte-range requests pdf.js issues itself), none of which can attach an Authorization
        // header, so those two routes alone also accept the JWT as a query parameter.
        //
        // The id segment used to be a numeric database key (hence \d+); it is now a "<year>-<month>"
        // string like "2026-09" (see PublicationServiceImpl), so it's matched as any non-slash
        // segment instead - otherwise this fallback silently never fires and every publication
        // file/thumbnail request 403s.
        String uri = request.getRequestURI();
        if (uri != null && uri.matches(".*/api/publications/[^/]+/(file|thumbnail)$")) {
            String tokenParam = request.getParameter("token");
            if (StringUtils.hasText(tokenParam)) {
                return tokenParam;
            }
        }

        return null;
    }
}
