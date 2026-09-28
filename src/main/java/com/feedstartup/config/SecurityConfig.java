package com.feedstartup.config;

import com.feedstartup.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/login", "/api/login/**", "/api/register").permitAll()
                .requestMatchers("/api/auth/**").authenticated()
                .requestMatchers("/api/ers/**").authenticated()
                // Profile-image GET stays public; only the upload/remove endpoints need auth.
                .requestMatchers("/api/users/me/**").authenticated()
                .requestMatchers("/api/publications/**").authenticated()
                // One admin for everything: Feed World publications and every EPM admin route
                // (events, registrations, volunteers, categories, venues, images, reviews) all
                // require the per-user ADMIN role from the JWT.
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(403);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"status\":\"error\",\"error\":\"Admin access required\"}");
                })
            );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*")); // Allow all origins for dev
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // "Range" is required for the Feed World PDF viewer: pdf.js fetches large PDFs in
        // byte-range chunks, and a cross-origin request carrying a Range header needs it
        // explicitly allow-listed or the browser's CORS preflight is rejected outright (the
        // request never even reaches the controller) - that was surfacing as "This PDF could
        // not be loaded" in the viewer.
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type", "Range"));
        // Content-Range/Accept-Ranges aren't on the default CORS-safelisted response headers,
        // so without exposing them explicitly, pdf.js can't read them cross-origin and can't
        // tell the byte-range request it just made actually succeeded.
        configuration.setExposedHeaders(Arrays.asList("Content-Range", "Accept-Ranges", "Content-Length", "Content-Disposition"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
